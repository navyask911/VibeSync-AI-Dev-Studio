# VibeSync Prompt Studio ⚡

An offline-first, native Android prompt engineering environment and AI orchestration playground built with Jetpack Compose, Material 3, Clean Architecture, and Ktor.

Designed for engineers, researchers, and creators to construct, benchmark, and deploy multi-turn system prompts and complex AI workflows directly on mobile with zero tracking and full cryptographic credential isolation.

---

## 🌟 Key Highlights

- **Local-First Architecture:** Prompts, session logs, variable bindings, and workspace templates reside entirely on your device.
- **Dynamic Variable Injection:** Insert dynamic token variables (`{{variable_name}}`) into system instructions and evaluate them in real-time.
- **Zero-Knowledge Credential Vault:** API keys and OAuth tokens are secured using the Android Keystore system with AES-256 GCM encryption.
- **Multi-Model Provider Compatibility:** Built to communicate seamlessly with OpenAI (GPT-4o, o1, o3-mini), Anthropic (Claude 3.5 Sonnet), and Google (Gemini 2.5/Flash) endpoints.
- **Material You Design (Material 3):** Fluid adaptive layouts supporting dynamic theming, predictive back gestures, and dual-pane foldable screens.

---

## 🏗️ Architecture & Technical Stack

The project adheres to strict **Clean Architecture** patterns separated into three isolated layers:


```

VibeSync-Prompt-Studio/
├── app/
│   └── src/main/
│       ├── java/com/vibesync/promptstudio/
│       │   ├── data/             # Network clients, encrypted storage, repositories
│       │   │   ├── local/        # Encrypted DataStore, Room DB, Keystore helpers
│       │   │   ├── remote/       # Ktor HTTP client, SSE streaming engines
│       │   │   └── repository/   # Repository implementation layers
│       │   ├── domain/           # Pure Kotlin business rules (No Android dependencies)
│       │   │   ├── model/        # Domain entities (Prompt, Variable, ModelConfig)
│       │   │   ├── repository/   # Repository contracts / interfaces
│       │   │   └── usecase/      # ExecutePromptUseCase, TokenizerUseCase, etc.
│       │   └── presentation/     # UI layer (Jetpack Compose, MVVM)
│       │       ├── canvas/       # Interactive Prompt Workspace & Tokenizers
│       │       ├── models/       # Model parameter configuration sheets
│       │       ├── components/   # Atomic Material 3 UI widgets
│       │       └── theme/        # Material 3 dynamic color tokens
│       └── res/                  # Vector drawables, adaptive launcher mipmaps

```

### Core Technologies
- **Language:** Kotlin 2.x with Coroutines & StateFlow
- **UI Framework:** Jetpack Compose (BOM) & Material 3
- **Network Engine:** Ktor Client with OkHttp engine and Server-Sent Events (SSE) support for streaming responses
- **Local Persistence:** Android Room Database & EncryptedSharedPreferences (AndroidX Security)
- **Dependency Injection:** Jetpack Hilt / Dagger
- **Serialization:** `kotlinx.serialization` (JSON parsing)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** OpenJDK 17 or 21
- **Android SDK:** Min SDK 26 (Android 8.0) | Target SDK 35 (Android 15)

### Build & Run
1. Clone the repository:
   ```bash
   git clone [https://github.com/navyask911/VibeSync-Prompt-Studio.git](https://github.com/navyask911/VibeSync-Prompt-Studio.git)
   cd VibeSync-Prompt-Studio

```

2. Open the project in Android Studio.
3. Allow Gradle to download dependencies and sync.
4. Run on an emulator or a physical device:
```bash
./gradlew installDebug

```



---

## 🔒 Security & Privacy

1. **Hardware-Backed Encryption:** All sensitive tokens (API keys, Custom Bearer tokens) are encrypted on-device using keys held inside the Android hardware-backed Keystore provider.
2. **No Analytics / Telemetry:** No third-party behavioral analytics, crash logging telemetry, or intermediate proxy servers are embedded. Requests travel directly from device to target API endpoints.
3. **Network Security Config:** Cleartext traffic is strictly blocked by default via manifest declarations.

---

## 🛡️ License

Copyright (c) 2026 **Vibesyncrotechnologies Pvt Ltd**. All rights reserved.

**PROPRIETARY AND CONFIDENTIAL**

This software, its source code, assets, UI designs, and related documentation are the sole proprietary property of Vibesyncrotechnologies Pvt Ltd. Unauthorized copying, reverse engineering, redistribution, sublicensing, or public deployment of this code via any medium without explicit written consent is strictly prohibited.

```

```
