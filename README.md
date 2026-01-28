# Stock Triggers App

A simple Android application that tracks stock market indices (default: NIFTY) and triggers "BUY" or "SELL" signals based on moving averages.

## Features
- **Configurable Ticker**: Track any stock symbol (e.g., `^NSEI`, `AAPL`).
- **Smart Triggers**:
  - **BUY**: If price drops > 1.5% below the 5-day mean.
  - **SELL**: If price rises > 3.0% above the 5-day mean.
- **Visualizations**: 6-day trend graph and animated signal cards.
- **Robustness**: Auto-fallback to Mock Data if API limits are hit.

## How to Build and Run
This project is designed to be built with **Android Studio**.

### Prerequisites
- Android Studio (latest version recommended)
- Java JDK 17 or higher (usually bundled with Android Studio)

### Steps
1. **Open** Android Studio.
2. Select **Open** and navigate to this directory:
   `/Users/suvarnasingh/gitrepos/newStockTriggers`
3. Wait for Gradle to sync. Android Studio will automatically generating the necessary wrapper files (`gradlew`) and download dependencies.
4. **Run** the app:
   - Connect an Android device via USB or create an Emulator (AVD manager).
   - Click the green **Play** button in the toolbar.

## Running Tests
To run the logic verification tests:
1. Open `app/src/test/java/com/example/stocktriggers/StockAnalyzerTest.kt`.
2. Click the Run icon next to the class or individual tests.