package io.github.sizzywizzy.switchyard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A stand-in origin server. Answers every request with its own name, after an
 * optional ?delay=<ms>. Run as: Backend <port> <name>
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
        BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));

        String requestLine = in.readLine();                 // e.g. "GET /?delay=2000 HTTP/1.1"
        if (requestLine == null) {
            return;                                         // connected, then hung up
        }
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // read past the headers; this backend doesn't care what they say
        }

        System.out.println(name + ": " + requestLine);      // the "access log"

        int delay = delayMillis(requestLine);
        if (delay > 0) {
            Thread.sleep(delay);                            // pretend to be a slow server
        }

        String body = "hello from " + name + "\n";
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Length: " + body.length() + "\r\n"
                + "Connection: close\r\n"
                + "\r\n"
                + body;
        OutputStream out = client.getOutputStream();
        out.write(response.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    /** The number after "delay=" in the request line, or 0 if there isn't one. */
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
