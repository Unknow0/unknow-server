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
	 */
	public NIOConnectionPlain(SelectionKey key, NIOConnectionHandler handler) {
		super(key, handler);
	}

	/**
	 * called after the connection is initialized
	 * 
	 * @throws InterruptedException on interrupt
	 */
	@Override
	protected final void onInit() throws InterruptedException { // for override
		handler.onInit(this, null);
	}

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param buf output buffer
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	@Override
	protected final void readFrom(ByteBuffer buf) throws InterruptedException, IOException {
		int l;
		lastRead = System.currentTimeMillis();
		while (true) {
			l = channel.read(buf);
			if (l == -1) {
				in.close();
				return;
			}
			if (l == 0)
				return;
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

	/**
	 * write pending data to the channel
	 * 
	 * @param buf local cache
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	@Override
	protected final void writeInto(ByteBuffer buf) throws InterruptedException, IOException {
		lastWrite = System.currentTimeMillis();
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
