# AppBox Android

AppBox es una tienda Android ligera. La app carga su página desde GitHub Pages, por lo que los cambios de la tienda no requieren reinstalar la APK. El código de la página se entrega al navegador y el repositorio de este proyecto es público; no guardes aquí claves ni material privado.

## Publicar apps desde la tienda

La sección **Subir APK** llama a un Worker privado de Cloudflare. El PIN se valida en el servidor; después de validarlo, se guarda una sesión firmada en el navegador para que no lo solicite de nuevo en ese dispositivo/sesión. El Worker crea una GitHub Release, carga el APK y actualiza `web/catalog.json`; Pages publica el catálogo.

1. Abre AppBox, toca **Subir APK** e ingresa el PIN.
2. Completa nombre, versión, categoría, descripción y selecciona el APK.
3. Para actualizar una app existente, escribe el mismo nombre y una versión mayor (por ejemplo `1.0.0` a `1.0.1`).
4. Espera la confirmación; el despliegue del catálogo puede tardar unos minutos.

Los APK se publican en Releases públicas. No se sube el código fuente del proyecto Android; solo se publica el archivo APK, que de todos modos puede analizarse o descompilarse. Android siempre pide confirmar la instalación. Para actualizar una app instalada, su APK debe tener el mismo `applicationId` y estar firmado con la misma clave. Límite de carga: 94 MB por archivo.

## Infraestructura y seguridad

- GitHub Pages sirve el catálogo y la tienda.
- GitHub Releases aloja los APK descargables.
- El Worker `appbox-uploader` valida el PIN y usa un token de GitHub restringido a este repositorio. Token y PIN son secretos de Cloudflare, no están en el HTML ni en la APK.
- La contraseña corta puede compartirse o descubrirse; el PIN no oculta el código público ni convierte los APK públicos en privados. Cambia el secreto del Worker si se filtra.
- AppBox no puede instalar actualizaciones en silencio ni enviar avisos mientras está cerrada.

## Compilar AppBox

El workflow **Build AppBox APK** compila la aplicación Android y publica una Release. Conserva los secretos persistentes de firma para que las nuevas versiones puedan instalarse encima de las anteriores.
