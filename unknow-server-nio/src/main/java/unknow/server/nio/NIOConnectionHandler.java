package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

@SuppressWarnings("unused")
public interface NIOConnectionHandler {

	/**
	 * called after the connection is initialized
	 * 
	 * @param co the connection
	 * @param now currentTimeMillis
	 * @param sslEngine the sslEngine for ssl connection null for other
	 * @throws IOException on io exception
	 */
	default void onInit(NIOConnection co, long now, SSLEngine sslEngine) throws IOException { // ok
	}

	/**
	 * called when the handshake process finish
	 * 
	 * @param sslEngine the sslEngine
	 * @throws IOException on io exception
	 */
	default void onHandshakeDone(SSLEngine sslEngine) throws IOException { // ok
	}

	/**
	 * called after some data has been read
	 * 
	 * @param b the read buffers
	 * @param now currentTimeMillis
	 * @throws IOException on io exception
	 */
	default void onRead(ByteBuffer b, long now) throws IOException { // ok
	}

	/**
	 * called before a buffer is written
	 * @param b buffer to be written
	 * @return new buffer to write
	 * @throws IOException on io exception
	 */
	default ByteBuffer beforeWrite(ByteBuffer b) throws IOException {
		return b;
	}

	/**
	 * called after data has been written
	 * 
	 * @param now currentTimeMillis
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
	 * @param now currentTimeMillis
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
	 * @param now currentTimeMillis
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
