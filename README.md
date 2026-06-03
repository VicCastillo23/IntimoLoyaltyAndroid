# IntimoCoffee Loyalty — Android ☕🎁

Repositorio Android del programa de **fidelidad** Intimo Coffee (copia de trabajo a partir de `IntimoCoffeeLoyalty`, Jun 2025).

**Paridad UI/flujo con `IntimoCoffeeLoyalty-iOS`:** mismas pantallas, textos, alertas “Aviso”, historial simplificado, QR local, bottom sheet de cupones, campos `IntimoOutlinedField`, versión mostrada `Intimo Loyalty v1.0.0`.

Aplicación Android para clientes del programa de **fidelidad** de Intimo Coffee Shop. Permite acumular puntos, canjear recompensas y consultar el historial de actividad.

## 🎯 Características

- **Dashboard**: Visualización de puntos acumulados, nivel de membresía y actividad reciente
- **Sistema de Recompensas**: Catálogo de premios canjeables con puntos
- **Código QR**: Generación de QR personal para identificación en tienda
- **Historial**: Consulta de transacciones y movimientos de puntos
- **Autenticación**: Registro e inicio de sesión de clientes

## 🏗️ Arquitectura

```
app/
├── core/
│   ├── datastore/        # SessionDataStore (DataStore Preferences)
│   ├── di/               # Hilt modules (NetworkModule)
│   └── navigation/       # Destinations & BottomNavDestinations
├── feature/
│   ├── auth/             # Login & Registro (Screen + ViewModel)
│   ├── dashboard/        # Panel principal con puntos y nivel
│   ├── rewards/          # Catálogo de recompensas + canje
│   ├── history/          # Historial de transacciones
│   ├── qrcode/           # Generador de QR (ZXing)
│   └── settings/         # Configuración de servidor + logout
└── ui/theme/             # Material Design 3 theming
```

## 🛠️ Stack Tecnológico

- **Kotlin 100%**
- **Jetpack Compose** - UI declarativa
- **Material Design 3** - Sistema de diseño
- **Hilt** - Inyección de dependencias
- **Retrofit + OkHttp** - Cliente HTTP
- **DataStore Preferences** - Sesión persistente
- **ZXing** - Generación de códigos QR
- **Navigation Compose** - Navegación con Bottom Navigation Bar
- **ViewModel & StateFlow** - Gestión de estado
- **Coroutines** - Programación asíncrona

## 🔗 Conexión con el Servidor

Esta app se conecta a **IntimoCoffeeApp** (el servidor POS) a través de los endpoints REST `/loyalty/*`:

- `POST /loyalty/register` - Registro de cliente
- `POST /loyalty/login` - Inicio de sesión
- `GET /loyalty/customer/{id}` - Perfil del cliente
- `GET /loyalty/customer/{id}/points` - Consulta de puntos
- `GET /loyalty/customer/{id}/transactions` - Historial
- `GET /loyalty/rewards` - Catálogo de recompensas
- `POST /loyalty/redeem` - Canjear recompensa
- `GET /loyalty/customer/{id}/qr` - Datos para QR

## 📱 Niveles de Membresía

| Nivel    | Puntos Requeridos | Beneficios                   |
|----------|-------------------|------------------------------|
| Bronze   | 0                 | Acumulación base (1pt/$10MXN)|
| Silver   | 200               | Acceso a recompensas básicas |
| Gold     | 500               | Recompensas premium          |
| Platinum | 1000              | Beneficios exclusivos        |

## 🎁 Recompensas por Defecto

- ☕ **Café Gratis** - 50 puntos
- 🍰 **Postre Gratis** - 100 puntos
- 🎉 **Descuento 20%** - 200 puntos

## 🚀 Configuración

### Prerequisitos
- **Android Studio Hedgehog** (2023.1.1) o superior
- **JDK 17**
- **Android SDK 34**

### Instalación

1. Clonar el repositorio:
   ```bash
   git clone git@github.com:VicCastillo23/IntimoCoffeeLoyalty.git
   cd IntimoCoffeeLoyalty
   ```

2. Abrir en Android Studio y sincronizar Gradle.

3. Configurar la IP del servidor en la pantalla de **Ajustes** dentro de la app (por defecto: `http://10.0.2.2:8080` para emulador).

4. Asegurarse de que **IntimoCoffeeApp** esté corriendo como servidor.

### Compilar

```bash
./gradlew assembleDebug
```

### Branding (paridad con iOS)

- **Ícono de launcher y splash:** `app/src/main/branding/app_icon_source.png` (copia de iOS `Resources/Branding/app_icon_source.png` — granos de café sobre negro).
- Regenerar densidades: `python3 scripts/generate-branding-assets.py` (escala el arte al **62 %** del lienzo para la zona segura del adaptive icon de Android, paridad visual con iOS).

## 📂 Navegación

```
Login → Register
  ↓
Main Screen (Bottom Navigation)
  ├── Dashboard   (Inicio)
  ├── Rewards     (Premios)
  ├── QR Code     (QR)
  ├── History     (Historial)
  └── Settings    (Ajustes → Logout → Login)
```

## 📄 Licencia

Proyecto desarrollado con fines educativos y de demostración.

---

**IntimoCoffee Loyalty** - Programa de fidelidad para clientes ☕🎁
