package io.github.sizzywizzy.switchyard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class Main {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);   // claim port 8080 from the OS
        System.out.println("listening on 8080");

        while (true) {
            Socket client = server.accept();            // wait here until someone connects
            Thread worker=new Thread(() -> {
                try {
                    handle(client);
                } catch (IOException e) {
                    System.err.println("client "+ client.getPort()+ " failed: "+e);
                }
            }, "client-"+client.getPort());
            worker.start(); 
        }
    }

    static void handle(Socket client) throws IOException {
        try (client) {                                  // closes the socket when this block ends, even on error

            // Headers are bytes; ISO-8859-1 turns each byte into exactly one char, nothing lost.
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));

            // Read the request head line by line and stop at the BLANK line.
            // Not at end-of-stream: the client is holding the connection open
            // waiting for our reply, so end-of-stream never comes.
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
            }

            String body = "hello from switchyard\n";
            String response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Length: " + body.length() + "\r\n"   // how many body bytes to expect
                    + "Connection: close\r\n"                        // we hang up after this
                    + "\r\n"                                         // blank line ends the headers
                    + body;

            OutputStream out = client.getOutputStream();
            out.write(response.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }
    }
}
