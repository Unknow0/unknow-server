package unknow.server.bench;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {
	public static final NullOutputStream INSTANCE = new NullOutputStream();

	private NullOutputStream() {
	}

	@Override
	public void write(int b) throws IOException { // OK
	}

	@Override
	public void write(byte[] b) throws IOException { // OK
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException { // OK
	}

}
