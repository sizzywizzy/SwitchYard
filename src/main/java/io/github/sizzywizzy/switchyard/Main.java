package io.github.sizzywizzy.switchyard;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);   // claim port 8080 from the OS
        System.out.println("listening on 8080");

        while (true) {
            Socket client = server.accept();            // wait here until someone connects

            Thread worker = new Thread(() -> {          // a thread whose only job is this one client
                try {
                    handle(client);
                } catch (IOException e) {               // a Runnable cannot throw it, so it is caught here
                    System.err.println("client " + client.getPort() + " failed: " + e);
                }
            }, "client-" + client.getPort());           // named, so jstack shows which thread is which

            worker.start();                             // start() — not run() — then straight back to accept()
        }
    }

    static void handle(Socket client) throws IOException {
        try (client) {                                  // closes the socket when this block ends, even on error

            // Bytes, not characters — and ONE stream object for the whole request. This
            // buffer reads ahead too, but everything it reads ahead is handed out, in
            // order, by this same object. The BufferedReader we had before kept its
            // read-ahead private, which is how request bodies were being lost.
            InputStream in = new BufferedInputStream(client.getInputStream());

            byte[] head = Http.readHead(in);
            if (head == null) {
                return;                                 // connected, then hung up
            }
            List<String> lines = Http.lines(head);
            if (lines.isEmpty()) {
                return;                                 // a bare blank line: nothing to forward
            }
            System.out.println(Thread.currentThread().getName() + ": " + lines.get(0));

            // The body is exactly Content-Length bytes, and they come next on the same
            // stream. readNBytes blocks until all of them have arrived. GET has none: 0.
            byte[] body = in.readNBytes(Http.contentLength(lines));

            // The other direction: now WE are the client, and the backend is the server.
            try (Socket backend = new Socket("localhost", 9001)) {

                // Rebuild the head line by line. The added Connection: close makes the
                // backend hang up after replying — so on THIS leg, end-of-stream is a real
                // signal, and transferTo below knows when to stop.
                StringBuilder request = new StringBuilder();
                for (String line : lines) {
                    request.append(line).append("\r\n");
                }
                request.append("Connection: close\r\n");
                request.append("\r\n");

                OutputStream toBackend = backend.getOutputStream();
                toBackend.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
                toBackend.write(body);                  // the body follows the head, byte for byte
                toBackend.flush();

                // Copy the reply back to the client — status line, headers, body, whatever
                // it is — until the backend closes its side.
                backend.getInputStream().transferTo(client.getOutputStream());
                client.getOutputStream().flush();

            } catch (ConnectException e) {
                // Nothing listening on 9001. Say so, instead of closing in silence.
                String text = "backend unavailable\n";
                String response = "HTTP/1.1 502 Bad Gateway\r\n"
                        + "Content-Length: " + text.length() + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + text;
                client.getOutputStream().write(response.getBytes(StandardCharsets.ISO_8859_1));
                client.getOutputStream().flush();
            }
        }
    }
}
