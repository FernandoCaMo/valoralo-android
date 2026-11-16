# Importación de centros

Carga en Firestore (`centros/{codigo}`) los centros públicos del
"Directorio de centros docentes no universitarios de Andalucía"
(Junta de Andalucía, CC BY 4.0). Mapeo, tipos incluidos y decisiones: `docs/02-arquitectura-y-datos.md` y `docs/decisiones.md`.

## Preparar

```powershell
python -m venv tools\import\.venv
tools\import\.venv\Scripts\python.exe -m pip install -r tools\import\requirements.txt
```

## Tests

```powershell
python -m unittest discover -s tools/import
```

## Uso

Siempre hay que elegir el destino (`--dry-run`, `--emulador` o `--produccion`); sin él no se escribe nada.

```powershell
$py = "tools\import\.venv\Scripts\python.exe"

# 1. Revisar: JSON + recuento por tipo, sin escribir
& $py tools\import\importar_centros.py --provincia Huelva --dry-run --salida huelva.json

# 2. Emulador (en otra terminal: firebase emulators:start --only firestore --project demo-valoralo)
& $py tools\import\importar_centros.py --provincia Huelva --emulador 127.0.0.1:8080 --proyecto demo-valoralo

# 3. Firestore real
$env:GOOGLE_APPLICATION_CREDENTIALS = "<ruta a la clave, FUERA del repo>"
& $py tools\import\importar_centros.py --provincia Huelva --produccion
```

- Por defecto descarga el CSV del curso 2024/25. Con `--csv RUTA` usa uno local y con `--curso` indicas su curso.
- Re-importar es seguro: escribe con *merge* y solo crea `stats` vacíos en los centros nuevos.
- Si el CSV trae un tipo de centro desconocido, el script se para sin escribir nada: añádelo a `TIPOS` en `mapeo.py`.
- La clave de la cuenta de servicio nunca va al repo (el `.gitignore` excluye `service-account*.json`).
