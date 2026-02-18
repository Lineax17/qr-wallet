# QR Wallet - Developer Documentation

> ⚠️ **Disclaimer**: This project contains a significant amount of AI-generated code and serves as an experiment to test the capabilities of modern LLMs in software development. **This project is not production-ready.**

## Overview

QR Wallet is a simple Android application for storing and managing QR codes locally. The app prioritizes privacy by keeping all data on-device without cloud synchronization.

## Architecture

The application follows a **simplified MVVM-like pattern** with Jetpack Compose:

```
┌───────────────────────────────────────────────────────────────┐
│                        UI Layer                               │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────────┐  │
│  │QRWalletScreen│  │ QRCodeList  │  │   QRCodeDialogs      │  │
│  └──────┬───────┘  └──────┬──────┘  └───────────┬──────────┘  │
│         │                 │                     │             │
│         └─────────────────┴─────────────────────┘             │
│                           │                                   │
└───────────────────────────┼───────────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────────┐
│                    MainActivity                               │
│         (State Management & Coordination)                     │
│                           │                                   │
└───────────────────────────┼───────────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────────┐
│                     Data Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐    │
│  │ CodesWriter │  │ QRCodeData  │  │    AppMigration     │    │
│  │  (Storage)  │  │   (Model)   │  │    (Versioning)     │    │
│  └─────────────┘  └─────────────┘  └─────────────────────┘    │
└───────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/com/example/qr_wallet/
├── MainActivity.kt              # App entry point, permissions, state coordination
├── MyCaptureActivity.kt         # QR scanner with portrait lock
│
├── data/
│   ├── local/
│   │   └── CodesWriter.kt       # JSON file persistence for QR codes
│   ├── migration/
│   │   └── AppMigration.kt      # Version management and data migrations
│   └── model/
│       └── QRCodeData.kt        # Data model with serialization
│
├── ui/
│   ├── components/
│   │   ├── QRWalletScreen.kt    # Main screen with Scaffold, TopBar, FAB
│   │   ├── QRCodeListComponents.kt  # List, Item, EmptyState composables
│   │   └── QRCodeDialogs.kt     # Fullscreen, Delete, Version dialogs
│   └── theme/
│       ├── Color.kt             # Color definitions
│       ├── Theme.kt             # Material You theming
│       └── Type.kt              # Typography
│
└── util/
    └── QRCodeGenerator.kt       # QR code bitmap generation utility
```

## Key Components

### MainActivity

The central coordinator handling:
- **Camera permissions**: Runtime permission requests for QR scanning
- **QR code scanning**: Integration with ZXing library
- **State management**: Reactive `mutableStateListOf` for UI updates
- **Data persistence**: Coordination with `CodesWriter`

### UI Components

| Component | File | Description |
|-----------|------|-------------|
| `QRWalletScreen` | `QRWalletScreen.kt` | Main screen with Scaffold, manages selection mode and dialogs |
| `QRWalletTopBar` | `QRWalletScreen.kt` | Context-aware app bar (normal/selection mode) |
| `QRCodeList` | `QRCodeListComponents.kt` | LazyColumn displaying QR code items |
| `QRCodeItem` | `QRCodeListComponents.kt` | Individual card with edit, show/hide, selection |
| `EmptyState` | `QRCodeListComponents.kt` | Placeholder when no QR codes exist |
| `QRCodeFullscreenDialog` | `QRCodeDialogs.kt` | Large QR code display |
| `DeleteConfirmationDialog` | `QRCodeDialogs.kt` | Bulk delete confirmation |
| `VersionInfoDialog` | `QRCodeDialogs.kt` | App version information |

### Data Layer

#### QRCodeData
```kotlin
@Serializable
data class QRCodeData(
    val id: String,           // Unique identifier
    val content: String,      // QR code content/URL
    val name: String,         // User-friendly display name
    val timestamp: Long       // Creation time in milliseconds
)
```

#### CodesWriter
Handles JSON file persistence with:
- Thread-safe I/O via `Dispatchers.IO`
- Atomic save operations
- Duplicate detection on add
- Auto-generated sequential names

#### AppMigration
Manages app version transitions:
- Detects version changes
- Performs data migrations when needed
- Maintains backward compatibility

### Utility

#### QRCodeGenerator
Static utility for generating QR code bitmaps:
```kotlin
QRCodeGenerator.generate(content: String, size: Int): Bitmap?
```

## Data Storage

- **File**: `qr_codes.json`
- **Location**: App's private storage (`Context.filesDir`)
- **Format**: JSON with `QRCodeList` wrapper

Example:
```json
{
  "codes": [
    {
      "id": "qr_1234567890_1234",
      "content": "https://example.com",
      "name": "QR Code 1",
      "timestamp": 1234567890000
    }
  ]
}
```

## Key Features

### QR Code Scanning
- **Library**: ZXing Android Embedded
- **Orientation**: Portrait-locked via `MyCaptureActivity`
- **Permission**: Runtime camera permission required

### Selection Mode
Activated via long-press on any item:
- Multi-select with checkboxes
- Bulk delete
- Reorder (single item selected)

### Inline Editing
- Tap edit icon to rename
- Keyboard "Done" action saves changes

## Dependencies

- **Jetpack Compose**: UI framework
- **Material 3**: Design system
- **ZXing**: QR code scanning and generation
- **kotlinx.serialization**: JSON serialization

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and ensure build passes
5. Submit a pull request

## Code Style

- Follow Kotlin coding conventions
- Use meaningful names for functions and variables
- Keep composables focused and single-purpose
- Document public APIs with KDoc comments
