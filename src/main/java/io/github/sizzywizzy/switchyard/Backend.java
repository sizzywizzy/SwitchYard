package io.github.sizzywizzy.switchyard;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A stand-in origin server. Answers every request with its own name, after an
 * optional ?delay=<ms>, and logs any request body it receives. Run as:
 * Backend <port> <name>
 */
public final class Backend {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        String name = args[1];

        ServerSocket server = new ServerSocket(port);
        System.out.println(name + " listening on " + port);

        while (true) {
            try (Socket client = server.accept()) {        // one at a time is fine for a test backend
                handle(client, name);
            } catch (IOException | InterruptedException e) {
                System.err.println(name + ": request failed: " + e);
            }
        }
    }

    static void handle(Socket client, String name) throws IOException, InterruptedException {
        InputStream in = new BufferedInputStream(client.getInputStream());

        byte[] head = Http.readHead(in);
        if (head == null) {
            return;                                         // connected, then hung up
        }
        List<String> lines = Http.lines(head);
        if (lines.isEmpty()) {
            return;
        }
        String requestLine = lines.get(0);                  // e.g. "POST / HTTP/1.1"

        // Read the body so it shows in the log. This is what makes a body-dropping
        // proxy visible: the backend waits for bytes that never come, and hangs.
        byte[] body = in.readNBytes(Http.contentLength(lines));
        System.out.println(name + ": " + requestLine
                + (body.length > 0 ? "  body: " + new String(body, StandardCharsets.ISO_8859_1) : ""));

        int delay = delayMillis(requestLine);
        if (delay > 0) {
            Thread.sleep(delay);                            // pretend to be a slow server
        }

        String text = "hello from " + name + "\n";
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Length: " + text.length() + "\r\n"
                + "Connection: close\r\n"
                + "\r\n"
                + text;
        OutputStream out = client.getOutputStream();
        out.write(response.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    /** The number after "delay=" in the request line, or 0 if there is none. */
    static int delayMillis(String requestLine) {
        int at = requestLine.indexOf("delay=");
        if (at < 0) {
            return 0;
        }
        int start = at + "delay=".length();
        int end = start;
        while (end < requestLine.length() && Character.isDigit(requestLine.charAt(end))) {
            end++;                                          // walk forward over the digits
        }
        return end == start ? 0 : Integer.parseInt(requestLine.substring(start, end));
    }
}
