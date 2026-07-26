# Publicar una versión de Kuse

Distribución fuera de Play Store: APK **release firmado** como asset de GitHub
Releases + manifiesto `docs/updates.json` servido por GitHub Pages. La app se
auto-actualiza sola con el módulo [`actualizador`](actualizador/README.md), que nunca
consulta la API de GitHub: solo esa URL estática.

## Preparativos (una sola vez)

### 1. Keystore de Kuse (fuera del repo)

La firma **debe ser estable para siempre**: todas las versiones se firman con la misma
keystore, o Android trata la actualización como otra app distinta y no la instala
encima.

```bash
keytool -genkeypair -v \
  -keystore C:/Users/marcm/kuse-release.jks \
  -alias kuse -keyalg RSA -keysize 4096 -validity 10000
```

Después, `keystore.properties` en la raíz (está gitignored; hay plantilla en
`keystore.properties.example`):

```properties
storeFile=C:/Users/marcm/kuse-release.jks
storePassword=…
keyAlias=kuse
keyPassword=…
```

Alternativa sin fichero: exportar `KUSE_STORE_FILE`, `KUSE_STORE_PASSWORD`,
`KUSE_KEY_ALIAS` y `KUSE_KEY_PASSWORD`. **Nunca** se versionan ni la keystore ni las
contraseñas.

La huella SHA-256 del certificado queda registrada en `scripts/firma_esperada.txt` la
primera vez, y el script **aborta** si un build futuro sale firmado con otra clave —
o con la de debug.

> Si se pierde esa keystore no hay vuelta atrás: ninguna instalación existente podrá
> actualizarse nunca más, habría que desinstalar y reinstalar a mano en cada móvil.

### 2. Repositorio público y Pages

El manifiesto y el APK se sirven sin autenticación, así que el repo debe ser
**público** y Pages activado desde `main`, carpeta `/docs`:

```bash
gh repo create marcmayol/kuse --public --source . --remote origin --push
gh api -X POST repos/marcmayol/kuse/pages \
  -f 'source[branch]=main' -f 'source[path]=/docs'
```

En la **primera** release el orden es: ejecutar el script (crea la Release y commitea
`docs/updates.json`) → activar Pages → comprobar que la URL responde. A partir de la
segunda, el script ya verifica la URL él solo.

## Publicar una versión nueva

1. Sube `versionCode` (y `versionName`) en `app/build.gradle.kts`. **El `versionCode`
   siempre incrementa**: es lo único que decide si hay novedad.
2. Prepara sin publicar y revisa el manifiesto que saldría:

   ```bash
   python scripts/publicar_release.py --dry-run --notas "Qué cambia…"
   ```

3. Publica:

   ```bash
   python scripts/publicar_release.py --notas "Qué cambia…"
   ```

El script construye el APK firmado, comprueba que el `versionCode` del APK coincide
con el declarado y supera al ya publicado, que el sha256 del manifiesto es el del APK
real y que la firma no ha cambiado; crea la Release con `gh`, commitea y empuja el
manifiesto, y espera a que la URL pública sirva el `versionCode` nuevo.

Si algo no cuadra, aborta **antes** de publicar nada.

## Qué ve el usuario

| Momento | Qué pasa |
|---|---|
| Al abrir la app | Comprobación silenciosa a los 4 s. Si hay versión nueva, banner en Hoy. |
| Cada `check_horas` | WorkManager comprueba con red y guarda el hallazgo para el próximo arranque. |
| Ajustes › Actualizaciones | "Buscar ahora": dice "estás al día" o el error concreto. |
| Al pulsar Actualizar | Descarga → verificación SHA-256 → instalación. |

La **primera** auto-actualización pide confirmación del sistema (Kuse todavía no es su
propio instalador registrado). A partir de ahí, en Android 12+, se aplican solas.

## Notas

- El APK se descarga a `cacheDir/actualizaciones`, privado, y solo se promueve a
  `.apk` instalable **después** de verificar el SHA-256. Un hash que no cuadra borra
  el archivo y no instala nada.
- El primer arranque tras instalar Kuse sobre la app anterior ("Yo", firmada en debug)
  requiere desinstalar antes: la firma cambia y Android no permite actualizar encima.
