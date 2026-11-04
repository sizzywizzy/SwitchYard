package io.github.sizzywizzy.switchyard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
                } catch (IOException e) {               // a Runnable can't throw it, so it's caught here
                    System.err.println("client " + client.getPort() + " failed: " + e);
                }
            }, "client-" + client.getPort());           // named, so jstack shows which thread is which

            worker.start();                             // start() — not run() — then straight back to accept()
        }
    }

    static void handle(Socket client) throws IOException {
        try (client) {                                  // closes the socket when this block ends, even on error

            // Headers are bytes; ISO-8859-1 turns each byte into exactly one char, nothing lost.
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));

            // Collect the request head instead of printing it: we have to send it on.
            // Stop at the BLANK line, not at end-of-stream — the client is holding the
            // connection open waiting for our reply, so end-of-stream never comes.
            List<String> head = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                head.add(line);
            }
            if (head.isEmpty()) {
                return;                                 // connected, then hung up
            }
            System.out.println(Thread.currentThread().getName() + ": " + head.get(0));

            // The other direction: now WE are the client, and the backend is the server.
            try (Socket backend = new Socket("localhost", 9001)) {

                // Rebuild the request line by line. The added Connection: close makes the
                // backend hang up after replying — so on THIS leg, end-of-stream is a real
                // signal, and transferTo below knows when to stop.
                StringBuilder request = new StringBuilder();
                for (String h : head) {
                    request.append(h).append("\r\n");
                }
                request.append("Connection: close\r\n");
                request.append("\r\n");

                OutputStream toBackend = backend.getOutputStream();
                toBackend.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
                toBackend.flush();

                // Copy the reply back to the client byte for byte — status line, headers,
                // body, whatever it is — until the backend closes its side.
                backend.getInputStream().transferTo(client.getOutputStream());
                client.getOutputStream().flush();

            } catch (ConnectException e) {
                // Nothing listening on 9001. Say so, instead of closing in silence.
                String body = "backend unavailable\n";
                String response = "HTTP/1.1 502 Bad Gateway\r\n"
                        + "Content-Length: " + body.length() + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + body;
                client.getOutputStream().write(response.getBytes(StandardCharsets.ISO_8859_1));
                client.getOutputStream().flush();
            }
        }
    }
}
