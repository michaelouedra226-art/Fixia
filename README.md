# Fixia

Application Android pour Fixia avec intégration CI/CD GitHub Actions.

## Requirements & Build
- Gradle Wrapper (`./gradlew`)
- JDK 17
- Android SDK

## GitHub Actions
Le workflow `.github/workflows/build.yml` compile automatiquement l'APK Debug et génère le Gradle Wrapper s'il n'est pas présent sur la branche.
