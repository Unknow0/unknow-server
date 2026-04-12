package unknow.server.util;

public class Utf8EncoderTest extends EncoderTest {

	@Override
	protected unknow.server.util.Encoder encoder() {
		return new Utf8Encoder();
	}

}
