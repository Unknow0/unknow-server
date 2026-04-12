package unknow.server.util;

public class Utf8DecoderTest extends DecoderTest {
	@Override
	protected unknow.server.util.Decoder decoder() {
		return new Utf8Decoder();
	}
}
