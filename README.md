# AppBox Android

AppBox es una tienda Android ligera con interfaz web publicada en GitHub Pages. La página carga desde internet, así que sus cambios pueden actualizarse sin reinstalar la APK de AppBox.

## Publicar apps desde la tienda

La sección **Subir APK** usa un servicio privado en Cloudflare Workers. Para entrar se requiere el PIN configurado en el servidor. El servicio publica el archivo como una GitHub Release, actualiza `web/catalog.json`, y GitHub Pages despliega el nuevo catálogo. El token de GitHub y el PIN son secretos del Worker; nunca se incluyen en el HTML ni en la APK.

1. Abre AppBox y toca **Subir APK**.
2. Escribe el PIN y completa nombre, versión, categoría, descripción y archivo APK.
3. Para actualizar una aplicación ya listada, usa el mismo nombre y aumenta la versión (por ejemplo `1.0.0` → `1.0.1`).
4. Espera la confirmación. La publicación del catálogo puede tardar unos minutos.

El APK debe ser válido, tener el mismo `applicationId` y estar firmado con la misma clave que la versión instalada para que Android lo acepte como actualización. Android siempre pedirá confirmar la instalación. El límite de carga es 94 MB por APK.

## Infraestructura

- GitHub Pages sirve la página pública y el catálogo JSON.
- GitHub Releases aloja los APK públicos para descarga.
- Cloudflare Worker valida el PIN y envía las publicaciones a GitHub con un token limitado al repositorio `blopex67-cloud/AppBox-Android`.
- La tienda no puede instalar una app automáticamente ni enviar avisos cuando está cerrada.

**Privacidad:** el PIN protege la acción de publicación, pero no hace privado el código cliente de la tienda ni oculta los APK publicados. La página servida por web y el repositorio público se pueden inspeccionar. No uses el PIN como una protección contra atacantes avanzados; si se filtra, cambia el secreto `APPBOX_PIN` en el Worker.

## Compilar AppBox

El workflow **Build AppBox APK** compila y publica la app Android. Los secretos de firma están en GitHub Actions; consérvalos para que futuras APK puedan instalarse como actualizaciones.
