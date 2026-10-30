# NOVA X para TX10 Pro - instalador para Windows
# Se abre con INSTALAR.bat (doble clic). Habla con la caja por la red usando ADB.
$ErrorActionPreference = "Continue"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $dir
$pt = Join-Path $dir "platform-tools"
$adb = Join-Path $pt "adb.exe"
New-Item -ItemType Directory -Force -Path (Join-Path $dir "registros") | Out-Null
$log = Join-Path $dir ("registros\novax_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".txt")
$global:caja = $null

function Log($t, $color = "Gray") { "$t" | Out-File -Append -Encoding utf8 $log; Write-Host "$t" -ForegroundColor $color }
function Titulo($t) { Log ""; Log "=== $t ===" "Cyan" }
function Pausa { Read-Host "`nEnter para continuar" | Out-Null }

# ------------------------------------------------------------------ ADB
function Asegurar-ADB {
  if (Test-Path $adb) { return $true }
  Titulo "Descargando las herramientas de Android (platform-tools, de Google)"
  $zip = Join-Path $dir "platform-tools.zip"
  try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -UseBasicParsing "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $zip
    Expand-Archive -Force $zip $dir
    Remove-Item $zip
  } catch { Log "No se pudo descargar: $_" "Red"; return $false }
  return (Test-Path $adb)
}

function A {
  # corre adb contra la caja; si la conexion se corta, reconecta y reintenta
  param([string[]]$args2)
  for ($t = 1; $t -le 3; $t++) {
    $r = & $adb -s $global:caja @args2 2>&1 | Out-String
    if ($r -notmatch "device offline|device '.*' not found|no devices|error: closed|cannot connect") { return $r.Trim() }
    Log "  (se corto la conexion, reintentando...)" "DarkYellow"
    Start-Sleep 3
    Conectar $global:caja | Out-Null
  }
  return $r.Trim()
}
function Shell($cmd) { return (A @("shell", $cmd)) }
function Root($cmd) { return (A @("shell", "su 0 sh -c '$cmd'")) }

function Conectar($ip) {
  if ($ip -notmatch ":") { $ip = "$ip`:5555" }
  & $adb connect $ip 2>&1 | Out-Null
  Start-Sleep 2
  $s = (& $adb -s $ip get-state 2>&1 | Out-String)
  if ($s -match "^device") { $global:caja = $ip; return "ok" }
  if ($s -match "unauthorized") { return "permiso" }
  return "no"
}

function Buscar-Caja {
  Titulo "Buscando la caja en tu red"
  $ips = @()
  try {
    $ips = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop | Where-Object { $_.IPAddress -match "^(192\.168|10\.|172\.(1[6-9]|2\d|3[01]))" } | ForEach-Object { $_.IPAddress }
  } catch {
    $ips = (ipconfig | Select-String "IPv4" | ForEach-Object { ($_ -split ":")[-1].Trim() })
  }
  $found = @()
  foreach ($me in $ips) {
    $base = ($me -split "\.")[0..2] -join "."
    Log "Revisando $base.1 - $base.254 ..."
    $tasks = @{}
    foreach ($n in 1..254) {
      $c = New-Object System.Net.Sockets.TcpClient
      $tasks["$base.$n"] = @($c, $c.ConnectAsync("$base.$n", 5555))
    }
    Start-Sleep -Milliseconds 2500
    foreach ($k in $tasks.Keys) {
      $t = $tasks[$k]
      if ($t[1].Status -eq "RanToCompletion" -and $t[0].Connected) { $found += $k }
      $t[0].Close()
    }
  }
  return $found
}

function Elegir-Caja {
  & $adb kill-server 2>&1 | Out-Null
  & $adb start-server 2>&1 | Out-Null
  $ip = Read-Host "Escribe la IP de la caja (la ves en la caja: Ajustes > Red) o solo Enter para buscarla"
  $lista = @()
  if ($ip) { $lista = @($ip) } else { $lista = Buscar-Caja }
  if ($lista.Count -eq 0) {
    Log "No encontre ninguna caja con la depuracion por red prendida." "Red"
    Log "Revisa el paso 2 de las instrucciones (README) y que la caja y el PC esten en la misma red."
    return $false
  }
  foreach ($c in $lista) {
    for ($i = 1; $i -le 20; $i++) {
      $r = Conectar $c
      if ($r -eq "ok") { Log "Conectado a la caja en $global:caja" "Green"; return $true }
      if ($r -eq "permiso") {
        if ($i -eq 1) { Log "MIRA LA TELE: acepta 'Permitir depuracion' (marca 'Permitir siempre desde este equipo')." "Yellow" }
        Start-Sleep 3; continue
      }
      break
    }
  }
  Log "No pude conectarme." "Red"
  return $false
}

