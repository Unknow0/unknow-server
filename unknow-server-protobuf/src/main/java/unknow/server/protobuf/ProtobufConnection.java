package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Parser;

import unknow.server.nio.NIOConnection;

/**
 * @author unknow
 */
public abstract class ProtobufConnection<T> extends NIOConnection {
	private final Parser<T> parser;
	private final LimitedInputStream limited;

	@SuppressWarnings("resource")
	protected ProtobufConnection(Parser<T> parser) {
		this.parser = parser;
		this.limited = new LimitedInputStream(getIn());
	}

	@SuppressWarnings("resource")
	@Override
	public final void onRead() {
		InputStream in = getIn();
		try {
			in.mark(4096);
			int len = readInt(in);
			if (len < 0 || len < in.available())
				return;
			limited.setLimit(len);
			process(parser.parseFrom(limited));
		} catch (@SuppressWarnings("unused") IOException e) {
			getOut().close();
		} finally {
			try {
				in.reset();
			} catch (@SuppressWarnings("unused") IOException e) { // OK
			}
		}
	}

	protected abstract void process(T t) throws IOException;

	private final int readInt(InputStream in) throws IOException {
		int read = in.read();
		if (read == -1)
			return -1;
		return CodedInputStream.readRawVarint32(read, in);
	}
}