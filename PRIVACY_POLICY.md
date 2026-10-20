# Privacy Policy for VibeSync AI Dev Studio

**Effective Date:** October 20, 2026  
**Last Updated:** October 20, 2026

**VibeSync AI Dev Studio** ("we," "our," or "the Application"), engineered and published by **Navyask911** and **Vibesyncro Technologies Pvt Ltd**, is built upon a strict **Privacy-First, Zero-Telemetry Mandate**. This Privacy Policy explains transparently how our application handles, processes, and protects your data.

---

## 1. Fundamental Privacy Principles

1. **Zero Cloud Telemetry:** We do not operate proprietary analytics servers, remote logging daemons, or user tracking backends. We do not collect, harvest, or sell device identifiers (IMEI, Android ID, MAC address), location data, or user behavior metrics.
2. **Absence of Third-Party Trackers:** We deliberately exclude diagnostic tracking libraries such as Firebase Analytics, Google Analytics, Firebase Crashlytics, and third-party commercial SDKs.
3. **Local-First Storage:** All prompts, execution histories, custom variable sets, and developer settings are persisted exclusively on your local device.

---

## 2. In-App "AI Multi-Browser" Operations

The Application includes a tabbed, embedded web interface powered by the native Android System WebView to facilitate access to public conversational AI engines.

### A. Authentication & User Credentials
- When you authenticate with an AI provider (such as Google Gemini, OpenAI ChatGPT, Anthropic Claude, Microsoft Copilot, DeepSeek, or Mistral) using Google, Microsoft, Apple, or email logins, **the authentication happens directly inside that specific third-party provider's official web session.**
- The Application does not intercept, log, read, or transmit your passwords, two-factor authentication codes, or session tokens.
- Authentication cookies and web session tokens are stored within your device's isolated Android System WebView sandbox via Android's native `CookieManager`.

### B. Prompt Execution & DOM Interaction
- When using the floating prompt bar, your text is injected directly into the document object model (DOM) of the active web session locally on your device.
- No intermediary proxy or cloud relay handles or records your prompt text.

### C. Third-Party Web Privacy Policies
- When accessing third-party websites within browser tabs, your interactions and generated responses are governed by the respective terms and privacy policies of those providers:
  - Google Services: [Google Privacy Policy](https://policies.google.com/privacy)
  - OpenAI: [OpenAI Privacy Policy](https://openai.com/privacy)
  - Anthropic: [Anthropic Privacy Policy](https://www.anthropic.com/privacy)
  - Microsoft: [Microsoft Privacy Statement](https://privacy.microsoft.com/)
  - DeepSeek: [DeepSeek Privacy Policy](https://www.deepseek.com/privacy)

---

## 3. Developer Mode & API Token Security

If you utilize Developer Mode:
- **Hardware-Backed Encryption:** Any API keys you input (e.g., Google AI Studio, OpenAI, Anthropic) are encrypted at rest using the **Android Keystore AES-GCM** encryption suite with non-exportable hardware keys.
- **Direct TLS Connections:** API requests connect directly to the respective official endpoints over HTTPS. No traffic passes through third-party servers or developer proxies.
- **Diagnostics & Local Logs:** The integrated Developer Diagnostics utility writes events exclusively to app-private storage. API keys, prompts, and server responses are explicitly omitted from diagnostic records. Diagnostic exports are generated only when manually initiated by the user via temporary `FileProvider` URIs.

---

## 4. Advertising & Monetization

To support independent software development, the Application displays advertisements via Google AdMob:
- **Google Mobile Ads SDK:** AdMob may collect and process pseudonymous identifiers, IP addresses, and interaction metrics in accordance with the Google Play Developer Policy.
- **Consent Management:** We implement Google's User Messaging Platform (UMP) to obtain and manage European Economic Area (EEA) and global user consent before ad loading.
- **Offline Collapse & Sponsor Fallback:** When offline, ad slots collapse automatically to a stable-height creator support card (e.g., GitHub Sponsors, Ko-fi, Buy Me a Coffee).
- **Ad Opt-Out:** You may manage ad personalization anytime through your Android device settings (**Settings > Security & Privacy > Privacy > Ads**).

---

## 5. Network Permissions

The Application requests only the minimal Android permissions necessary for operation:

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required to load web AI interfaces in browser tabs, execute developer API calls, and query Google Play for updates. |
| `android.permission.ACCESS_NETWORK_STATE` | Used locally by `ConnectivityManager` to detect connectivity changes and switch between online features and the offline sponsor fallback. |

---

## 6. Data Control & Retention

You retain complete control over all data stored within the Application:
- **Clearing Web Sessions:** You can delete all cached browser cookies and active sessions by navigating to **Android Settings > Apps > VibeSync AI Dev Studio > Storage > Clear Data / Clear Cache**.
- **Erasing API Keys & History:** You can clear saved API keys, templates, and execution records directly inside the app settings at any time.
- **Uninstalling:** Removing the Application permanently purges all databases, encrypted vaults, and local preferences from the device.

---

## 7. Children's Privacy

VibeSync AI Dev Studio is not intended for or directed at children under the age of 13 (or under 16 in applicable jurisdictions). We do not knowingly collect personal information from children.

---

## 8. Policy Updates

We may update this Privacy Policy to reflect app updates or evolving legal requirements. Changes will be published within the repository with an updated effective date.

---

## 9. Contact & Inquiries

For legal, technical, or privacy-related questions concerning VibeSync AI Dev Studio, please submit an issue through our official GitHub repository:

* **GitHub:** [https://github.com/navyask911/VibeSync-AI-Dev-Studio](https://github.com/navyask911/VibeSync-AI-Dev-Studio)  
* **Owners:** **Navyask911** & **Vibesyncro Technologies Pvt Ltd**
