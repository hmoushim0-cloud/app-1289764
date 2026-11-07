# تطبيق محور الصناعي — منصة الصيانة التنبؤية الذكية (`mihwar-modbus-industrial-iot-1289764`)
> منظومة industrial_iot المتكاملة مع حساسات الاهتزاز ثلاثية المحاور وبروتوكول Modbus RS485 RTU

- **Project ID**: `#1289764`
- **Commercial Archetype**: `industrial_iot`
- **Bound Hardware Peripheral**: `3-Axis Vibration Accelerometer + Modbus RS485 RTU`
- **Package Name**: `sa.factory.mihwarmodbusindust`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]
- **Summary of Completed Fixes and Features**:
  - **ModbusRtuProtocolEngine:** Implemented binary Modbus RTU RS485 serial communication protocol engine. Features Function Code 0x03 (Read Holding Registers), CRC-16 checksum calculation with polynomial 0xA001 and initial value 0xFFFF, packet serialization/deserialization, big-endian 16-bit register packing, and security validation against corrupted frames.
  - **Iso10816VibrationDiagnosticEngine:** Implemented full ISO 10816-3 mechanical vibration evaluation standards for industrial rotating machinery (Classes I through IV). Computes Euclidean triaxial RMS vibration velocity ($V_{rms} = \sqrt{x^2 + y^2 + z^2}$), classifies operational severity into Zones A (Optimum), B (Acceptable), C (Restricted / Alarm), and D (Trip / Critical Danger), and performs spectral harmonic diagnostic analysis identifying unbalance (1X), misalignment (2X), and high-frequency bearing wear.
  - **Jetpack Compose UI Hardening:** Fixed Kotlin single-quote string interpolation syntax hazards in `HomeScreen.kt` and `TelemetryScreen.kt`, resolved missing `FontWeight` and `sp` Compose imports in `MainActivity.kt`, and replaced deprecated divider components.
  - **Toolchain & Distribution:** Bundled complete Gradle wrapper scripts (`gradle/`, `gradlew`, `gradlew.bat`) and `.gitignore` for zero-friction cloud builds.
  - **Comprehensive Unit Regression Tests:** Established `MihwarModbusRegressionTest.kt` with 9 unit tests verifying Modbus CRC-16 calculation, request building, corrupted packet rejection, register encoding/decoding, triaxial RMS velocity, ISO 10816-3 severity zones, spectral failure mode diagnosis, and driver telemetry streaming.

- **System Architecture**:
  - Clean Architecture & MVVM with unidirectional data flow (`StateFlow` / `SharedFlow`).
  - Modular engine abstractions under `sa.factory.mihwarmodbusindust.engine` wired directly into `HardwareSensorDriver`.
  - WCAG 2.1 AAA contrast compliance and strict zero-emoji invariant throughout all user-facing strings and logs.

- **Build and Test Verification**:
  - Run unit tests: `./gradlew testDebugUnitTest`
  - Cloud Emulator Test: Executed on `hmoushim0-cloud/Android-test-harness` running Google Pixel 6 (API 34).
