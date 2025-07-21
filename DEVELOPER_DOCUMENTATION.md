# QR Wallet - Developer Documentation

## Overview

QR Wallet is a simple, secure Android application for storing and managing QR codes locally on the device. The app focuses on privacy and simplicity, storing all data locally without any cloud synchronization.

## Architecture

### Current Architecture Pattern
The application uses a **simplified MVVM-like pattern** with Jetpack Compose:

- **View Layer**: Jetpack Compose UI components with declarative state management
- **State Management**: `mutableStateListOf` for reactive UI updates
- **Data Layer**: Direct file-based persistence using JSON serialization
- **Business Logic**: Integrated within the MainActivity and UI components

### Key Components

#### MainActivity
- **Purpose**: Main entry point and orchestration layer
- **Responsibilities**:
  - Camera permission management
  - QR code scanning coordination
  - UI state management and data flow
  - App lifecycle management
  - Migration handling

#### CodesWriter
- **Purpose**: File-based data persistence
- **Features**:
  - JSON serialization/deserialization
  - Thread-safe operations with coroutines
  - Automatic QR code naming
  - CRUD operations for QR codes

#### AppMigration
- **Purpose**: Version management and data migration
- **Features**:
  - Version detection and comparison
  - Automatic data format migrations
  - Backward compatibility maintenance

## Data Structure

### QRCodeData
```kotlin
@Serializable
data class QRCodeData(
    val id: String,           // UUID for unique identification
    val content: String,      // The actual QR code content/URL
    val name: String,         // User-friendly display name
    val timestamp: Long       // Creation time in milliseconds
)
```

### Storage Format
- **File**: `qr_codes.json` in app's private storage
- **Format**: JSON with structured QRCodeList wrapper
- **Location**: `Context.getFilesDir()/qr_codes.json`
- **Permissions**: Private to app (no external access)

## Key Features Documentation

### QR Code Scanning
- **Library**: ZXing Android Embedded (journeyapps)
- **Orientation**: Portrait-locked via `MyCaptureActivity`
- **Permission**: Runtime camera permission required
- **Process**:
  1. Permission check → Request if needed
  2. Launch ScanContract with custom options
  3. Receive scanned content
  4. Auto-generate sequential name ("QR Code 1", "QR Code 2", etc.)
  5. Add to reactive list with duplicate detection

### Data Persistence
- **Format**: JSON with kotlinx.serialization
- **Threading**: All I/O operations on Dispatchers.IO
- **Consistency**: Atomic updates (clear + addAll pattern)
- **Backup**: No automatic backup (intentional for privacy)

### UI Components

#### QRWalletApp (Main Screen)
- **State**: Local state management with remember
- **Features**: Selection mode, version info, delete confirmation
- **Layout**: Scaffold with TopAppBar and FloatingActionButton

#### QRCodeList
- **Component**: LazyColumn with itemsIndexed
- **Features**: Alternating backgrounds, selection mode, reordering
- **Performance**: Key-based composition for efficient updates

#### QRCodeItem
- **Features**: 
  - Inline editing with keyboard handling
  - QR code display with fullscreen mode
  - Selection checkbox in selection mode
  - Material Design 3 theming

### Selection Mode
- **Activation**: Long press on any QR code item
- **Features**:
  - Multi-select with checkboxes
  - Bulk delete with confirmation dialog
  - Single-item reordering (up/down arrows)
  - Visual feedback with different card colors

### QR Code Display
- **Generation**: ZXing QRCodeWriter with custom parameters
- **Sizes**: 300dp for inline, 500dp for fullscreen
- **Format**: Bitmap with RGB_565 for memory efficiency
- **Error Handling**: Graceful degradation with null return

## Technical Implementation

### State Management Pattern
```kotlin
// Reactive list for automatic recomposition
private val qrCodes = mutableStateListOf<QRCodeData>()

// UI state with remember for composition-local state
var isSelectionMode by remember { mutableStateOf(false) }
var selectedCodes by remember { mutableStateOf(setOf<String>()) }
```

### Coroutine Usage
- **Scope**: `lifecycleScope` for activity-bound operations
- **Context**: `Dispatchers.IO` for file operations
- **Pattern**: launch { /* suspend operations */ }

### Permission Handling
```kotlin
// Modern permission request with ActivityResultContracts
private val requestCameraPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted -> /* handle result */ }
```

## Build Configuration

### Dependencies
- **UI**: Jetpack Compose BOM 2024.12.01
- **QR Scanning**: ZXing Android Embedded 4.3.0
- **QR Generation**: ZXing Core 3.5.1
- **Serialization**: kotlinx-serialization-json 1.6.0
- **Target SDK**: Android 35 (API level 35)
- **Min SDK**: Android 24 (API level 24)

