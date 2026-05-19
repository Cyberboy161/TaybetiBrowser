# Taybeti Browser

A secure Android browser with a custom in-app keyboard that completely bypasses Android's IME system, preventing keyloggers and spyware from capturing your input.

## Features

### Custom Secure Keyboard
- **No System IME**: Completely bypasses Android's input method system
- **Multi-language Support**: English, German (Deutsch), Kurdish Sorani (کوردی)
- **Hold-to-delete**: Press and hold backspace for continuous deletion with accelerating speed
- **Visual Feedback**: Keys highlight when pressed
- **Minimize Button**: Hide keyboard to free up screen space

### Privacy Features
- **HTTPS-Only Mode**: All connections upgraded to HTTPS by default
- **DNS over HTTPS**: Encrypted DNS queries (Cloudflare, Quad9, NextDNS)
- **Strict Tracking Protection**: Blocks fingerprinting, cryptomining, social trackers
- **First-Party Cookies Only**: Third-party cookies rejected
- **Safe Browsing**: Google Safe Browsing enabled
- **Bounce Tracking Protection**: Blocks redirect-based tracking

### Security Hardening
- **FLAG_SECURE**: Prevents screenshots, screen recording, and app switcher previews
- **Excluded from Recents**: App doesn't appear in recent apps list
- **No Backup**: Data extraction rules exclude all app data from backup
- **No Cleartext Traffic**: Network security config blocks all HTTP
- **Minimal Permissions**: Only INTERNET and ACCESS_NETWORK_STATE
- **Session Cleanup**: Data purged on app background/exit

## Screenshots

| Keyboard Layout | Language Switching |
|-----------------|-------------------|
| English QWERTY  | EN → DE → Kurdish |

## Installation

### Build from Source

**Prerequisites:**
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35

**Steps:**
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/TaybetiBrowser.git

# Navigate to project directory
cd TaybetiBrowser

# Build debug APK
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Keyboard Layout

### Bottom Row Controls
- **123**: Switch to numbers/symbols keyboard
- **🌐**: Cycle through languages (EN → DE → Kurdish)
- **Space**: Insert space character
- **▼**: Minimize/hide keyboard
- **Go**: Submit/confirm input
- **⌫**: Delete character (hold for continuous delete)

### Special Keys
- **⇧**: Toggle shift for uppercase letters
- **↵**: Enter/newline (number keyboard)
- **German**: ü, ö, ä, ß
- **Kurdish**: و, ە, ر, ت, ی, ۆ, پ, چ, ژ, ن, م, ه, ێ, ل, ک, گ, س, ب, ف, ئ, ش, ڕ, ق, د, ج, خ, ح, ز

## Architecture

```
TaybetiBrowser/
├── app/
│   └── src/main/
│       └── java/com/Taybetibrowser/
│           ├── MainActivity.kt         # Main browser activity
│           ├── TaybetiApplication.kt   # Application class
│           ├── keyboard/
│           │   └── SecureKeyboardView.kt  # Custom secure keyboard
│           ├── security/
│           │   └── AntiScreenshot.kt  # Screenshot prevention
│           ├── blocking/
│           │   └── FilterListManager.kt  # Content blocking
│           └── settings/
│               └── SettingsActivity.kt   # Settings screen
```

## Security Model

### How the Custom Keyboard Works

1. `NoSystemKeyboardWebView.onCreateInputConnection()` returns `null` — this blocks the system IME from attaching
2. JavaScript is injected into web pages to intercept touch/click events on input fields
3. When an input field is focused, our `SecureKeyboardView` is shown instead
4. Key presses inject characters directly via JavaScript or EditText methods
5. Keystrokes never leave the app's process space

### Key Security Properties

| Property | Implementation |
|----------|----------------|
| No system keyboard | `onCreateInputConnection` returns null |
| Input interception | JavaScript touchstart/click handlers |
| No clipboard | Custom keyboard doesn't use clipboard |
| No screenshots | FLAG_SECURE window flag |
| No recent apps | excludeFromRecents in manifest |

## Contributing

Contributions are welcome! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to submit pull requests.

## License

This project is provided as-is for educational and personal use. See [LICENSE](LICENSE) for details.

## Changelog

### v1.0.0 (Beta)
- Custom secure keyboard with EN/DE/Kurdish support
- Hold-to-delete with accelerating speed
- Keyboard minimize button
- Visual button press feedback
- HTTPS-only browsing
- DNS over HTTPS
- Strict tracking protection