# Game Clock

**Game Clock** is a **Wear OS** app for American football-style quarters: a **countdown timer** for each quarter, tuned for sideline or referee use on the watch.

## What it does

- **Quarter length** — You choose how long a quarter runs (minutes and seconds). The value is **saved on the device** and used as the default the next time you open the app.
- **Countdown** — Tap the large time on the home screen to **start** the quarter. Time counts **down** in `MM:SS`.
- **Pause and resume** — With a quarter running, **tap the big countdown** to pause or resume (there are no separate pause buttons on that screen).
- **Wall clock** — While a quarter is active, the **top of the screen** shows the **current time of day** (`HH:mm`), so you still see real time at a glance.
- **Haptics** — In the **last 10 seconds**, the watch **vibrates** each second; the last few seconds use a stronger double-pulse pattern.
- **Adjust remaining time** — When paused, open **adjust time** (clock icon). Change minutes and seconds with **+ / −**; changes apply **immediately**. The **“Before change”** line stays fixed so you can compare. **Done** (checkmark) returns to the countdown; **reset** (↺) ends the quarter after a **confirmation** dialog.
- **Set quarter length** — On the home screen, the **sliders icon** opens the editor (same compact layout as adjust time). **Chevron** goes back without saving; **checkmark** saves and closes.

The UI uses a **dark theme** with purple accents, similar in spirit to a companion “play clock” style Wear app.

## Requirements

- **Wear OS** device (watch form factor)
- **Android API 30+** on the watch (`minSdk 30`)

## Building

1. Open the project in **Android Studio** (Giraffe or newer recommended).
2. Let Gradle sync; generate or add a **Gradle wrapper** if prompted.
3. Run **Run > Run ‘app’** with a **Wear OS** emulator or a **paired physical watch** with developer options / debugging enabled.

Command-line build (with `gradlew` present):

```text
./gradlew :app:assembleDebug
```

## Tech stack

- **Kotlin**, **Jetpack Compose** for UI  
- **Wear Compose Material 3**  
- **SharedPreferences** for the saved default quarter duration  
- **Java `java.time`** for the wall-clock header  

## Package

Application ID / namespace: **`com.gameclock.referee`**.

---

This project is for personal or team sideline timing; it is not affiliated with any league or official timekeeping system.