### Gradle Configuration
- **Language**: Kotlin 2.1.0
- **Compose Compiler**: Kotlin compiler plugin
- **Build Types**: Debug and Release
- **Features**: BuildConfig enabled for version access

## Code Style Guidelines

### Naming Conventions
- **Classes**: PascalCase (`MainActivity`, `QRCodeData`)
- **Functions**: camelCase (`checkCameraPermissionAndOpen`)
- **Properties**: camelCase (`isSelectionMode`, `selectedCodes`)
- **Constants**: UPPER_SNAKE_CASE in companion objects
- **Composables**: PascalCase (`QRWalletApp`, `QRCodeItem`)

### Documentation Standards
- **Public APIs**: Complete JavaDoc with @param, @return, @throws
- **Private methods**: Concise inline documentation
- **Complex logic**: Step-by-step explanation in comments
- **Architecture decisions**: Documented in code comments

### Error Handling
- **Pattern**: Try-catch with logging
- **Logging**: Android Log.d/Log.e with consistent tags
- **User Feedback**: UI state updates for error communication
- **Graceful Degradation**: Null returns and safe defaults

## Testing Strategy

### Testable Components
1. **QRCodeData**: Unit tests for data class behavior
2. **CodesWriter**: Integration tests for file operations
3. **AppMigration**: Version migration logic testing
4. **QR Generation**: Bitmap generation validation

### Testing Challenges
- **Compose UI**: Requires Compose testing framework
- **File I/O**: Needs temporary directories or mocking
- **Camera**: Requires device testing or mocking
- **Permissions**: Runtime permission simulation

## Performance Considerations

### Memory Management
- **Bitmap Recycling**: Automatic with modern Android
- **List Efficiency**: LazyColumn for large datasets
- **State Optimization**: Minimal recomposition with keys
- **Cache Strategy**: No in-memory caching (file system reliance)

### Storage Efficiency
- **JSON Format**: Human-readable but larger than binary
- **Compression**: Not implemented (trade-off for simplicity)
- **File Growth**: Linear with QR code count
- **Limits**: No artificial limits (OS storage constraints only)

## Security & Privacy

### Data Protection
- **Storage**: Private app directory (no root/external access)
- **Transmission**: No network communication
- **Backup**: Not included in Android backups
- **Encryption**: Relies on Android's file system security

### Permission Model
- **Camera**: Runtime permission (required for scanning)
- **Storage**: Uses scoped storage (no external permissions needed)
- **Network**: No network permissions requested

## Development Workflow

### Setup Requirements
1. Android Studio Arctic Fox or later
2. Kotlin 2.1.0 support
3. Android SDK 35
4. Device/emulator with camera for testing

### Build Commands
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
./gradlew test            # Run tests
```

### Code Quality
- **Linting**: Built-in Kotlin lint rules
- **Formatting**: Android Studio default formatting
- **Static Analysis**: Lint warnings addressed
- **Documentation**: JavaDoc for all public APIs

## Future Enhancement Ideas

### Potential Features
- **Export/Import**: QR code backup to external files
- **Categories**: Organize QR codes by type
- **Search**: Find QR codes by name or content
- **History**: Track scan history
- **Widgets**: Home screen QR code widgets

### Architecture Evolution
- **MVVM**: Proper ViewModel with Repository pattern
- **Dependency Injection**: Hilt for better testability
- **Database**: Room for complex queries
- **Modularization**: Feature-based modules

### Technical Improvements
- **Caching**: Smart bitmap caching
- **Encryption**: Optional data encryption
- **Backup**: Secure cloud backup option
- **Analytics**: Privacy-focused usage analytics

## Troubleshooting

### Common Issues
1. **Camera Permission**: Check manifest and runtime permissions
2. **QR Generation**: Verify content encoding and size parameters  
3. **File Access**: Ensure proper context and directory permissions
4. **Memory**: Monitor bitmap creation for large QR codes

### Debug Logging
- **Tag Pattern**: Use class name as log tag
- **Levels**: Debug for development, Error for production issues
- **Content**: Never log sensitive QR code content
- **Performance**: Avoid excessive logging in loops

## Contributing Guidelines

### Code Standards
- Follow existing patterns and naming conventions
- Add JavaDoc for all public methods
- Include error handling for all operations
- Test changes on multiple device sizes

### Pull Request Process
1. Create feature branch from main
2. Implement changes with tests
3. Update documentation
4. Submit PR with detailed description

This documentation serves as a comprehensive guide for developers working on the QR Wallet application, covering architecture, implementation details, and development practices.
