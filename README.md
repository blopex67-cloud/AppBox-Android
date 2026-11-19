# AppBox Android

AppBox es una tienda sencilla para Android: interfaz HTML empaquetada en una app WebView, alojada en GitHub Pages. Los APK viven en GitHub Releases; no hay un servidor encendido que mantener.

## Agregar una app (solo una vez)

1. Abre `web/catalog.json` y agrega un objeto dentro de `apps`:

```json
{
  "id": "mi-juego",
  "name": "Mi juego",
  "iconText": "M",
  "category": "juegos",
  "categoryLabel": "Juego Android",
  "version": "1.0.0",
  "description": "Descripción breve.",
  "githubRepo": "USUARIO/REPO"
}
```

2. Publica el cambio. AppBox consultará las GitHub Releases más recientes de cada `githubRepo` cada vez que alguien abra o actualice la tienda. La página se actualiza sola; no hay que editar el catálogo al publicar versiones posteriores.
3. Para cada nueva versión, crea una Release pública en ese repositorio, agrega el APK y usa un tag de versión como `v1.1.0`. AppBox mostrará el tag, el APK y las novedades de esa Release.

AppBox toma el primer archivo `.apk` de la Release más reciente. Conserva el mismo nombre de archivo APK entre versiones para que el navegador de la tienda lo pueda descargar sin problemas.

## Límites importantes

- La tienda consulta Releases públicas y necesita conexión a internet. Para comprobar una versión nueva, la persona abre AppBox y toca **Actualizar**; Android no permite que una tienda propia instale APKs silenciosamente.
- El APK nuevo de cada app debe conservar el mismo `applicationId` y estar firmado con la misma clave de la versión anterior; Android solicitará confirmar la instalación.
- Este repositorio y los APK publicados en Releases son públicos. No subas apps privadas ni datos sensibles.
- No hay carga de APK desde un botón público de la tienda: la publicación se hace en GitHub, evitando guardar un token de cuenta dentro de AppBox.

## Compilar AppBox

El workflow **Build AppBox APK** compila y publica la tienda como Release. Los secretos de firma están configurados en GitHub Actions; consérvalos para que futuras versiones de AppBox puedan instalarse sobre la anterior.
