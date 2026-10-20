# VibeSync AI Dev Studio ⚡

A local-first native Android prompt engineering workspace built with Kotlin, Jetpack Compose, Material 3, Clean Architecture, MVVM, Ktor streaming, Room, and Android Keystore.

## What is implemented

- **Prompt canvas:** multiline system and user prompts, variable substitution with {{variable_name}}, saved templates, local run history, and variable-set persistence.
- **Live estimates:** character-based approximate input/output token counts; cost estimates use rates entered by the user because provider pricing and model availability change.
- **Streaming responses:** direct HTTPS SSE handling for OpenAI Chat Completions, Anthropic Messages, and Gemini streamGenerateContent.
- **Secure credential vault:** AES/GCM encryption with a non-exportable Android Keystore key, per-entry authenticated data, masked API key entry, and a Keystore status check.
- **Lifecycle-safe state:** StateFlow UI models, ViewModel-owned CoroutineScopes, cancellation support, and streaming flow backpressure.
- **OAuth foundation:** generic Authorization Code + PKCE utilities, a state-verifying deep-link callback, and a configurable token exchange. OAuth is disabled in the default build and its identity token is not used as a provider API key.
- **Material 3 UI:** system dark mode, Android 12+ dynamic colors, edge-to-edge layout, Markdown-style headings/lists/code fences, copy, and share/export.
- **Build validation:** GitHub Actions runs unit tests and lint, runs instrumentation tests on an Android emulator, and on main builds and uploads signed release AAB/APK artifacts plus the debug APK.

## Architecture

- domain/model: provider, prompt, variable, model configuration, history, and error models.
- domain/repository: storage/execution/credential contracts.
- domain/usecase: prompt validation/execution, variable substitution, approximate token/cost estimation, and credential management.
- data/local: Room templates, run history, variable sets, DAO, and database.
- data/security: Android Keystore AES/GCM vault and PKCE cryptographic utilities.
- data/remote: Ktor OkHttp client, provider request encoding, SSE parsing, and error mapping.
- data/repository: repository implementations and Room/domain mappings.
- presentation: Compose screens, lifecycle-aware ViewModels, OAuth callback activity, and Material 3 theme.
- VibeSyncApplication: lightweight application-scoped dependency container.

## Build requirements

- Android Studio with JDK 17
- Android SDK platform 36 and build tools 36.0.0
- Gradle 8.13

Open the repository in Android Studio after installing the listed SDK and Gradle version, or use Gradle from a terminal:

- Compile debug: gradle :app:assembleDebug
- Unit tests: gradle :app:testDebugUnitTest
- Lint: gradle :app:lintDebug

GitHub Actions executes these checks on pushes and pull requests. The generated debug APK is available as a workflow artifact when the job passes.

## Provider API keys

In Settings, select a provider and save its API key. The value is encrypted with AES/GCM before being persisted. Provider requests go directly from the device to the provider over HTTPS; there are no built-in proxies, telemetry, or embedded credentials.

A ChatGPT subscription is not an OpenAI API key. For OpenAI API requests, use an API key issued for the API platform with the required API billing configuration. A ChatGPT OAuth identity token is deliberately kept separate from API keys.

## Optional OAuth configuration

The default build intentionally disables OAuth until you have an OAuth client registered and authorized for this Android app. Set these Gradle properties in a local Gradle properties file or the build environment:

- VIBESYNC_OAUTH_CLIENT_ID
- VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT
- VIBESYNC_OAUTH_TOKEN_ENDPOINT

Both endpoints must be HTTPS. The app uses the redirect URI vibesync://auth/callback; register that exact URI with the identity provider if supported. The implementation performs PKCE S256 and validates a random state value before exchanging the code. Do not add provider client secrets to the APK. Configuring these properties does not make an unsupported provider OAuth flow valid, and the resulting OAuth token is not used to authenticate provider model requests.

## Security notes

- Cleartext network traffic and automatic redirects are disabled.
- No API keys, OAuth client secrets, or external proxy dependencies are embedded.
- Android Keystore may be hardware-backed or software-backed depending on device capabilities; the app reports what the platform says.
- If Android Keystore state is lost or invalidated, previously encrypted credentials may need to be re-entered.
- Token counts are estimates, not exact model tokenizer counts or provider-reported usage.

