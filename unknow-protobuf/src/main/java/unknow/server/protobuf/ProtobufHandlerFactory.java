package unknow.server.protobuf;

/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Parser;

import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;

/**
 * @author unknow
 */
public abstract class ProtobufHandlerFactory<T> extends HandlerFactory {
	private final Parser<T> parser;

	public ProtobufHandlerFactory(Parser<T> parser) {
		this.parser = parser;
	}

	@Override
	protected Handler create() {
		return new ProtobufHandler(this);
	}

	protected abstract void process(T t, OutputStream h);

	private final class ProtobufHandler extends Handler {
		private final LimitedInputStream limited;

		public ProtobufHandler(HandlerFactory factory) {
			super(factory);
			this.limited = new LimitedInputStream(getIn());
		}

		@Override
		public void onRead() {
			InputStream in = getIn();
			try {
				in.mark(4096);
				int len = readInt(in);
				if (len < 0 || len < in.available())
					return;
				limited.setLimit(len);
				process(parser.parseFrom(limited), getOut());
			} catch (IOException e) {
				try {
					getOut().close();
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
	}
}