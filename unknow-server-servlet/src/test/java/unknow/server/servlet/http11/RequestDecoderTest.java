package unknow.server.servlet.http11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.impl.ServletRequestImpl;

public class RequestDecoderTest {
	public static Stream<Arguments> test() {
		return Stream.of( //
				Arguments.of("GET / HTTP/1.1", new byte[][] { "GET / HTTP/1.1\r\n\r\n".getBytes() }), //
				Arguments.of("GET / HTTP/1.1", new byte[][] { "GET / HTTP/1.1\r".getBytes(), "\n\r\n".getBytes() }), //
				Arguments.of("GET / HTTP/1.1\nk: [v]", new byte[][] { "GET / HTTP/1.1\r\nK  :  v  \r\n\r\n".getBytes() }), //
				Arguments.of("GET / HTTP/1.1\nk: [v\rv]", new byte[][] { "GET / HTTP/1.1\r\nK  :  v\r".getBytes(), "v\r\n\r\n".getBytes() }) //
		);
	}

	@ParameterizedTest
	@MethodSource
	public void test(String expected, byte[]... chunk) {
		Http11Processor co = mock(Http11Processor.class, withSettings().useConstructor((HttpConnection) null));
		RequestDecoder d = new RequestDecoder(co);
		ServletRequestImpl req = null;
		for (byte[] b : chunk)
			req = d.append(ByteBuffer.wrap(b));

		assertNotNull(req);
		assertEquals(expected, req.toString());
	}
}
