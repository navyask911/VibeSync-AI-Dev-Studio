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

## Upgrade implementation (version 1.0.4)

- Android Keystore AES/GCM diagnostics use SecretKeyFactory for symmetric-key KeyInfo; AES-GCM encryption/decryption is verified independently of optional hardware-backing metadata. The vault never falls back to plaintext or a weaker cipher. If the original key is missing/invalidated while ciphertext remains, Settings offers an explicit destructive reset with a warning that credentials cannot be recovered.
- App-owned logs are bounded, locally persisted, sanitized, searchable, and exportable via FileProvider. This cannot retrieve logs from before collection began and is not device-wide Logcat.
- Provider streaming emits sanitized lifecycle diagnostics including HTTP status, duration, first chunk latency, failures, timeout, cancellation, and a safe request ID where available. Response bodies and secret headers are not persisted.
- A lifecycle-safe connectivity observer distinguishes validated internet access, a not-yet-validated network, and offline state.
- Google Play update/review APIs use best-effort flows. Review requests are gated to repeated successful use and deferred on a transition where an interstitial is started.
- AdMob uses Google Mobile Ads Next-Gen SDK 1.5.0 and UMP 4.0.0. UMP consent update/form resolution precedes Mobile Ads SDK initialization and ad requests. Debug uses Google's official test IDs; release uses the configured publisher IDs. Banner is visually separated at the bottom of the prompt screen. Interstitial is loaded asynchronously and only shown when already loaded, after at least three successful prompt runs, with a ten-minute minimum interval. No ad blocks editing, saving, network calls, or navigation.
- CI runs JVM unit tests, Android lint, Android instrumentation tests on an API 36 Google APIs emulator, and signed bundle/APK builds on pushes to main. Real-device Keystore characteristics, consent UI, ad delivery, Play update/review service behavior, and Play Console publisher compliance require on-device/account verification.
- Default version code/name for this upgrade: 20261023 / 1.0.4. Verify uniqueness against the actual Play Console before rollout.


### Offline utility catalogue initial phase

A dedicated Compose Developer Toolbox destination is linked from the prompt screen. Favorites and recently used tools persist locally. Implemented operations: JSON pretty/minified validation; secure XML formatting; bounded line diff; Base64; URL component encoding/decoding; JSON Pointer; linear-time RE2/J regular expression matching and capture groups; JWT header/payload decoding with a non-validation warning; SHA-256/SHA-512; batch UUID generation; epoch/ISO timestamp conversion; WCAG contrast calculation; Unicode/line-ending inspection; text transforms; compact HTTP status/MIME references; URI component preview with sensitive query redaction. Input size, diff line count, regex input/pattern length, match count and UUID batch count are capped. Automated tests cover normal, invalid, security and boundary cases.
