# AppBox Android

AppBox es una tienda pequeña para Android: interfaz en HTML, empaquetada en una app WebView. El catálogo es un JSON estático servido por GitHub Pages; las versiones y los APK se descargan de GitHub Releases. No hay un servidor encendido ni una base de datos que mantener.

## Publicar una app o una actualización

1. En el repositorio de la app, crea una **Release** nueva desde GitHub y adjunta el APK. No reemplaces el archivo con otro nombre: usa el mismo nombre de APK por cada release, por ejemplo `MiJuego.apk`.
2. Abre `web/catalog.json` en este repositorio y añade o cambia una entrada dentro de `apps`. Ejemplo:

```json
{
  "id": "mi-juego",
  "name": "Mi juego",
  "iconText": "M",
  "category": "juegos",
  "categoryLabel": "Juego Android",
  "version": "1.2.0",
  "description": "Descripción breve de la app.",
  "packageName": "com.ejemplo.mijuego",
  "apkUrl": "https://github.com/USUARIO/REPO/releases/latest/download/MiJuego.apk",
  "releaseUrl": "https://github.com/USUARIO/REPO/releases/latest"
}
```

3. Guarda el cambio. Pages publica el catálogo y AppBox lo consulta al abrirse o al tocar **Actualizar**. La actualización se ve cuando termina el despliegue de Pages (normalmente unos minutos).

Las Releases, los APK y el catálogo de este repositorio son públicos. No publiques aquí aplicaciones privadas o archivos con información sensible. Para instalar una actualización, el APK nuevo debe conservar el mismo `applicationId` y estar firmado con la misma clave que la versión instalada; Android siempre muestra su confirmación de instalación.

## App Android

El workflow **Build AppBox APK** compila el APK. Se inicia desde Actions → Build AppBox APK → Run workflow e indica `version_name` y `version_code`. El APK resultante se publica como Release del propio AppBox.

La firma de AppBox se guarda en GitHub Actions Secrets y no en el código. La primera compilación requiere estos secretos: `APPBOX_KEYSTORE_B64`, `APPBOX_KEYSTORE_PASSWORD`, `APPBOX_KEY_ALIAS` y `APPBOX_KEY_PASSWORD`. No borres ni reemplaces esa clave: sin ella, Android no aceptará instalar versiones nuevas encima de AppBox.

## Catálogo y límites

- `web/catalog.json` es la lista que muestra la tienda; el catálogo inicia vacío hasta que agregues tus apps.
- El botón de descarga inicia la descarga del APK y abre el instalador de Android. Si Android lo pide, autoriza a AppBox como origen de instalación.
- GitHub Pages + Releases son el alojamiento sencillo. No ofrecen una carga APK anónima desde la interfaz pública de la tienda; la publicación se hace desde GitHub para proteger tu cuenta y evitar dejar un token secreto dentro de la app.
- No se instala nada en silencio ni se salta la firma/confirmación de Android.
