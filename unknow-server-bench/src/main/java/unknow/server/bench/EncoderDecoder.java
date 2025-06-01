package unknow.server.bench;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;

import unknow.server.servlet.utils.Utf8Decoder;
import unknow.server.servlet.utils.Utf8Encoder;

public class EncoderDecoder {
	private static final String data = "Hello world!\nÇa va bien ?\nПривет, как дела?\n你好，世界\nこんにちは世界\n👋🌍✨🔥🚀\nLorem ipsum dolor sit amet, consectetur adipiscing elit.";

	@Benchmark
	public String utf8Encoder() {
		ByteBuffer b = ByteBuffer.allocate(4096);
		Utf8Encoder.encode(data, b);
		return new Utf8Decoder().append(b.array(), 0, b.position()).done();
	}

	@Benchmark
	public String charset() {
		ByteBuffer b = ByteBuffer.allocate(4096);
		CharsetEncoder e = StandardCharsets.UTF_8.newEncoder();
		e.encode(CharBuffer.wrap(data), b, true);

		b.flip();
		CharsetDecoder d = StandardCharsets.UTF_8.newDecoder();
		CharBuffer c = CharBuffer.allocate(b.limit());
		d.decode(b, c, true);
		return c.flip().toString();
	}
}
