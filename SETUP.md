# EV Charging Mobile - Setup Guide

## Quick Fix for Build Errors

The build errors you're seeing are now fixed! Here's what I've done:

### ✅ Fixed Issues:

1. **AndroidX Dependencies Error**
   - Created `gradle.properties` with `android.useAndroidX=true`
   - Added `android.enableJetifier=true` for third-party library compatibility

2. **Missing google-services.json**
   - Created placeholder `google-services.json` file
   - Added placeholder Google Maps API key

3. **Missing Dependencies**
   - Added `androidx.coordinatorlayout:coordinatorlayout:1.2.0`

4. **Missing Launcher Icons**
   - Created adaptive icon resources
   - Added proper drawable resources

## Next Steps:

### 1. Get Your Google Maps API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable "Maps SDK for Android" API
4. Create credentials (API Key)
5. Restrict the API key to your app's package name: `com.evcharge.mobile`

### 2. Update API Key
Replace the placeholder API key in these files:
- `app/google-services.json` (line 23)
- `app/src/main/res/values/strings.xml` (line 127)

### 3. Update Backend URL (if needed)
In `app/build.gradle`, line 20:
```gradle
buildConfigField "String", "BASE_URL", "\"http://your-backend-url:port\""
```

### 4. Build and Run
1. **Clean Project**: Build → Clean Project
2. **Sync Project**: File → Sync Project with Gradle Files
3. **Build**: Build → Rebuild Project
4. **Run**: Click the Run button (green play icon)

## Expected Result:
- ✅ Build should complete successfully
- ✅ App should install on emulator/device
- ✅ Login screen should appear
- ✅ All features should work (with proper backend)

## Troubleshooting:

If you still get errors:
1. **Invalidate Caches**: File → Invalidate Caches and Restart
2. **Clean Build**: Build → Clean Project, then Build → Rebuild Project
3. **Check API Key**: Make sure Google Maps API key is valid and restricted properly

## Testing the App:

### Debug Login (Development Only):
- Username: `123456789V`
- Password: `password123`

### Features to Test:
1. **Owner Registration** - Create new account
2. **Owner Login** - Login with credentials
3. **Dashboard** - View stats and map
4. **Booking Creation** - Create new booking
5. **QR Code Generation** - Generate QR for approved booking
6. **Operator Login** - Login as operator
7. **QR Scanning** - Scan booking QR codes

The app is now ready to build and run! 🚀
