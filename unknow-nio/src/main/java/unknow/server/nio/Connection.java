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
public final class Connection {
	/** factory that created the handler */
	private final Handler handler;

	/** the data waiting to be wrote */
	public final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	public final Buffers pendingRead = new Buffers();

	/** Stream of pending data */
	private final InputStream in = new BuffersInputStream(pendingRead);

	/** Output stream */
	private Out out;

	/** selection key */
	private SelectionKey key;

	private long lastRead;
	private long lastWrite;

	protected Connection(HandlerFactory factory) {
		this.handler = factory.create(this);
	}

	void attach(SelectionKey key) {
		this.key = key;
		this.out = new Out(this);
		lastRead = lastWrite = System.currentTimeMillis();
	}

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param channel source channel
	 * @param buf     output buffer
	 * @throws IOException          in case of error
	 * @throws InterruptedException
	 */
	protected final void readFrom(SocketChannel channel, ByteBuffer buf) throws IOException, InterruptedException {
		lastRead = System.currentTimeMillis();
		buf.clear();
		if (channel.read(buf) == -1) {
			channel.close();
			return;
		}
		buf.flip();
		pendingRead.write(buf);
		handler.onRead();
	}

	/**
	 * write pending data to the channel
	 * 
	 * @param channel where to write
	 * @param buf     local cache
	 * @throws IOException          in case of error
	 * @throws InterruptedException
	 */
	protected final void writeInto(SocketChannel channel, ByteBuffer buf) throws IOException, InterruptedException {
		lastWrite = System.currentTimeMillis();
		buf.clear();
		while (pendingWrite.read(buf, false)) {
			buf.flip();
			channel.write(buf);
			if (buf.hasRemaining())
				break;
		}
		if (buf.hasRemaining()) // we didn't write all
			pendingWrite.prepend(buf);
		if (pendingWrite.isEmpty())
			key.interestOps(SelectionKey.OP_READ);
		handler.onWrite();
	}

	/**
	 * timestamp of the last read
	 * 
	 * @return timestamp in ms
	 */
	public final long lastRead() {
		return lastRead;
	}

	/**
	 * timestamp of the last write
	 * 
	 * @return timestamp in ms
	 */
	public final long lastWrite() {
		return lastWrite;
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
	public final Out getOut() {
		return out;
	}

	/**
	 * the remote address of the channel is connected to
	 * 
	 * @return the remote address
	 */
	public final InetSocketAddress getRemote() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getRemoteAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * the local address of the channel this handler has
	 * 
	 * @return the local address
	 */
	public final InetSocketAddress getLocal() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getLocalAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * check if this connection is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		try {
			return out.isClosed() && pendingWrite.isEmpty();
		} catch (InterruptedException e) {
			return true;
		}
	}

	public boolean closed(long now, boolean stop) {
		return handler.closed(now, stop);
	}

	/**
	 * free the handler
	 * 
	 * @throws InterruptedException
	 */
	public final void free() throws InterruptedException {
		reset();
		handler.free();
	}

	/**
	 * reset this handler
	 * 
	 * @throws InterruptedException
	 */
	public void reset() throws InterruptedException {
		out.close();
		pendingWrite.clear();
		pendingRead.clear();
		handler.reset();
	}

	@Override
	public String toString() {
		try {
			return key.channel().toString() + " closed: " + isClosed() + " pending: " + pendingWrite.length();
		} catch (InterruptedException e) {
			return "";
		}
	}

	public static final class Out extends OutputStream {
		private Connection h;

		private Out(Connection h) {
			this.h = h;
		}

		@Override
		public synchronized final void write(int b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			try {
				h.pendingWrite.write(b);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
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
		 * @param buf data
		 * @param off offset
		 * @param len number of byte to write
		 * @throws IOException
		 */
		@Override
		public synchronized final void write(byte[] buf, int off, int len) throws IOException {
			if (len == 0)
				return;
			if ((off | len | (off + len) | (buf.length - (off + len))) < 0)
				throw new IndexOutOfBoundsException();
			if (h == null)
				throw new IOException("already closed");
			try {
				h.pendingWrite.write(buf, off, len);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
			h.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		/**
		 * request to close the handler
		 * 
		 */
		@Override
		public synchronized void close() {
			flush();
			h = null;
			Exception e = new Exception();
			StackTraceElement[] stackTrace = e.getStackTrace();
			if (stackTrace.length < 2 || !"unknow.server.nio.Connection".equals(stackTrace[1].getClassName()) || !"reset".equals(stackTrace[1].getMethodName()))
				e.printStackTrace();
		}

		public synchronized boolean isClosed() {
			return h == null;
		}

		/**
		 * flush pending data
		 * 
		 */
		@Override
		public synchronized void flush() {
			if (h == null)
				return;
			try {
				if (!h.pendingWrite.isEmpty())
					h.key.selector().wakeup();
			} catch (InterruptedException e) {
			}
		}
	}
}
