# EV Charging Mobile App

A pure Android application for EV Charging Station Booking System built with Kotlin, XML Views, SQLite, and OkHttp.

## Features

### EV Owner Features
- **Self Registration & Login**: Register with NIC as primary key
- **Profile Management**: View and update profile information
- **Booking Management**: Create, modify, cancel bookings
- **Dashboard**: View booking statistics and nearby stations
- **QR Code Generation**: Generate QR codes for approved bookings
- **Station Map**: View nearby charging stations on Google Maps

### Station Operator Features
- **Login**: Operator-only login (no registration)
- **QR Code Scanning**: Scan booking QR codes to complete reservations
- **Booking Completion**: Finalize bookings after verification

## Technical Stack

- **Language**: Kotlin
- **UI**: XML Views with Material Design 3
- **Database**: SQLite with SQLiteOpenHelper
- **Networking**: OkHttp (no Retrofit)
- **JSON Parsing**: org.json
- **QR Code**: ZXing for generation and scanning
- **Maps**: Google Maps SDK for Android
- **Location**: Fused Location Provider
- **Session**: SharedPreferences for JWT storage

## Project Structure

```
app/src/main/java/com/evcharge/mobile/
├── App.kt                          # Application class
├── common/                         # Utility classes
│   ├── Result.kt                   # Result wrapper
│   ├── Prefs.kt                    # SharedPreferences helper
│   ├── Datex.kt                    # Date/time utilities
│   ├── Toasts.kt                   # Toast helpers
│   ├── Validators.kt               # Input validation
│   └── Permissions.kt              # Permission handling
├── data/                           # Data layer
│   ├── api/                        # API clients
│   │   ├── ApiClient.kt            # OkHttp client
│   │   ├── AuthApi.kt              # Authentication API
│   │   ├── OwnerApi.kt             # Owner management API
│   │   ├── BookingApi.kt           # Booking management API
│   │   └── StationApi.kt           # Station management API
│   ├── dto/                        # Data transfer objects
│   │   ├── AuthDtos.kt             # Auth request/response
│   │   ├── OwnerDtos.kt            # Owner data models
│   │   ├── BookingDtos.kt          # Booking data models
│   │   └── StationDtos.kt          # Station data models
│   ├── repo/                       # Repository pattern
│   │   ├── AuthRepository.kt       # Auth operations
│   │   ├── OwnerRepository.kt      # Owner operations
│   │   ├── BookingRepository.kt    # Booking operations
│   │   └── StationRepository.kt    # Station operations
│   └── db/                         # Database layer
│       ├── UserDbHelper.kt         # SQLiteOpenHelper
│       └── OwnerDao.kt             # Owner data access
├── ui/                             # User interface
│   ├── auth/                       # Authentication screens
│   │   ├── AuthActivity.kt         # Login screen
│   │   └── RegisterActivity.kt     # Registration screen
│   ├── dashboard/                  # Dashboard screens
│   │   ├── OwnerDashboardActivity.kt    # Owner dashboard
│   │   └── OperatorHomeActivity.kt      # Operator home
│   ├── owners/                     # Owner management
│   │   └── OwnerProfileActivity.kt # Profile management
│   ├── bookings/                   # Booking management
│   │   ├── BookingListActivity.kt  # Booking list
│   │   ├── BookingFormActivity.kt  # Create/edit booking
│   │   └── BookingDetailActivity.kt # Booking details
│   ├── stations/                   # Station management
│   │   └── StationMapActivity.kt   # Station map
│   ├── qr/                         # QR code features
│   │   ├── QrCodeActivity.kt       # QR code display
│   │   └── QrScanActivity.kt       # QR code scanning
│   └── widgets/                    # Custom widgets
│       └── LoadingView.kt          # Loading indicator
└── res/                            # Resources
    ├── layout/                     # XML layouts
    ├── values/                     # Strings, colors, themes
    ├── drawable/                   # Icons and graphics
    └── menu/                       # Menu definitions
```

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Google Maps API key

### Configuration

1. **Google Maps API Key**:
   - Get a Google Maps API key from Google Cloud Console
   - Add it to `res/values/strings.xml`:
   ```xml
   <string name="google_maps_key">YOUR_API_KEY_HERE</string>
   ```

2. **Backend URL**:
   - Update the backend URL in `app/build.gradle`:
   ```gradle
   buildConfigField "String", "BASE_URL", "\"http://your-backend-url:port\""
   ```

3. **Network Security**:
   - For development, HTTP is allowed in `res/xml/network_security_config.xml`
   - For production, use HTTPS and update the configuration

### Building the App

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Add your Google Maps API key
5. Update backend URL if needed
6. Build and run

## API Endpoints

The app expects the following backend endpoints:

### Authentication
- `POST /api/auth/register` - Register new owner
- `POST /api/auth/login` - Login user

### Owner Management
- `GET /api/evowner/{nic}` - Get owner profile
- `PUT /api/evowner/{nic}` - Update owner profile
- `POST /api/evowner/{nic}/deactivate` - Deactivate owner

### Booking Management
- `POST /api/booking` - Create booking
- `GET /api/booking/owner/{nic}` - Get owner bookings
- `GET /api/booking/{id}` - Get booking details
- `PUT /api/booking/{id}` - Update booking
- `DELETE /api/booking/{id}` - Cancel booking
- `GET /api/booking/dashboard/{nic}` - Get dashboard stats
- `POST /api/booking/complete` - Complete booking (operator)

### Station Management
- `GET /api/station` - Get all stations
- `GET /api/station/nearby` - Get nearby stations
- `POST /api/station/availability` - Check availability

## Database Schema

### Owners Table
```sql
CREATE TABLE owners (
    nic TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

## Key Features

### Validation
- NIC validation (Sri Lanka old/new formats)
- Email validation
- Phone number validation (Sri Lanka formats)
- Password strength validation
- Booking time constraints

### Business Rules
- Bookings can only be created within 7 days
- Bookings can only be updated/cancelled 12+ hours before start
- Account deactivation requires backoffice approval
- QR codes generated only for approved bookings

### Security
- JWT token authentication
- Secure password handling
- Network security configuration
- Input validation and sanitization

## Testing

The app includes basic unit tests for:
- Validation utilities
- Database operations
- API client functionality

Run tests with:
```bash
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please contact the development team or create an issue in the repository.
