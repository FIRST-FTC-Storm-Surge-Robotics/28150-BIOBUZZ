# Pedro Pathing

Installed using the [official manual installation guide](https://pedropathing.com/docs/pathing/installation):

- `com.pedropathing:revhub:3.0.2` (includes `core:3.0.2`)
- `com.pedropathing:tuning:1.0.1` (AutoTune)
- Dairy Maven repository for the tuning scanner dependency

FTC SDK 12.0.0 is retained. Sync Gradle in Android Studio to download the libraries.
Build from the repository root with JDK 17 and an installed Android SDK:

```sh
./gradlew :TeamCode:assembleDebug
```

The `src/main/java/org/firstinspires/ftc/teamcode/pedro` package comes from
[Pedro's official Quickstart](https://github.com/Pedro-Pathing/Quickstart/tree/9dc89787898fab6b5cd9095df38489a14bf51a95/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro),
revision `9dc89787898fab6b5cd9095df38489a14bf51a95`. The nine `procedures` files
are copied with only final-newline normalization; their upstream FIRST license is retained in the repository's
root `LICENSE`. `Tuning.java` registers the mecanum and supported localizer setup
procedures. `Constants.create()` throws an explanatory exception until the robot
has been configured, instead of returning the quickstart's placeholder `null`.

## Configure the robot

Installation provides the library and tuning tools. Robot-specific constants still
need to be measured before following paths. Follow the [tuning guide](https://pedropathing.com/docs/pathing/tuning)
in order:

1. Build and install TeamCode on the Robot Controller. Connect to its network and
   open AutoTune at `http://192.168.43.1:10158` (or the controller's IP on port 10158).
2. Run the Mecanum procedure. The existing prototype uses `frontLeftDrive`,
   `frontRightDrive`, `backLeftDrive`, and `backRightDrive`. Enter those names and
   verify wheel directions with the tuner. Paste its generated configuration into
   `pedro/Constants.java`.
3. Run the localizer procedure matching the robot's hardware: Pinpoint, OTOS,
   OctoQuad, two-wheel, three-wheel, or three-wheel plus IMU. Paste the generated
   configuration into `Constants.java`.
4. Register `ForesightTuner` in `Tuning.java` using the configured localizer and
   drivetrain factories, then run it and save the generated algorithm settings.
5. Implement `Constants.create(HardwareMap)` with the tuned localizer, drivetrain,
   and Foresight algorithm. Register the included `Tests` procedure with those same
   factories to validate localization and path following. See the official
   [constants guide](https://pedropathing.com/docs/pathing/tuning/constants) and
   [test guide](https://pedropathing.com/docs/pathing/tuning/test).

Foresight and the combined tests depend on measured settings and are intentionally
not registered yet. No localizer type, odometry offsets, or follower gains have
been assumed. The existing prototype TeleOp continues to use its own drive code.
