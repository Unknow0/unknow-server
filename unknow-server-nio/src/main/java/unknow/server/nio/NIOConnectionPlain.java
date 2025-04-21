/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public class NIOConnectionPlain extends NIOConnectionAbstract {
	private static final Logger logger = LoggerFactory.getLogger(NIOConnectionPlain.class);

	/**
	 * create new connection
	 * 
	 * @param key the selectionKey
	 * @param now currentTimeMillis
	 * @param handler the connection handler
	 */
	public NIOConnectionPlain(SelectionKey key, long now, NIOConnectionHandler handler) {
		super(key, now, handler);
	}

	/**
	 * called after the connection is initialized
	 */
	@Override
	protected final void onInit() { // for override
		handler.onInit(this, null);
	}

	@Override
	protected final boolean readFrom(ByteBuffer buf, long now) throws InterruptedException, IOException {
		int l;
		lastRead = now;
		while (true) {
			l = channel.read(buf);
			if (l == -1) {
				in.close();
				return false;
			}
			if (l == 0)
				return true;
			buf.flip();

			if (logger.isTraceEnabled()) {
				buf.mark();
				byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);
				logger.trace("read {}", new String(bytes));
				buf.reset();
			}
			pendingRead.write(buf);
			handler.onRead(pendingRead);
		}
	}


	@Override
	protected final void writeInto(ByteBuffer buf, long now) throws InterruptedException, IOException {
		lastWrite = now;
		while (pendingWrite.read(buf, false)) {
			buf.flip();

			if (logger.isTraceEnabled()) {
				buf.mark();
				byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);
				logger.trace("writ {}", new String(bytes));
				buf.reset();
			}

			channel.write(buf);
			if (buf.hasRemaining()) {
				pendingWrite.prepend(buf);
				break;
			}
		}
		toggleKeyOps();
		handler.onWrite();
	}
}
