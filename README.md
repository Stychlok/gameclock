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

## Google Play Store

### Short description
Referee game clock for your wrist — track quarters hands-free on Wear OS.

### Long description
Game Clock is a Wear OS app built for referees and sideline staff who need a reliable, wrist-mounted quarter timer during American football games and similar timed sports.

**Quarter countdown**
Set your quarter length once — the app remembers it. Tap the large time display to start the countdown. Tap again to pause or resume. No fumbling with your phone on the sideline.

**Adjust time on the fly**
Made a mistake? Paused the clock and need to correct the remaining time? Open the adjust screen and nudge minutes and seconds up or down with large, glove-friendly buttons. The original value stays visible so you always know what changed.

**Haptic alerts**
The watch vibrates each second during the final 10 seconds, and switches to a stronger double-pulse in the last few seconds — so you feel the quarter end even in a noisy environment.

**Real time at a glance**
While a quarter is running, the current time of day is shown at the top of the screen. You never lose track of the actual clock while timing the game.

**Always-on display**
The screen stays awake for the full duration of a quarter, so the time is always visible without a wrist flick.

**Clean, minimal design**
Dark theme with high-contrast purple accents, built for round Wear OS watch faces. Large tap targets work reliably even with gloves.

---

This project is for personal or team sideline timing; it is not affiliated with any league or official timekeeping system.
