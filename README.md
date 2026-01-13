# WaveReader

WaveReader is an Android app that measures and visualizes ocean wave conditions in real time using your phone’s built-in motion sensors. It features live wave tracking, historical data access, and wave API data—making it a powerful tool for surfers, sailors, and ocean enthusiasts.

---

## Features

- **Real-Time Measurement**  
  Calculates wave height, period, and direction using the phone’s accelerometer, gyroscope, and magnetometer.
- **Interactive Graphs**  
  View wave data in real-time with a scrollable and zoomable custom graph that supports toggling lines for height, period, direction, and forecast prediction.
- **Location-Based Wave Data**  
  Search for wave forecasts by zip code or map, powered by the Marine Weather API and Androids Geocoding.
- **History & Export**  
  Save your measurements, filter, and export past data as CSV or JSON.
- **User Accounts**  
  Create an account to access all full features like data history and export tools.

---

## User Experience

WaveReader is designed for both casual users and experienced mariners:

- **Record Tab**: Measures waves in real-time from your current location. Customize the graph with filter options.
- **Search Tab**: Explore wave data anywhere using zip code or map search.
- **History Page**: Review past sessions, filter by location or date, and view summary trends.

---

## Requirements

- Android 8.0+
- Device with:
    - Accelerometer
    - Gyroscope
    - Magnetometer
- Internet connection for API features and saving data
- Firebase account for login features (optional)

---

## Core Technologies

### Android & UI
- **Kotlin**
- **Jetpack Compose** – UI and Canvas-based graph rendering
- **ViewModel + StateFlow** – Reactive architecture

### Sensor & Signal Processing
- **Android Sensor Framework** – Access motion sensors and orientation data

### Location & Maps
- **Google Maps SDK**
- **Fused Location Provider**
- **Android Geocoding API**

### Networking & API
- **kotlinx.serialization** – JSON parsing

### Backend & Storage
- **Firebase Authentication** – User login
- **Cloud Firestore** – Save & load session history

### Data Export
- Built-in tools for CSV and JSON export via Android Storage Access Framework

### Upcoming Features
- iOS and Android Compatibility using Compose Multiplatform

### Resourses

- [NDBC Wave Calculations](https://www.ndbc.noaa.gov/wave.shtml)
- [Marine Weather API](https://www.marineweatherapi.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
