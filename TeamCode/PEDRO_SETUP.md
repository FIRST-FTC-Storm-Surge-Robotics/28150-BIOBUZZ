# Pedro Pathing setup

Pedro Pathing helps the robot follow paths during autonomous. The library and its
tuning tools are installed, but we still need to set them up for our robot.
Tuning means measuring how the robot moves and using those measurements to adjust
its settings.

## What's installed

- Pedro Pathing `3.0.2`: the path-following library. The Gradle dependency is
  `com.pedropathing:revhub:3.0.2`, which also includes `core:3.0.2`.
- AutoTune `1.0.1`: a tool with a web page that walks you through tuning. Its
  dependency is `com.pedropathing:tuning:1.0.1`.
- The Dairy Maven repository: a download source for a library AutoTune uses to
  find our tuning methods.

The project still uses FTC SDK `12.0.0`. The installation follows the
[official Pedro guide](https://pedropathing.com/docs/pathing/installation).

## Open and build the project

Open the project in Android Studio and sync Gradle to download the libraries.
To build from a terminal, you need JDK 17 and an installed Android SDK. Run this
command from the main project folder:

```sh
./gradlew :TeamCode:assembleDebug
```

This command builds the app. Install it on the Robot Controller through Android
Studio before using AutoTune.

## Where the Pedro code lives

The files are in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/`:

- `Constants.java` is where we will save our robot's settings and create its
  path follower. Calling `Constants.create()` currently gives a clear error
  because those settings are not ready yet.
- `Tuning.java` lists the setup tools that AutoTune can show. It includes mecanum
  drive setup and the supported ways to track the robot's position.
- `procedures/` contains the tuning and test tools from Pedro's quickstart.

## Tune the robot

Follow the [official tuning guide](https://pedropathing.com/docs/pathing/tuning)
in this order:

1. Connect your computer to the Robot Controller's network. After installing the
   app, open `http://192.168.43.1:10158` in your browser. If your controller has a
   different IP address, use that address with port `10158`.
2. Run the Mecanum tool in AutoTune. Enter the four motor names used by our
   prototype: `frontLeftDrive`, `frontRightDrive`, `backLeftDrive`, and
   `backRightDrive`. Use the tool to check which way each wheel spins. Copy the
   settings it creates into `pedro/Constants.java`.
3. Run the tool that matches our position-tracking hardware: Pinpoint, OTOS,
   OctoQuad, two-wheel, three-wheel, or three-wheel plus IMU. Pedro calls the code
   that tracks position a **localizer**. Copy the settings from that tool into
   `Constants.java` too.
4. Add `ForesightTuner` to `Tuning.java`. Give it methods that create our drive
   system and localizer using the settings from the earlier steps. Foresight is
   the part of Pedro that controls how the robot follows a path. Run its tuner
   and save the settings it creates.
5. Fill in `Constants.create(HardwareMap)` so it creates a follower using our
   tuned localizer, drive system, and Foresight settings. Add the included `Tests`
   tool to `Tuning.java` using those same settings. Use it to check position
   tracking and path following. The official
   [constants guide](https://pedropathing.com/docs/pathing/tuning/constants) and
   [test guide](https://pedropathing.com/docs/pathing/tuning/test) show how to wire
   these pieces together.

Foresight and the combined tests will appear in AutoTune after we add them to
`Tuning.java`. They need our measured settings first. We still need to choose the
localizer and measure its sensor positions and the path-following settings.
The existing prototype TeleOp keeps using its own driving code.

## Source of the tuning files

The Pedro package came from the
[official Quickstart](https://github.com/Pedro-Pathing/Quickstart/tree/9dc89787898fab6b5cd9095df38489a14bf51a95/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro),
at commit `9dc89787898fab6b5cd9095df38489a14bf51a95`. The nine files in `procedures/`
match that version except for blank lines at the ends of files. Their FIRST
license is kept in the project's main `LICENSE` file. We changed `Tuning.java`
to list the setup tools and changed `Constants.create()` to report the missing
setup instead of returning `null`.
