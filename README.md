# Nearo for Android

Nearo is a port of KDE Connect for Android with additional features, including a Material You interface and Mac Spaces controls. It connects your Android phone or tablet to Nearo on macOS and to devices running KDE Connect.

KDE Connect provides the underlying pairing and encrypted communication, file and clipboard sharing, notification replies, media controls, and remote input.

## Installation

Download `Nearo.apk` on a device running Android 7.0 or newer. Open Nearo on Android and Nearo or KDE Connect on the other device. Select the other device under **Devices** and accept the pairing request. [Download the Android APK](https://github.com/anwarulislam/nearo-android/releases/latest)

## Development

Build a debug APK with JDK 21 and the Android SDK installed.

```sh
./gradlew assembleDebug
```

Report Nearo issues and submit changes in this repository. [Issues](https://github.com/anwarulislam/nearo-android/issues) · [Pull requests](https://github.com/anwarulislam/nearo-android/pulls)

## Credits

**Anwarul Islam** is the main contributor to Nearo. [GitHub profile](https://github.com/anwarulislam)

The original KDE Connect developers and their roles are listed under **Settings → About Nearo → KDE Connect development**. The KDE localization teams provided the upstream translations. [KDE Connect for Android](https://invent.kde.org/network/kdeconnect-android/)

## License

See [COPYING](COPYING) and the [license texts](LICENSES/) for the terms that apply to this project.
