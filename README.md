# NOVA X para TX10 Pro

Limpia el **malware que viene de fábrica** en la caja Android TV **TX10 Pro (Allwinner H313)** y la convierte en **NOVA X**: un sistema con inicio, ajustes y controles propios, pensado para usarse con el control remoto como un televisor.

> [!WARNING]
> Esto modifica tu caja. Se probó en **una** TX10 Pro (firmware `H313-Q-v1.0`). El instalador revisa tu caja antes de hacer cualquier cosa y **no toca nada si no es igual**. Aun así, lo usas bajo tu propia responsabilidad.

> 🇬🇧 **English summary** — [jump below](#english)

---

## ¿Qué hace?

### 1. Limpia el malware de fábrica

La TX10 Pro trae escondida una app que se hace pasar por parte de Android (`com.android.netservice`). Desde ahí **descarga e instala otras apps** que corren con permisos de sistema. En nuestra caja encontramos 9:

- Una manda cada rato la **MAC y el número de serie** de la caja a un servidor.
- Otras pueden **instalar apps**, **manejar cuentas** y **dibujar encima de la pantalla** sin pedir permiso.
- Se reinstalan entre ellas: si quitas una, otra la vuelve a poner.

El limpiador las quita **en el orden correcto** (primero el instalador escondido, después las demás), reinicia y comprueba que no vuelvan. Guarda una copia de todo lo que quita.
Detalles técnicos: [docs/MALWARE.md](docs/MALWARE.md).

### 2. Instala NOVA X

| | |
|---|---|
| **Inicio** | Tus apps como tarjetas grandes, con el color de cada app de fondo. |
| **Ajustes propios** | WiFi (buscar redes, conectarte, olvidar), sonido, pantalla, fecha e idioma, apps, almacenamiento real y control remoto. |
| **Panel rápido** | Dentro de cualquier app, el **botón del mouse** abre un panel a la derecha: cambiar de WiFi, volumen, bloqueo de anuncios, **apagado automático** e ir al inicio. |
| **Cambiar de app** | **Mantén oprimida la casita** y sale una barra abajo con tus apps recientes. |
| **Volumen de televisor** | Solo el número, arriba a la derecha. Si mantienes el botón, sube o baja rápido. |
| **Apagado** | Pregunta "¿Qué quieres hacer?", muestra una animación y apaga **del todo** (no reposo). |
| **Temporizador** | Apaga la caja en 30 min, 1 h, 1 h 30 o 2 h. Avisa 60 s antes. |
| **Bloqueo de anuncios** | DNS de AdGuard para toda la caja. |
| **Botón Netflix** | Abre la app que tú elijas (o Netflix, si no eliges ninguna). |

### 3. Extras (opcional, avanzado)

- **Logo de encendido** NOVA X y **animación de arranque** con sonido.
- **Botón de encendido** del control → abre la pregunta de apagado de NOVA X.
- **Botón del mouse** → abre Ajustes y el panel rápido (y deja de mostrar el puntero o abrir Prime Video).

Todo se puede deshacer con la opción **5. Restaurar** del instalador.

---

## Lo que necesitas

- La caja **TX10 Pro** y su control.
- Un **PC con Windows 10 u 11**, conectado a **la misma red** (WiFi o cable) que la caja.
- Recomendado: un **cable de red** (Ethernet), solo por si hace falta rescatar la caja.

## Instalación

### Paso 1. Descarga esta carpeta

Arriba en esta página: **Code → Download ZIP**. Descomprímelo en tu PC, por ejemplo en `Descargas\novax-tx10pro`.

### Paso 2. Prende la depuración en la caja

En la caja, con el control:

1. **Ajustes → Preferencias del dispositivo → Información**.
2. Baja hasta **Compilación** y oprime **OK 7 veces** seguidas, hasta que diga que eres desarrollador.
3. Vuelve atrás: aparece **Opciones de desarrollador**. Entra.
4. Activa **Depuración por USB**. Si ves **Depuración por red** o **ADB por red**, actívala también.
5. Anota la **IP** de la caja: **Ajustes → Red** (por ejemplo `192.168.1.35`).

### Paso 3. Corre el instalador

1. En la carpeta, doble clic en **`INSTALAR.bat`**.
   - Si Windows muestra "Windows protegió su PC": **Más información → Ejecutar de todas formas**.
2. La primera vez descarga las herramientas oficiales de Android (de Google), unos pocos MB.
3. Escribe la IP de la caja, o solo **Enter** para que la busque.
4. **Mira la tele**: acepta **"¿Permitir la depuración?"** y marca **"Permitir siempre desde este equipo"**.
5. El instalador revisa que tu caja sea compatible y muestra el menú:

```
  1. Limpiar malware (recomendado)
  2. Instalar NOVA X
  3. Extras de sistema: logo y botones (avanzado)
  4. Todo: 1 + 2 + 3
  5. Restaurar como venia
  0. Salir
```

Recomendado: **1** y luego **2**. Si quieres todo, **4**.
**No toques el control mientras trabaja.** La caja se reinicia sola cuando hace falta.

### Paso 4. Cierra la puerta al terminar

Apaga la depuración, para que nadie en tu red pueda entrar a la caja:
**botón del mouse → Información → Depuración por red → Desactivada.**
(Si después quieres cambiar algo, la vuelves a prender ahí mismo.)

---

## Cómo se usa

| Botón | Qué hace |
|---|---|
| **Encendido** | "¿Qué quieres hacer?" → Apagar (con animación) o Cancelar. Oprimirlo dos veces apaga directo. |
| **Casita** | Toque: inicio de NOVA X. **Mantener**: barra para cambiar de app. |
| **Mouse** (botón del medio) | En el inicio: Ajustes. Dentro de una app: **panel rápido**. Otra vez: lo cierra. |
| **Netflix** | La app que elijas en Ajustes → Control remoto. |
| **Volumen** | Número estilo televisor. |

El botón de encendido y el del mouse funcionan así solo si instalaste los **extras**. Sin ellos, puedes apagar desde la tarjeta **Apagar** del inicio y abrir Ajustes desde su tarjeta.

---

## Si algo sale mal

- **La caja arranca pero el instalador no la encuentra.** Puede que haya cambiado de IP. Mírala en Ajustes → Red y escríbela en el instalador.
- **La pantalla queda en negro después del logo, o la caja no se conecta al WiFi.** Conecta la caja al router con **cable de red**, desconéctala de la corriente y vuelve a conectarla, espera 2 minutos y corre el instalador: suele encontrarla por el cable. Elige **5. Restaurar**.
- **Nada funciona.** Modo de recuperación: con la caja desconectada, mete un palillo en el hueco **AV** hasta sentir un clic, mantenlo oprimido, conecta la corriente y **suelta apenas salga el logo**. En el menú elige **Wipe data/factory reset**. Esto borra tus apps, pero no el malware del sistema: después corre otra vez la limpieza.
- Cada vez que usas el instalador queda un registro en la carpeta `registros`. Si abres un *issue* en GitHub, adjúntalo (no trae datos personales).

## Preguntas

**¿Borra mis apps?** No. Solo quita el malware de la lista y deja todo lo demás.

**¿Por qué Netflix no funciona en esta caja?** Netflix exige cajas certificadas por Google, y esta no lo está. Eso no se puede arreglar sin saltarse la protección de Netflix, y este proyecto no lo hace.

**¿Por qué Android dice que es la versión 13?** La caja **miente**: es Android 10. En NOVA X → Información verás la versión real.

**¿Sirve para otras cajas?** Para otras cajas con el mismo chip **H313** puede funcionar, pero el instalador solo permite seguir si el firmware es igual al probado. Si tienes otra caja, abre un *issue* con el registro del instalador y lo revisamos.

---

## Para desarrolladores

```
app/          código de NOVA X (Java, sin Gradle)
caja/         scripts que corren en la caja (sh)
apk/          NOVA X compilado
media/        logo de encendido y animación de arranque
docs/         informe del malware
instalar.ps1  instalador de Windows (lo abre INSTALAR.bat)
```

Para compilar se necesita el SDK de Android (build-tools con `dx` y la plataforma 29) y un JDK:

```bash
cd app
BUILD_TOOLS=~/Android/Sdk/build-tools/30.0.3 \
ANDROID_JAR=~/Android/Sdk/platforms/android-29/android.jar \
bash build.sh
```

Si no le pasas una llave tuya (`KEYSTORE`, `KEYSTORE_PASS`), el script crea una local. Un APK firmado con otra llave no puede actualizar el `apk/` publicado: primero hay que desinstalar el anterior.

## English

**NOVA X for TX10 Pro** removes the **factory-installed malware** found on the *TX10 Pro* Android TV box (Allwinner **H313**, firmware `H313-Q-v1.0`, real Android 10 that reports itself as "Android 13") and turns the box into **NOVA X**: its own home screen, settings and remote-control features.

**The malware.** The firmware ships a hidden system app, `com.android.netservice` (in `/system/app/netserevice-release/`), signed with the public AOSP *platform test key*. Together with `com.android.assistant.setting.iknew`, it silently downloads and installs other apps that run as the system user (uid 1000). One of them sends the box's MAC address and serial number to an analytics server every few minutes. Others can install apps, manage accounts and draw over other apps. They reinstall each other, so removing just one does not work. Full report, package list and SHA-256 hashes: [docs/MALWARE.md](docs/MALWARE.md).

**What the tool does**
1. **Cleans the malware** in the right order: it first removes the hidden installer from `/system` (with a backup), then uninstalls every payload, reboots and checks that nothing comes back.
2. **Installs NOVA X** (optional): home screen, its own settings (Wi-Fi, sound, display, apps, real storage), a quick side panel inside any app, a recent-apps bar (hold Home), TV-style volume, full power-off with a sleep timer, and AdGuard DNS ad blocking.
3. **System extras** (optional, advanced): boot logo and animation, remote power button → NOVA X power-off, mouse button → settings.

The installer first **identifies the box** and refuses to change anything if it is not the tested hardware/firmware. Option 5 restores the original state.

**How to use it** (Windows 10/11, PC and box on the same network)
1. On the box: *Settings → Device Preferences → About → Build* (press OK 7 times) → *Developer options* → enable *USB debugging* (and *Network debugging* / *ADB over network* if present).
2. On the PC: *Code → Download ZIP*, extract, double-click **`INSTALAR.bat`**, accept the debugging prompt on the TV and choose from the menu (the interface is in Spanish: **1** = clean malware, **2** = install NOVA X, **4** = everything, **5** = restore).
3. When done, turn debugging off again: *mouse button → Información → Depuración por red → Desactivada*.

⚠️ Tested on **one** box. Use at your own risk. If you have a similar box (another H313 model) and the installer refuses it, open an issue with the log file from the `registros` folder.

## Autores

- **[danielj2109](https://github.com/danielj2109)**: idea, diseño de NOVA X y pruebas en la caja.
- **Claude** (Anthropic): coautor. Investigación del malware, código y scripts.

Si te sirvió, deja una ⭐ en el repositorio.

## Licencia

Código y medios (logo, animaciones, sonidos): **MIT**, ver [LICENSE](LICENSE).
Fuente **Inter** © The Inter Project Authors, licencia SIL Open Font License 1.1 ([app/assets/fonts/LICENSE-Inter-OFL.txt](app/assets/fonts/LICENSE-Inter-OFL.txt)).

NOVA X no tiene relación con el fabricante de la caja, con Google ni con ninguna de las apps mencionadas.
