# Limelight viewer experiment

A standalone Java 17+ desktop program showing the live camera feed and **every
field returned by `/results`, `/status`, and `/hwreport`**. No extra libraries,
Gradle build, Android SDK, or robot deployment are needed.

From the repository root:

```sh
java experiments/LimelightViewer.java
```

The left side displays live video from `http://limelight.local:5800/`.
The right side has scrollable, selectable JSON tabs for targeting results,
device status, and hardware reports, refreshed approximately twice per second.
All nested objects, arrays, unknown fields, and numeric values are retained.
The viewer uses read-only GET requests and reconnects automatically after errors.
Close the window to stop it. Last successful JSON stays visible on an error,
with a warning and its previous update timestamp; video clears on disconnect.

Port 5800 is an MJPEG video stream, so it does not contain targeting telemetry.
The [Limelight REST API](https://docs.limelightvision.io/docs/docs-limelight/apis/rest-http-api)
serves those JSON endpoints on port 5807. Both ports must be reachable from your
computer. The viewer displays the current output of the configured pipeline;
it does not change pipelines or enumerate saved configuration/resources.

For terminal output (Ctrl+C to stop):

```sh
java experiments/LimelightViewer.java --console
```

For one complete set of JSON responses, then exit (status 1 if any endpoint fails):

```sh
java experiments/LimelightViewer.java --once
```

Text modes omit video. A computer without a graphical display defaults to text mode.
To use an IP address or another hostname:

```sh
java experiments/LimelightViewer.java --stream http://192.168.1.50:5800/
```

The API defaults to the stream's hostname on port 5807. Override it if needed:

```sh
java experiments/LimelightViewer.java --stream http://localhost:5800/ --api http://localhost:5807/
```

If `limelight.local` cannot be resolved, use the camera's IP address and confirm
your computer is connected to the same network as the camera.

Compile check (optional; output is Git-ignored):

```sh
javac -Xlint:all -d experiments/build experiments/LimelightViewer.java
```
