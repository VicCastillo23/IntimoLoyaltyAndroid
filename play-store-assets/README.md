# Assets para Google Play Console

Generados desde `splash_logo.png` y `logo-intimo.png` del proyecto.

## Qué subir en Play Console

| Campo en Play Console | Archivo recomendado | Tamaño |
|----------------------|---------------------|--------|
| **Ícono de la app** | `play-store-icon-512.png` | 512 × 512 px |
| **Gráfico de funciones** | `play-store-feature-graphic-1024x500-banner.png` | 1024 × 500 px |

Todos son PNG &lt; 1 MB (requisito de Google).

## Archivos extra

- `play-store-icon-512-compact.png` — ícono sin texto «LOYALTY» (más legible en tamaño muy pequeño).
- `play-store-feature-graphic-1024x500.png` — banner centrado solo con el logo completo.

## Regenerar

Desde la raíz del repo Android:

```bash
python3 - <<'PY'
# Ver script en historial o volver a ejecutar desde Cursor
PY
```

Fuente: `app/src/main/res/drawable-xxxhdpi/splash_logo.png` (768×768).
