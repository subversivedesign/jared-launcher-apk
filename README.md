# Jim's Launcher APK

A locked-down Android app that wraps the YouTube launcher in a kiosk shell. Designed for Jim's Kindle Fire.

## What it does

- Shows the launcher at https://steady-bunny-526205.netlify.app/
- Locks the screen so Jim can't escape (no home, back, recents, notifications)
- Caps volume at 85%
- Auto-launches on device boot
- Auto-resumes when screen wakes from power button
- Set as the HOME app so home button doesn't go anywhere
- 5-second hold in top-right corner + PIN 8228 = exit kiosk for parent access

## How to build the APK (no Android Studio required)

### Step 1: Create a GitHub account
Go to github.com and sign up if you don't have one. Free.

### Step 2: Create a new repository
1. Click the **+** in top right → **New repository**
2. Name it `jim-launcher` (or whatever you like)
3. Make it **Private** if you prefer
4. Click **Create repository**

### Step 3: Upload these files
1. On the new empty repo page, click **uploading an existing file**
2. Drag this entire folder's contents into the upload area
3. Scroll down and click **Commit changes**

### Step 4: Wait for build (about 5 minutes)
1. Click the **Actions** tab at the top of your repo
2. You'll see a build running ("Build APK")
3. Wait for the green tick

### Step 5: Download the APK
1. Click on the completed workflow run
2. Scroll down to **Artifacts**
3. Click **jim-launcher-apk** to download a zip
4. Extract the zip - inside is `app-debug.apk`

### Step 6: Sideload onto Kindle Fire
1. On the Fire tablet: Settings → Security & Privacy → enable **Apps from Unknown Sources** (for whatever method you'll use - email, USB, browser)
2. Transfer the APK to the tablet (email it to yourself, USB cable, etc.)
3. Open the APK file on the tablet via Files
4. Tap Install
5. The app appears as "Jim's Videos"

### Step 7: Make it the default launcher
1. Tap the home button - Android will ask which launcher to use
2. Pick **Jim's Videos** and select **Always**
3. Done - the app is now the home screen

### Step 8: Enable Screen Pinning (one-time setting)
1. Settings → Security & Privacy → **Screen Pinning** → toggle ON
2. Disable "Ask for unlock pattern before unpinning"
3. This lets the app pin itself in Lock Task Mode

### Step 9: Set lock screen to None
1. Settings → Security & Privacy → **Lock Screen Password** → **None**
2. So power button resume goes directly to the app

## Updating the launcher content

The APK only points at the Netlify URL. To update what videos Jim sees, just update your Netlify deployment - no APK rebuild needed. The APK auto-loads the latest content.

## Updating the APK itself

If you need to change behaviour (PIN, volume cap, URL, etc):
1. Edit `MainActivity.kt` 
2. Commit changes to GitHub
3. New APK builds automatically
4. Download and reinstall

## Carer exit

Hold finger in top-right corner of the screen for 5 seconds → PIN dialog appears → enter `8228` → kiosk exits, you're at the home screen.
