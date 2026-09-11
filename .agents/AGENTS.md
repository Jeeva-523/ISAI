# Project Workspace Rules

- **Automated Commands**: Build commands such as `$env:JAVA_HOME="C:/Program Files/Microsoft/jdk-17.0.19.10-hotspot"; .\gradlew.bat assembleDebug` and `npm run build` are pre-approved for build & verification steps.
- **Deduplication**: Keep song deduplication active across Web and Android repositories.
- **App Update Release Workflow**: Only trigger an app update release (version bump, release APK build, Firebase update config) when the user **EXPLICITLY requests an update/release**. For regular code edits and fixes, update and verify the code normally without triggering a new release.
- **Language Preference**: Always communicate and reply to the user strictly in Tanglish (Tamil words written in English letters).