function Subir-Archivos {
  Shell "mkdir -p /data/local/tmp/novax" | Out-Null
  foreach ($f in Get-ChildItem (Join-Path $dir "caja")) {
    A @("push", $f.FullName, "/data/local/tmp/novax/$($f.Name)") | Out-Null
  }
}

# ------------------------------------------------------------------ pasos
function Identificar {
  Titulo "Identificando la caja"
  Subir-Archivos
  $r = Shell "sh /data/local/tmp/novax/identificar.sh"
  $r -split "`n" | ForEach-Object { Log "  $_" }
  $global:root = ($r -match "ROOT=si")
  if ($r -notmatch "COMPATIBLE=si") {
    Log "`nEsta caja NO es igual a la TX10 Pro probada (H313, Android 10)." "Red"
    Log "Para no danarla, el instalador no va a hacer cambios." "Red"
    return $false
  }
  Log "`nCaja compatible." "Green"
  return $true
}

function Reiniciar-Y-Esperar {
  Log "Reiniciando la caja (1-2 minutos)..." "Yellow"
  A @("reboot") | Out-Null
  Start-Sleep 75
  for ($i = 1; $i -le 30; $i++) {
    if ((Conectar $global:caja) -eq "ok") {
      $b = Shell "getprop sys.boot_completed"
      if ($b -match "1") { Start-Sleep 10; Log "La caja arranco bien." "Green"; return $true }
    }
    Start-Sleep 5
  }
  Log "La caja no volvio a conectar. Mira la seccion 'Si algo sale mal' del README." "Red"
  return $false
}

function Limpiar {
  Titulo "1. Limpiar malware"
  $r = Shell "sh /data/local/tmp/novax/escanear.sh"
  $mal = @($r -split "`n" | Where-Object { $_ -match "^MALWARE=" } | ForEach-Object { ($_ -split "=")[1].Trim() })
  $sis = @($r -split "`n" | Where-Object { $_ -match "^SISTEMA=" } | ForEach-Object { ($_ -split "=")[1].Trim() })
  $sos = @($r -split "`n" | Where-Object { $_ -match "^SOSPECHOSA=" } | ForEach-Object { ($_ -split "=")[1].Trim() })
  if ($mal.Count -eq 0 -and $sis.Count -eq 0) { Log "No encontre el malware conocido." "Green" }
  foreach ($m in $mal) { Log "  malware: $m" "Red" }
  foreach ($s in $sis) { Log "  instalador escondido en el sistema: $s" "Red" }
  foreach ($s in $sos) { Log "  sospechosa (no la toco, revisala): $s" "Yellow" }
  if ($mal.Count -eq 0 -and $sis.Count -eq 0) { return }
  # orden importante: 1) sacar el instalador del sistema, 2) desinstalar todo, 3) reiniciar y comprobar
  foreach ($s in $sis) {
    if (-not $global:root) { Log "Sin root no puedo sacar $s del sistema." "Red"; continue }
    Log (Root "sh /data/local/tmp/novax/sacar_del_sistema.sh $s")
  }
  for ($ronda = 1; $ronda -le 3; $ronda++) {
    foreach ($m in $mal) {
      $esta = Shell "pm list packages $m"
      if ($esta -notmatch "package:$m(\s|$)") { continue }
      $u = Shell "pm uninstall $m"
      if ($u -notmatch "Success") { $u = Shell "pm uninstall -k --user 0 $m" }
      Log "  quitar $m : $u"
    }
  }
  if (Reiniciar-Y-Esperar) {
    Log "Esperando 60 s por si algo intenta reinstalarlas..."
    Start-Sleep 60
    $r2 = Shell "sh /data/local/tmp/novax/escanear.sh"
    $quedan = @($r2 -split "`n" | Where-Object { $_ -match "^(MALWARE|SISTEMA)=" })
    if ($quedan.Count -eq 0) { Log "LIMPIA: no volvio ninguna." "Green" }
    else { $quedan | ForEach-Object { Log "  VOLVIO: $_" "Red" }; Log "Corre la limpieza otra vez; si sigue, abre un 'issue' en GitHub con el registro." "Yellow" }
  }
}

