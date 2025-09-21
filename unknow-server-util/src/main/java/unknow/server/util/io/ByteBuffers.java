package unknow.server.util.io;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import unknow.server.util.ConsumerWithException;

public class ByteBuffers implements Consumer<ByteBuffer> {
	public ByteBuffer[] buf;
	public int len;

	public ByteBuffers(int l) {
		buf = new ByteBuffer[l];
		len = 0;
	}

	@Override
	public void accept(ByteBuffer b) {
		if (len == buf.length)
			buf = Arrays.copyOf(buf, len + 1);
		buf[len++] = b;
	}

	public boolean isEmpty() {
		return len == 0;
	}

	public void clear() {
		for (int i = 0; i < len; i++)
			buf[i] = null;
		len = 0;
	}

	public int remaining() {
		int r = 0;
		for (int i = 0; i < len; i++)
			r += buf[i].remaining();
		return r;
	}

	/**
	 * remove empty buffers
	 */
	public void compact() {
		int o = 0;
		while (o < len && !buf[o].hasRemaining())
			o++;
		if (o == 0)
			return;

		len -= o;
		if (len > 0)
			System.arraycopy(buf, o, buf, 0, len);
		for (int i = len; i < len + o; i++)
			buf[i] = null;
	}

	/**
	 * drain buffers
	 * @param <E> exception thrown from consumer
	 * @param c consumer of buffer
	 * @throws E from Consumer
	 */
	public <E extends Throwable> void drain(ConsumerWithException<ByteBuffer, E> c) throws E {
		if (len == 0)
			return;

		for (int i = 0; i < len; i++) {
			c.accept(buf[i].flip());
			buf[i] = null;
		}
		len = 0;
	}
}
