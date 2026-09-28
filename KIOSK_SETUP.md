# Kiosk Setup — One-Time ADB Instructions

This only needs to be done **once** per tablet.  
After this, every time the tablet powers on it will boot straight into Wenonah's Orals Timer, locked in kiosk mode.

---

## Prerequisites

- A Mac or Windows PC with **Android Studio** (or standalone ADB) installed
- A USB cable to connect the tablet
- The app already installed on the tablet (built and deployed from Android Studio)

---

## Step 1 — Factory Reset the Tablet

> ⚠️ Device Owner can **only** be set on a tablet with **no existing accounts**.
> If the tablet already has a Google account logged in, you must factory reset first.

1. **Settings → General Management → Reset → Factory data reset**
2. Complete initial Android setup **but skip adding a Google account** (tap "Skip" when prompted)

---

## Step 2 — Enable Developer Options & USB Debugging

1. **Settings → About tablet → Software information**
2. Tap **Build number** 7 times until "Developer mode enabled" appears
3. Go back to **Settings → Developer options**
4. Enable **USB debugging**

---

## Step 3 — Install the App via Android Studio

1. Open the `Timer` project in Android Studio
2. Connect the tablet via USB — accept the "Allow USB debugging" prompt on the tablet
3. Select the tablet as the target device and click **▶ Run**
4. The app installs and launches — then **close it** (go back to home screen)

---

## Step 4 — Set Device Owner via ADB

On your Mac, open Terminal and run:

```bash
adb shell dpm set-device-owner com.wenonah.oralstimer/.AdminReceiver
```

Expected response:
```
Success: Device owner set to package com.wenonah.oralstimer
```

> If you get an error saying accounts exist, repeat Step 1 (factory reset).

---

## Step 5 — Reboot the Tablet

```bash
adb reboot
```

The tablet will boot and **automatically launch Wenonah's Orals Timer in kiosk mode**.

- The home button, recents button, and status bar are all disabled
- No other apps are accessible
- The timer is the only thing running

---

## How to Exit Kiosk Mode (if ever needed)

From within the app, kiosk mode can be exited programmatically by calling `stopLockTask()`.
If you ever need to remove Device Owner entirely:

```bash
adb shell dpm remove-active-admin com.wenonah.oralstimer/.AdminReceiver
```

Or go to: **Settings → General Management → Reset → Factory data reset**

---

## Summary

| Step | Action |
|---|---|
| 1 | Factory reset tablet, skip Google account |
| 2 | Enable Developer Options + USB Debugging |
| 3 | Install app via Android Studio |
| 4 | `adb shell dpm set-device-owner com.wenonah.oralstimer/.AdminReceiver` |
| 5 | `adb reboot` |
| ✅ | Tablet boots directly into Wenonah's Orals Timer, locked |
