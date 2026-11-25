# Build a MarketZed Debug APK with GitHub Actions

This project ZIP now includes a GitHub Actions workflow at
`.github/workflows/android-debug.yml`.

## Upload the project to GitHub

1. Extract this ZIP on your phone or computer.
2. Open the extracted `mz_project` folder.
3. Upload the **contents** of `mz_project` to the root of a GitHub repository
   (do not upload the outer folder as an extra nested directory).
4. Commit the files to the `main` branch.

## Run the APK build

1. Open the GitHub repository.
2. Open the **Actions** tab.
3. Select **MarketZed Android Debug APK**.
4. Tap **Run workflow** and choose `main`, or push a new commit to `main`.
5. When the run finishes successfully, open the run and download the
   **MarketZed-Debug-APK** artifact.

The APK will be inside the downloaded artifact ZIP. Extract it and install the
`.apk` file on your Android device. If Android asks, allow installation from
the app you used to open the file.

## Project-specific notes

- The project has no Gradle wrapper script or wrapper JAR, so the workflow
  installs the exact Gradle version declared in
  `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.3.1).
- The app compiles against Android SDK 36.1. The workflow installs that SDK.
- This project explicitly points its debug build type at a root-level
  `debug.keystore`; the workflow creates a temporary standard debug key for
  the CI build. This key is not a production/release signing key.
- `app/google-services.json` is already included in the project archive.
  Keep Firebase configuration aligned with the Firebase Android app whose
  package name is `com.techgu.marketzed`.
- The workflow only builds `assembleDebug`. It does not create a Play Store
  release APK and does not verify live payment processing or backend behaviour.

## If the workflow fails

Open the failed Actions run, expand the first red step, and copy the complete
error text. A successful compile is not by itself proof that every app feature
works correctly.
