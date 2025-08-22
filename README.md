# BLEScanner

BLEScanner is a user-friendly Android application built with Kotlin, designed to scan, discover, and interact with nearby Bluetooth Low Energy (BLE) devices. This project is ideal for developers, testers, and enthusiasts working with IoT hardware, BLE peripherals, or Bluetooth debugging.

## Features

- **Device Discovery:** Scan for BLE devices and view detailed information (name, MAC address, RSSI).
- **Modern Kotlin Codebase:** Built using best practices in Kotlin and Android development.
- **Material UI:** Clean, intuitive interface following Material Design guidelines.
- **Permission Handling:** Handles Bluetooth and location permissions gracefully.
- **Filter & Sort:** Easily filter and sort discovered devices for quick access.
- **Extensible Architecture:** Modular codebase for easy feature addition and maintenance.

## Screenshots

_Add screenshots here to showcase the app UI._

## Getting Started

### Prerequisites

- Android Studio (latest recommended)
- Android device or emulator with BLE support (Android 6.0+ required for BLE scanning)
- Kotlin 1.5+ (project uses modern Kotlin features)

### Installation

1. **Clone the repository:**
    ```bash
    git clone https://github.com/pluzarev-nemanja/BLEScanner.git
    ```
2. **Open in Android Studio:**
    - Select "Open Existing Project" and choose the BLEScanner directory.
3. **Build & Run:**
    - Connect your Android device or start an emulator.
    - Hit "Run" to build and launch the app.

### Usage

- Launch BLEScanner on your device.
- Grant Bluetooth and location permissions when prompted.
- Tap "Scan" to discover nearby BLE devices.
- Select a device to view detailed information or interact if supported.

## Project Structure

```
BLEScanner/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── ... (Kotlin source files)
│   │   │   ├── res/
│   │   │   │   └── ... (UI resources)
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── README.md
```

## Contributing

Contributions are welcome!  
- Fork the repo and create your feature branch (`git checkout -b feature/AmazingFeature`)
- Commit your changes (`git commit -m 'Add some feature'`)
- Push to the branch (`git push origin feature/AmazingFeature`)
- Open a Pull Request

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

Questions or suggestions?  
Open an issue or reach out to [pluzarev-nemanja](https://github.com/pluzarev-nemanja).

---

**Happy scanning with BLEScanner! 🚀**