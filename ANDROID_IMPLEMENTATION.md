# Android implementation notes

## Project

- Application ID: com.vibesync.promptstudio
- Kotlin 2.3.21, Android Gradle Plugin 8.13.2, Compose BOM 2025.12.01, Ktor 3.4.3, Room 2.8.5.
- Minimum Android API 26, compile/target API 36, Java 17.
- Main packages: domain models/contracts/use cases, data Room/Keystore/provider clients/repositories, and presentation Compose screens/ViewModels.

## Configure and build

Open the repository in Android Studio with a JDK 17 installation and Android SDK platform 36. Let Gradle sync dependencies, then run the app configuration for the app module.

The optional generic OAuth 2.0 Authorization Code + PKCE flow is disabled unless the app is built with all of these Gradle properties:

- VIBESYNC_OAUTH_CLIENT_ID
- VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT
- VIBESYNC_OAUTH_TOKEN_ENDPOINT

The endpoints must be HTTPS and must be issued for an OAuth client registered and authorized by the provider. Do not put API keys or client secrets in these values. Build properties are configuration, not a mechanism for making an unsupported provider OAuth flow valid.

## Security choices

- API key values are AES/GCM encrypted before being persisted in preferences. The non-exportable AES key is generated in Android Keystore; hardware backing depends on device capabilities.
- OAuth PKCE verifier and state are stored in the same encrypted vault and removed after callback handling.
- OAuth access tokens are stored separately as identity credentials. The model execution engine only reads provider API keys.
- Cleartext traffic and automatic network redirects are disabled.
- The app does not include telemetry, proxy services, keys, or client secrets.
- The Compose token counter is an approximation based on characters per token. Cost rates are user-entered instead of silently using potentially stale prices.

## Provider integration

The app sends HTTPS requests directly to OpenAI Chat Completions, Anthropic Messages, and Gemini streamGenerateContent, parses SSE data events, and maps common HTTP/auth/rate-limit/network/timeout failures to UI-friendly errors. Provider model identifiers and prices can change; use a model identifier currently enabled for the API account and enter current per-million-token prices for estimates.

## Verification still required

Run unit tests and a debug/release build in Android Studio or CI with SDK 36 installed. Also verify the deployed OAuth client/redirect registration and test streaming, rate limits, malformed provider events, network loss, process recreation, and Keystore behavior on representative physical devices. This environment did not run an Android Gradle build or physical-device tests.
