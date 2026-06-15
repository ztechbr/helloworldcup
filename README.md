# World Cup 2026 - Wear OS App 🏆

A modern Wear OS application built with Jetpack Compose, designed to provide real-time World Cup 2026 data with a focus on high performance and a premium aesthetic inspired by the Samsung Galaxy Fit 3.

## 📱 Features

- **Samsung Fit 3 Design Language**: Optimized for rectangular and circular displays with a black background and high-contrast green accents (`#00FF87`).
- **Real-time Match Data**: Live scores, upcoming matches, and daily schedules via Retrofit.
- **Standings & Groups**: Complete group stage tables with dynamic point calculation.
- **Secure Data Layer**: Authenticated API requests using `BuildConfig` injection for sensitive tokens.
- **Optimized for Wear**: Uses `ScalingLazyColumn`, `SwipeToDismiss`, and Wear OS navigation for a native experience.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose for Wear OS
- **Networking**: Retrofit 2 + OkHttp + Gson
- **Architecture**: MVVM (ViewModel + State)
- **Image/Icons**: Material Icons Outlined
- **Security**: Local properties injection for API keys

## 🔐 Setup & Security

This project uses an authenticated API. To protect sensitive keys, the API token is not stored in the repository.

### Adding the API Token

1. Open the `local.properties` file in your project root (create it if it doesn't exist).
2. Add the following line:
   ```properties
   WORLD_CUP_TOKEN=YOUR_TOKEN_HERE
   ```
3. Sync the project with Gradle. The build system will automatically inject this token into `BuildConfig.WORLD_CUP_TOKEN`.

## 🚀 How to Run

1. Clone the repository.
2. Add your token to `local.properties` as described above.
3. Open in **Android Studio Ladybug** or newer.
4. Run on a **Wear OS Emulator** (Rectangular recommended for the full Fit 3 experience) or a **Physical Wear OS Watch**.

---
*Developed for the World Cup 2026 experience.*
