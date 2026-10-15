# AuraLMS Android APK + online backend

This project has a Spring Boot backend and a web UI served by the same backend. The Android app is a WebView wrapper, so it uses the same interface and API rather than duplicating the application.

## 1. Deploy the backend

The repository includes `render.yaml` and a production Spring profile. To deploy:

1. Push this updated repository to your GitHub repository.
2. Create/sign in to your Render account and create a Blueprint from that repository.
3. Review the service and PostgreSQL database plan/pricing before confirming. A persistent production database generally requires a paid plan; don't rely on an expiring/free test database for real library records.
4. When prompted for `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_ADMIN_FIRST_NAME`, and `BOOTSTRAP_ADMIN_LAST_NAME`, enter the administrator account you want. Use a long, unique password. Do not commit credentials to GitHub.
5. Deploy and wait for the web service to become healthy. Copy its HTTPS URL, for example `https://your-service.onrender.com`.
6. Open that URL in a browser and verify the AuraLMS page loads. Sign in with the bootstrap admin account. The initial admin is created only when the database has no users. The app seeds sample book data if the catalog is empty.

The production profile disables H2 console access and the known demo-user seeding. It requires a generated `APP_JWT_SECRET` and a PostgreSQL database. The normal local profile retains the original H2 development setup and demo accounts.

## 2. Build the Android APK

Requirements: Android Studio with Android SDK Platform 35 and a compatible JDK (Android Studio's bundled JDK is suitable).

1. Open the `android` folder in Android Studio.
2. Wait for Gradle sync. If prompted, install Android SDK Platform 35 and accept the required Android SDK licenses.
3. Before building, set the deployed HTTPS backend URL. In Android Studio, open **File > Settings > Build, Execution, Deployment > Build Tools > Gradle** only to confirm the Gradle JDK; set the URL when invoking Gradle as shown below, or change the default in `android/app/build.gradle.kts` to your real HTTPS URL.
4. From a terminal in the `android` folder, run:

   **Windows PowerShell**
   ```powershell
   .\gradlew.bat assembleDebug -PbackendUrl=https://YOUR-REAL-SERVICE.onrender.com
   ```

   **If the Gradle wrapper is not present yet:** open the `android` folder in Android Studio, let it sync, and use **Build > Build APK(s)** after setting the URL in `android/app/build.gradle.kts`. This repository package does not include a Gradle wrapper JAR.

5. The debug APK is normally created at `android/app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer that APK to your Android phone and install it. On the phone, allow installation from that source if Android asks.

Replace `YOUR-REAL-SERVICE.onrender.com` with the exact deployed host. Do not use `localhost` in the APK: on a phone, `localhost` means the phone itself, not your computer or cloud backend.

## 3. Before real users use the app

- Change the initial administrator password after first login if the application supports password changes; otherwise keep the bootstrap password private and use an account-management workflow to rotate it.
- Review and remove any demo/sample records that should not appear in the live catalog.
- Confirm the hosting database has backups and persistent storage.
- Test login, catalog, borrow/return, reservations, fines, and admin access on an actual phone.
- The app needs an internet connection and the deployed service must be running.

## Notes

- The Android wrapper requires HTTPS and does not enable cleartext HTTP traffic.
- The APK contains the backend URL, not the database password or JWT signing secret.
- An APK is not a substitute for hosting: the Spring Boot backend and PostgreSQL database remain online services.
