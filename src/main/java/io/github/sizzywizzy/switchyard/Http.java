package io.github.sizzywizzy.switchyard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** The few pieces of HTTP/1.1 parsing that both the proxy and the test backend need. */
final class Http {

    private Http() {
    }

    /**
     * Reads the request head — request line, headers, and the blank line that ends
     * them — as raw bytes. Reads one byte at a time and stops EXACTLY at the blank
     * line, so whatever follows (the body) is still in the stream for the next reader.
     *
     * @return the head including its final {@code \r\n\r\n}, or {@code null} if the
     *         stream ended first (the client hung up)
     */
    static byte[] readHead(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int last4 = 0;                                  // the last four bytes seen, packed into one int
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            last4 = (last4 << 8) | b;                   // slide the window along by one byte
            if (last4 == 0x0D0A0D0A) {                  // \r \n \r \n — end of the head
                return buf.toByteArray();
            }
        }
        return null;
    }

    /** The head split into lines: request line first, then one per header. No blank line. */
    static List<String> lines(byte[] head) {
        // ISO-8859-1 maps every byte to exactly one char, so no byte can be mangled.
        return List.of(new String(head, StandardCharsets.ISO_8859_1).split("\r\n"));
    }

    /** The Content-Length value, or 0 when the header is absent. Header names are case-insensitive. */
    static int contentLength(List<String> headLines) {
        for (String line : headLines) {
            if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                return Integer.parseInt(line.substring("content-length:".length()).trim());
            }
        }
        return 0;
    }
}