function Instalar-NovaX {
  Titulo "2. Instalar NOVA X"
  $apk = Get-ChildItem (Join-Path $dir "apk\NOVAX-*.apk") | Sort-Object Name -Descending | Select-Object -First 1
  if (-not $apk) { Log "No encuentro el archivo de NOVA X en la carpeta apk." "Red"; return }
  Log "Instalando $($apk.Name)..."
  $r = A @("install", "-r", $apk.FullName)
  Log "  $r"
  if ($r -notmatch "Success") { Log "No se pudo instalar NOVA X." "Red"; return }
  A @("push", (Join-Path $dir "media\arranque.mp4"), "/data/local/tmp/novax/arranque.mp4") | Out-Null
  (Shell "sh /data/local/tmp/novax/novax_ajustes.sh") -split "`n" | ForEach-Object { Log "  $_" }
  if ($global:root) { (Root "sh /data/local/tmp/novax/novax_root.sh") -split "`n" | ForEach-Object { Log "  $_" } }
  Shell "input keyevent KEYCODE_HOME" | Out-Null
  Log "NOVA X instalado. Mira la tele." "Green"
}

function Extras {
  Titulo "3. Extras de sistema (avanzado)"
  Log "Cambia el logo de encendido, los botones de encendido y mouse del control, y agrega el reloj de respaldo." "Yellow"
  Log "Guarda respaldo de todo y se puede deshacer con la opcion 5, pero toca partes del sistema." "Yellow"
  $ok = Read-Host "Escribe SI para continuar"
  if ($ok -ne "SI") { Log "Extras omitidos."; return }
  if (-not $global:root) { Log "Esta caja no tiene root: no se pueden hacer los extras." "Red"; return }
  A @("push", (Join-Path $dir "media\bootlogo.bmp"), "/data/local/tmp/novax/bootlogo.bmp") | Out-Null
  (Root "sh /data/local/tmp/novax/extras_sistema.sh") -split "`n" | ForEach-Object { Log "  $_" }
  Reiniciar-Y-Esperar | Out-Null
}

function Restaurar {
  Titulo "5. Restaurar como venia"
  $ok = Read-Host "Esto quita NOVA X y deja el logo, los botones y el inicio originales. Escribe SI para continuar"
  if ($ok -ne "SI") { return }
  if ($global:root) { (Root "sh /data/local/tmp/novax/restaurar.sh") -split "`n" | ForEach-Object { Log "  $_" } }
  Log ("  " + (Shell "pm uninstall com.novax.launcher"))
  Reiniciar-Y-Esperar | Out-Null
}

# ------------------------------------------------------------------ menu
Clear-Host
Write-Host "  N O V A   X   ·   para TX10 Pro" -ForegroundColor White
Write-Host "  -------------------------------`n" -ForegroundColor DarkGray
if (-not (Asegurar-ADB)) { Pausa; exit }
if (-not (Elegir-Caja)) { Pausa; exit }
if (-not (Identificar)) { Pausa; exit }
while ($true) {
  Write-Host ""
  Write-Host "  1. Limpiar malware (recomendado)"
  Write-Host "  2. Instalar NOVA X"
  Write-Host "  3. Extras de sistema: logo, botones y reloj de respaldo (avanzado)"
  Write-Host "  4. Todo: 1 + 2 + 3"
  Write-Host "  5. Restaurar como venia"
  Write-Host "  0. Salir"
  $op = Read-Host "`nElige"
  switch ($op) {
    "1" { Limpiar }
    "2" { Instalar-NovaX }
    "3" { Extras }
    "4" { Limpiar; Instalar-NovaX; Extras }
    "5" { Restaurar }
    "0" { break }
  }
  if ($op -eq "0") { break }
}
Log "`nRecuerda apagar la depuracion por red en NOVA X: Ajustes > Informacion." "Yellow"
Log "Registro guardado en: $log"
Pausa
