# NOVA X para TX10 Pro

Limpia el **malware que viene de fábrica** en la caja Android TV **TX10 Pro (Allwinner H313)** y la convierte en **NOVA X**: un sistema con inicio, ajustes y controles propios, pensado para usarse con el control remoto como un televisor.

> [!WARNING]
> Esto modifica tu caja. Se probó en **una** TX10 Pro (firmware `H313-Q-v1.0`). El instalador revisa tu caja antes de hacer cualquier cosa y **no toca nada si no es igual**. Aun así, lo usas bajo tu propia responsabilidad.

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

## Licencia

Código y medios (logo, animaciones, sonidos): **MIT**, ver [LICENSE](LICENSE).
Fuente **Inter** © The Inter Project Authors, licencia SIL Open Font License 1.1 ([app/assets/fonts/LICENSE-Inter-OFL.txt](app/assets/fonts/LICENSE-Inter-OFL.txt)).

NOVA X no tiene relación con el fabricante de la caja, con Google ni con ninguna de las apps mencionadas.
