package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.net.ssl.SSLEngine;

@SuppressWarnings("unused")
public interface NIOConnectionHandler {

	/**
	 * @return true if the handler should be init async
	 */
	default boolean asyncInit() {
		return false;
	}

	/**
	 * called to initialize the connection
	 * 
	 * @param co the connection
	 * @param now nanoTime
	 * @param sslEngine the sslEngine for ssl connection null for other
	 * @throws IOException on io exception
	 */
	default void init(NIOConnection co, long now, SSLEngine sslEngine) throws IOException { // ok
	}

	/**
	 * called when the handshake process finish
	 * 
	 * @param sslEngine the sslEngine
	 * @param now nanoTime
	 * @throws IOException on io exception
	 */
	default void onHandshakeDone(SSLEngine sslEngine, long now) throws IOException { // ok
	}

	/**
	 * called after some data has been read
	 * 
	 * @param b the read buffers
	 * @param now nanoTime
	 * @throws IOException on io exception
	 */
	default void onRead(ByteBuffer b, long now) throws IOException { // ok
	}

	/**
	 * called before a buffer is written (allow to collect buffers)
	 * @param b buffer to be written
	 * @param now nanoTime
	 * @param c consumer of generated buffers
	 * @throws IOException on io exception
	 */
	default void prepareWrite(ByteBuffer b, long now, Consumer<ByteBuffer> c) throws IOException {
		c.accept(b);
	}

	/**
	 * called before a buffer is written
	 * @param now nanoTime
	 * @param c consumer of generated buffers
	 * @throws IOException on io exception
	 */
	default void beforeWrite(long now, Consumer<ByteBuffer> c) throws IOException { // ok
	}

	/**
	 * check if this handler has some pengin write
	 * @return true if the handler has pending write
	 */
	default boolean hasPendingWrites() {
		return false;
	}

	/**
	 * called after data has been written
	 * 
	 * @param now nanoTime
	 * @throws IOException on io exception
	 */
	default void onWrite(long now) throws IOException { // ok
	}

	/**
	 * called when the output is closed
	 */
	default void onOutputClosed() { // ok
	}

	/**
	 * check if the connection can be closed
	 * 
	 * @param now nanoTime
	 * @param stop if true the server is in stop phase
	 * @return true is the collection can be closed
	 */
	default boolean canClose(long now, boolean stop) {
		return false;
	}

	/**
	 * start the closing of the connection
	 */
	default void startClose() { // ok
	}

	/**
	 * try to finish the closing
	 * @param now nanoTime
	 * @return true if the connection is closed
	 */
	default boolean finishClosing(long now) {
		return true;
	}

	/**
	 * called when the connection is closed
	 */
	default void doneClosing() { // ok
	}

}
