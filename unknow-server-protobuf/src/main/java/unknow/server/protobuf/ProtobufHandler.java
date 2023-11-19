package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Parser;

import unknow.server.nio.NIOConnection;
import unknow.server.nio.Handler;

/**
 * @author unknow
 */
public abstract class ProtobufHandler<T> implements Handler {
	private final Parser<T> parser;
	protected final NIOConnection co;
	private final LimitedInputStream limited;

	@SuppressWarnings("resource")
	protected ProtobufHandler(Parser<T> parser, NIOConnection co) {
		this.parser = parser;
		this.co = co;
		this.limited = new LimitedInputStream(co.getIn());
	}

	@SuppressWarnings("resource")
	@Override
	public void onRead() {
		InputStream in = co.getIn();
		try {
			in.mark(4096);
			int len = readInt(in);
			if (len < 0 || len < in.available())
				return;
			limited.setLimit(len);
			process(parser.parseFrom(limited));
		} catch (@SuppressWarnings("unused") IOException e) {
			co.getOut().close();
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

	@Override
	public void onWrite() { // OK
	}

	@Override
	public boolean closed(long now, boolean close) {
		return close;
	}

	@Override
	public void free() { // OK
	}
}