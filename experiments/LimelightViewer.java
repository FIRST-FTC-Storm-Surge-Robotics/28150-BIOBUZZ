import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Standalone desktop experiment; requires Java 17+, no FTC SDK or dependencies. */
public final class LimelightViewer {
    private static final String[] ROUTES = {"results", "status", "hwreport"};
    private static final int TIMEOUT_MS = 3000;
    private static final int MAX_FRAME_BYTES = 16 * 1024 * 1024;
    private final URI streamUrl;
    private final URI apiUrl;
    private final ScheduledExecutorService workers = Executors.newScheduledThreadPool(4, task -> {
        Thread thread = new Thread(task, "limelight-reader");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running = true;
    private volatile BufferedImage latestFrame;
    private volatile String streamStatus = "Connecting to camera...";

    private LimelightViewer(URI streamUrl, URI apiUrl) {
        this.streamUrl = streamUrl;
        this.apiUrl = apiUrl;
    }

    public static void main(String[] args) throws Exception {
        URI stream = URI.create("http://limelight.local:5800/");
        URI api = null;
        boolean console = false;
        boolean once = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--stream": stream = httpUri(args[++i]); break;
                    case "--api": api = httpUri(args[++i]); break;
                    case "--console": console = true; break;
                    case "--once": once = true; break;
                    case "--help": usage(); return;
                    default: throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            if (api == null) {
                api = new URI(stream.getScheme(), null, stream.getHost(), 5807, "/", null, null);
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            System.err.println("Invalid arguments: " + e.getMessage());
            usage();
            System.exit(2);
            return;
        }
        LimelightViewer viewer = new LimelightViewer(stream, api);
        if (once || console || GraphicsEnvironment.isHeadless()) {
            System.out.println("Text mode: displaying full JSON responses; video requires desktop mode.");
            boolean ok;
            do {
                ok = viewer.printData();
                if (!once) Thread.sleep(500);
            } while (!once);
            if (!ok) System.exit(1);
        } else {
            SwingUtilities.invokeLater(viewer::showWindow);
        }
    }

    private static URI httpUri(String value) {
        URI uri = URI.create(value);
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Expected an http(s) URL: " + value);
        }
        return uri;
    }

    private static void usage() {
        System.out.println("Usage: java experiments/LimelightViewer.java [--console | --once]\n"
                + "       [--stream http://limelight.local:5800/] [--api http://limelight.local:5807/]\n"
                + "Desktop: live video + all results/status/hwreport fields.\n"
                + "--console: poll JSON until Ctrl+C. --once: print JSON once and exit.");
    }

    private boolean printData() {
        boolean ok = true;
        for (String route : ROUTES) {
            System.out.println("\n=== " + apiUrl.resolve("/" + route) + " @ " + Instant.now() + " ===");
            try {
                System.out.println(readJson(route));
            } catch (IOException e) {
                ok = false;
                System.err.println("ERROR: " + describe(e));
            }
        }
        return ok;
    }

    private void showWindow() {
        JFrame window = new JFrame("Limelight Viewer — " + streamUrl.getHost());
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel camera = new JPanel(new BorderLayout());
        VideoPanel video = new VideoPanel();
        JLabel connection = new JLabel(streamStatus);
        camera.add(new JLabel("Live video: " + streamUrl), BorderLayout.NORTH);
        camera.add(video, BorderLayout.CENTER);
        camera.add(connection, BorderLayout.SOUTH);
        JTabbedPane tabs = new JTabbedPane();
        for (String route : ROUTES) {
            JTextArea text = new JTextArea("Waiting for " + route + "...");
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            text.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            JLabel updated = new JLabel("Connecting...");
            JPanel data = new JPanel(new BorderLayout());
            data.add(new JLabel(apiUrl.resolve("/" + route).toString()), BorderLayout.NORTH);
            data.add(new JScrollPane(text), BorderLayout.CENTER);
            data.add(updated, BorderLayout.SOUTH);
            tabs.addTab(route, data);
            String[] lastUpdate = {"never"};
            // Separate workers allow video and available endpoints to continue if one fails.
            workers.scheduleWithFixedDelay(() -> {
                try {
                    String body = readJson(route);
                    String timestamp = Instant.now().toString();
                    lastUpdate[0] = timestamp;
                    SwingUtilities.invokeLater(() -> {
                        int caret = text.getCaretPosition();
                        text.setText(body);
                        text.setCaretPosition(Math.min(caret, body.length()));
                        updated.setText("Updated " + timestamp);
                    });
                } catch (IOException e) {
                    String message = "Unavailable: " + describe(e)
                            + " — retrying; last update: " + lastUpdate[0];
                    SwingUtilities.invokeLater(() -> updated.setText(message));
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, camera, tabs);
        split.setResizeWeight(0.55);
        window.add(split);
        window.setSize(1250, 800);
        window.setLocationByPlatform(true);
        Timer refresh = new Timer(50, event -> {
            connection.setText(streamStatus);
            video.repaint();
        });
        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) {
                running = false;
                refresh.stop();
                workers.shutdownNow();
            }
        });
        window.setVisible(true);
        refresh.start();
        workers.execute(this::readVideo);
    }

    private String readJson(String route) throws IOException {
        HttpURLConnection connection = open(apiUrl.resolve("/" + route));
        connection.setRequestProperty("Accept", "application/json");
        try {
            checkStatus(connection);
            try (InputStream input = connection.getInputStream()) {
                String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                String trimmed = body.stripLeading();
                if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
                    throw new IOException("Expected JSON, received " + connection.getContentType()
                            + ". Check the API port (normally 5807).");
                }
                return prettyJson(body);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void readVideo() {
        while (running) {
            HttpURLConnection connection = null;
            try {
                connection = open(streamUrl);
                checkStatus(connection);
                String type = connection.getContentType();
                if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("multipart/x-mixed-replace")) {
                    throw new IOException("Expected an MJPEG stream; received " + type);
                }
                long frames = 0;
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    while (running) {
                        BufferedImage image = readFrame(input);
                        latestFrame = image;
                        streamStatus = "Live — " + image.getWidth() + " × " + image.getHeight()
                                + " — frames: " + (++frames) + " — " + Instant.now();
                    }
                }
            } catch (IOException e) {
                latestFrame = null;
                streamStatus = "Video unavailable: " + describe(e) + " — retrying";
            } finally {
                if (connection != null) connection.disconnect();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Read the MIME headers and exact JPEG length; never decode a never-ending HTTP body. */
    static BufferedImage readFrame(InputStream input) throws IOException {
        int length = -1;
        boolean hasHeaders = false;
        String line;
        while ((line = readLine(input)) != null) {
            if (line.isEmpty()) {
                if (hasHeaders) break;
                continue;
            }
            if (line.startsWith("--")) continue;
            hasHeaders = true;
            if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                try {
                    length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid MJPEG Content-Length", e);
                }
            }
        }
        if (line == null) throw new EOFException("Camera stream ended");
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Missing or invalid MJPEG frame length: " + length);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Incomplete camera frame");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IOException("Camera frame is not a supported image");
        return image;
    }

    private static String readLine(InputStream input) throws IOException {
        StringBuilder line = new StringBuilder();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') return line.toString();
            if (value != '\r') line.append((char) value);
            if (line.length() > 8192) throw new IOException("MJPEG header line too long");
        }
        return line.length() == 0 ? null : line.toString();
    }

    private static HttpURLConnection open(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setUseCaches(false);
        return connection;
    }

    private static void checkStatus(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status != 200) throw new IOException("HTTP " + status + " from " + connection.getURL());
    }

