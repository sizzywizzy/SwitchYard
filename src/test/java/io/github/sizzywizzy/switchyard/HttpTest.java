package io.github.sizzywizzy.switchyard;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pure parsing, no sockets: feed bytes in, check what comes out. These run in
 * milliseconds and pin down the one behaviour step 4 exists for — the body
 * must still be in the stream after the head has been read.
 */
class HttpTest {

    @Test
    void readHeadStopsAtTheBlankLineAndLeavesTheBodyInTheStream() throws IOException {
        InputStream in = new ByteArrayInputStream(
                "POST / HTTP/1.1\r\nContent-Length: 3\r\n\r\nabc".getBytes(ISO_8859_1));

        byte[] head = Http.readHead(in);

        assertEquals("POST / HTTP/1.1\r\nContent-Length: 3\r\n\r\n", new String(head, ISO_8859_1));
        assertEquals("abc", new String(in.readAllBytes(), ISO_8859_1));   // untouched, still there
    }

    @Test
    void readHeadReturnsNullWhenTheStreamEndsBeforeTheBlankLine() throws IOException {
        InputStream in = new ByteArrayInputStream("GET / HTTP/1.1\r\nHost: x\r\n".getBytes(ISO_8859_1));

        assertNull(Http.readHead(in));
    }

    @Test
    void linesSplitsTheHeadAndDropsTheBlankLine() {
        byte[] head = "GET /?delay=5 HTTP/1.1\r\nHost: x\r\n\r\n".getBytes(ISO_8859_1);

        assertEquals(List.of("GET /?delay=5 HTTP/1.1", "Host: x"), Http.lines(head));
    }

    @Test
    void contentLengthIsCaseInsensitiveAndDefaultsToZero() {
        assertEquals(3, Http.contentLength(List.of("POST / HTTP/1.1", "content-length: 3")));
        assertEquals(12, Http.contentLength(List.of("POST / HTTP/1.1", "Content-Length:   12  ")));
        assertEquals(0, Http.contentLength(List.of("GET / HTTP/1.1", "Host: x")));
    }
}
