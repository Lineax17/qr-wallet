# Changelog

All notable changes to QR Wallet will be documented in this file.

## [1.0.2] - 2026-02-19

### Description
- Major codebase modernization and cleanup

### Changed
- Refactored codebase following Clean Code principles and MVVM architecture
- Improved code organization and reduced code duplication
- Updated developer documentation
- Updated dependencies to latest stable versions:
  - androidx-core-ktx: 1.15.0 → 1.16.0
  - lifecycle-runtime-ktx: 2.8.7 → 2.9.0
  - activity-compose: 1.9.3 → 1.10.1
  - compose-bom: 2024.12.01 → 2025.02.00
  - zxing-core: 3.5.1 → 3.5.3
  - qrcode-kotlin: 4.0.6 → 4.2.0
  - kotlinx-serialization: 1.6.0 → 1.8.0

## [1.0.1] - 2025-07-27

### Description
- Interface adjustments 

## [1.0.0] - 2025-07-22

### Description
- First stable release of QR Wallet

### Added
- JavaDoc comments in the codebase

## [0.0.3] - 2025-07-21

### Description
- Third beta release of QR Wallet

### Fixed
- Reorder QR codes

## [0.0.2] - 2025-07-21

### Description
- Second beta release of QR Wallet

### Added
- QR Code deletion
- Reorder QR codes

## [0.0.1] - 2025-07-21

### Description
- First beta release of QR Wallet

### Added
- QR Code scanning
- Rename QR codes with edit button
- JSON persistent storage

### Features
- Scan and store QR codes locally
- Show/hide QR code images
- No data loss on app restart
- No internet connection required
- Material You design