    private static String describe(IOException error) {
        return error.getClass().getSimpleName() + ": " + error.getMessage();
    }

    /** Only changes whitespace outside strings, so every field and numeric value is preserved. */
    static String prettyJson(String json) {
        StringBuilder output = new StringBuilder();
        int indent = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                output.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
                continue;
            }
            switch (c) {
                case '"': quoted = true; output.append(c); break;
                case '{': case '[':
                    int next = i + 1;
                    while (next < json.length() && Character.isWhitespace(json.charAt(next))) next++;
                    char closing = c == '{' ? '}' : ']';
                    if (next < json.length() && json.charAt(next) == closing) {
                        output.append(c).append(closing);
                        i = next;
                        break;
                    }
                    output.append(c).append('\n').append("  ".repeat(++indent)); break;
                case '}': case ']':
                    indent = Math.max(0, indent - 1);
                    output.append('\n').append("  ".repeat(indent)).append(c); break;
                case ',': output.append(c).append('\n').append("  ".repeat(indent)); break;
                case ':': output.append(": "); break;
                default: if (!Character.isWhitespace(c)) output.append(c);
            }
        }
        return output.toString();
    }

    private final class VideoPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        VideoPanel() {
            setPreferredSize(new Dimension(640, 480));
            setMinimumSize(new Dimension(200, 150));
            setBackground(Color.BLACK);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage image = latestFrame;
            if (image == null) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("Waiting for live video...", 20, 30);
                return;
            }
            double scale = Math.min((double) getWidth() / image.getWidth(),
                    (double) getHeight() / image.getHeight());
            int width = (int) (image.getWidth() * scale);
            int height = (int) (image.getHeight() * scale);
            graphics.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2,
                    width, height, null);
        }
    }
}
