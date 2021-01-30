/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import unknow.server.nio.util.Buffers;
import unknow.server.nio.util.BuffersInputStream;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public abstract class Handler {
	/** the data waiting to be wrote */
	private final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	protected final Buffers pendingRead = new Buffers();
	private final InputStream in = new BuffersInputStream(pendingRead);

	private Out out = new Out(this);
	SelectionKey key;

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param channel
	 * @param buf
	 * @param executors
	 * @throws IOException
	 */
	protected final void readFrom(SocketChannel channel, ByteBuffer buf) throws IOException {
		buf.clear();
		if (channel.read(buf) == -1) {
			channel.close();
			return;
		}
		buf.flip();
		pendingRead.append(buf);
		handle(in, out);
	}

	/**
	 * write pending data to the channel
	 * 
	 * @param channel where to write
	 * @param buf     local cache
	 * @throws IOException
	 */
	protected final void writeInto(SocketChannel channel, ByteBuffer buf) throws IOException {
		buf.clear();
		while (pendingWrite.read(buf)) {
			buf.flip();
			channel.write(buf);
			if (buf.hasRemaining())
				break;
		}
		if (buf.hasRemaining()) // we didn't write all
			pendingWrite.prepend(buf);
		if (pendingWrite.isEmpty())
			key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * @return the current inputStream
	 */
	public final InputStream getIn() {
		return in;
	}

	/**
	 * @return the current outputStream
	 */
	public final OutputStream getOut() {
		return out;
	}

	public final InetSocketAddress getRemote() throws IOException {
		return (InetSocketAddress) ((SocketChannel) key.channel()).getRemoteAddress();
	}

	public final InetSocketAddress getLocal() throws IOException {
		return (InetSocketAddress) ((SocketChannel) key.channel()).getLocalAddress();
	}

	/**
	 * check if this handler is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		return out.h == null && pendingWrite.isEmpty();
	}

	/**
	 * reset this handler
	 */
	public void reset() {
		out.h = null;
		out = new Out(this);
		pendingWrite.clear();
		pendingRead.clear();
	}

	@Override
	public String toString() {
		return key.channel().toString() + " closed: " + (out.h == null) + " pending: " + pendingWrite.size();
	}

	/**
	 * handle the pendingData
	 */
	protected abstract void handle(InputStream in, OutputStream out);

	private static final class Out extends OutputStream {
		private Handler h;

		Out(Handler h) {
			this.h = h;
		}

		@Override
		public final void write(int b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			h.pendingWrite.append((byte) b);
			h.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		/**
		 * write data
		 * 
		 * @param b data to write
		 * @throws IOException
		 */
		@Override
		public final void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		/**
		 * write some data
		 * 
		 * @param b   data
		 * @param off offset
		 * @param len number of byte to write
		 * @throws IOException
		 */
		@Override
		public final void write(byte[] b, int off, int len) throws IOException {
			if (len == 0)
				return;
			if ((off | len | (off + len) | (b.length - (off + len))) < 0)
				throw new IndexOutOfBoundsException();
			if (h == null)
				throw new IOException("already closed");
			h.pendingWrite.append(b, off, len);
			h.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		/**
		 * request to close the handler
		 */
		@Override
		public void close() {
			flush();
			h = null;
		}

		/**
		 * flush pending data
		 */
		@Override
		public void flush() {
			h.key.selector().wakeup();
		}
	}
}