## Verification status

A CI workflow is included for Gradle compilation, Android lint, and unit tests. Physical-device testing of API provider accounts, OAuth registration, interruption/retry behavior, and hardware-backed Keystore behavior remains necessary before a production Play Store release.

## License

Copyright (c) 2026 Vibesyncrotechnologies Pvt Ltd. All rights reserved.

PROPRIETARY AND CONFIDENTIAL. This software, its source code, assets, UI designs, and related documentation are the sole proprietary property of Vibesyncrotechnologies Pvt Ltd. Unauthorized copying, reverse engineering, redistribution, sublicensing, or public deployment without explicit written consent is prohibited.

## Developer diagnostics, connectivity, and release features

- **Developer Diagnostics** is available from the prompt screen and Settings. It stores a bounded local record history for app-owned events, provider HTTP status/duration/first-stream-chunk/failure metadata, vault results, connectivity transitions, and uncaught crash summaries. Prompts, generated responses, API keys, and authorization headers are not intentionally recorded.
- Diagnostic exports are written under app-private storage and shared with temporary FileProvider read permission. The viewer can filter severity, component, keyword, and time range. Android restricts full device-wide Logcat access for ordinary apps; use Android Studio/ADB or an Android bug report when system logs are required.
- Connectivity status distinguishes offline, connecting/unvalidated, and validated internet access. Local prompt editing, saved templates, and local history remain available offline.
- Google Play In-App Updates and In-App Review are best-effort integrations. They are not available on every sideloaded installation and do not block core app use.
- Google Mobile Ads banner and interstitial components use Google's preferred Mobile Ads Next-Gen SDK 1.5.0. UMP consent is checked before ads are requested. Debug builds use Google's sample app/banner/interstitial identifiers; release builds use the configured publisher IDs. Interstitials are considered only after at least three successful prompt runs and no more than once per ten minutes, and are skipped if no ad is already loaded.
- Google Play reviews are requested only after repeated successful use. Google Play chooses whether to show its review dialog.
- Version defaults: VERSION_CODE=20261023, VERSION_NAME=1.0.4. If the Google Play Console has a later used version code, set a greater -PVERSION_CODE=... before release.

### Ad privacy and configuration

Google's User Messaging Platform (UMP) handles consent-information refresh and any configured required consent form. If UMP reports a privacy-options entry point is required, Settings > Developer tools / Ad privacy choices opens that form. Publisher-specific privacy messages and policy settings must still be configured in the AdMob account; inclusion of the SDK alone does not prove that the publisher's account consent configuration is complete.

Ad IDs are split by build variant:
- Release App ID: ca-app-pub-4783826505860771~3225002433
- Release banner: ca-app-pub-4783826505860771/3927535003
- Release interstitial: ca-app-pub-4783826505860771/4941233773

The debug build uses the official Google sample IDs. Do not click live release ads during testing. CI validates code/configuration and APK/AAB signatures, but real ad serving and consent presentation still require an appropriate test device, network access, and a correctly configured AdMob account.

### CI artifacts and tests

CI runs unit tests and lint, boots an API 36 Google APIs emulator for Android instrumentation tests, builds debug and signed release outputs on main, verifies the release package/version/signature, and uploads the release bundle, APKs, and (when present) R8 mapping file.


### Offline Developer Toolbox (initial phase)

The searchable toolbox is separate from the prompt editor and stores favorites/recent tools locally. The initial offline tools include JSON format/minify, XML formatting with external entities disabled, bounded text diff, Base64, URI component encoding, JSON Pointer, linear-time RE2/J regex testing with capture groups, JWT header/payload decoding (explicitly unverified), SHA-256/SHA-512 digests, capped UUID generation, UTC timestamp conversion, HEX contrast ratios, Unicode inspection, text transformations, and compact HTTP status/MIME references. The URI inspector redacts common secret query values and never launches a link. Tools enforce input/output bounds and have unit-test coverage. Tools not listed here remain future implementation work; this is not yet the entire catalogue proposed in the master prompt.
