package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Parser;

import unknow.server.nio.Connection;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;

/**
 * @author unknow
 */
public abstract class ProtobufHandlerFactory<T> implements HandlerFactory {
	private final Parser<T> parser;

	public ProtobufHandlerFactory(Parser<T> parser) {
		this.parser = parser;
	}

	@Override
	public Handler create(Connection co) {
		return new ProtobufHandler(co);
	}

	protected abstract void process(T t, OutputStream h);

	private final class ProtobufHandler implements Handler {
		private final Connection co;
		private final LimitedInputStream limited;

		public ProtobufHandler(Connection co) {
			this.co = co;
			this.limited = new LimitedInputStream(co.getIn());
		}

		@Override
		public void onRead() {
			InputStream in = co.getIn();
			try {
				in.mark(4096);
				int len = readInt(in);
				if (len < 0 || len < in.available())
					return;
				limited.setLimit(len);
				process(parser.parseFrom(limited), co.getOut());
			} catch (IOException e) {
				try {
					co.getOut().close();
				} catch (IOException e1) { // OK
				}
			} finally {
				try {
					in.reset();
				} catch (IOException e) { // OK
				}
			}
		}

		private final int readInt(InputStream in) throws IOException {
			int read = in.read();
			if (read == -1)
				return -1;
			return CodedInputStream.readRawVarint32(read, in);
		}

		@Override
		public void onWrite() {
		}

		@Override
		public boolean closed(long now, boolean close) {
			return close;
		}

		@Override
		public void free() {
		}

		@Override
		public void reset() {
		}
	}
}