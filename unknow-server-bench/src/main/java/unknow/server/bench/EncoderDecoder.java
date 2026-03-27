package unknow.server.bench;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;

import unknow.server.servlet.utils.Utf8Decoder;
import unknow.server.servlet.utils.Utf8Encoder;
import unknow.server.util.Decoder;
import unknow.server.util.Encoder;

public class EncoderDecoder {
	private static final String DATA = "Hello world!\nÇa va bien ?\nПривет, как дела?\n你好，世界\nこんにちは世界\n👋🌍✨🔥🚀\nLorem ipsum dolor sit amet, consectetur adipiscing elit.";

	@Benchmark
	public String utf8EncoderServlet() {
		ByteBuffer b = ByteBuffer.allocate(4096);
		Utf8Encoder.encode(DATA, b);
		return new Utf8Decoder().append(b.array(), 0, b.position()).done();
	}

	@Benchmark
	public String utf8Encoder() {
		ByteBuffer b = ByteBuffer.allocate(4096);
		Encoder.from(StandardCharsets.UTF_8).encode(CharBuffer.wrap(DATA), b, true);

		CharBuffer c = CharBuffer.allocate(4096);
		Decoder.from(StandardCharsets.UTF_8).decode(b.flip(), c, true);
		return c.flip().toString();
	}

	@Benchmark
	public String charset() {
		ByteBuffer b = ByteBuffer.allocate(4096);
		CharsetEncoder e = StandardCharsets.UTF_8.newEncoder();
		e.encode(CharBuffer.wrap(DATA), b, true);

		b.flip();
		CharsetDecoder d = StandardCharsets.UTF_8.newDecoder();
		CharBuffer c = CharBuffer.allocate(4096);
		d.decode(b, c, true);
		return c.flip().toString();
	}

	@Benchmark
	public String string() {
		byte[] bytes = DATA.getBytes(StandardCharsets.UTF_8);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
