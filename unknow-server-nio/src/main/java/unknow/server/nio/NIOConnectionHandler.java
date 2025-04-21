package unknow.server.nio;

import java.io.IOException;

import javax.net.ssl.SSLEngine;

import unknow.server.util.io.Buffers;

public interface NIOConnectionHandler {

	/**
	 * called after the connection is initialized
	 * 
	 * @param co the connection
	 * @param sslEngine the sslEngine for ssl connection null for other
	 */
	void onInit(NIOConnectionAbstract co, SSLEngine sslEngine);

	/**
	 * called when the handshake process finish
	 * 
	 * @param sslEngine the sslEngine
	 * @throws InterruptedException on interrupt
	 */
	void onHandshakeDone(SSLEngine sslEngine) throws InterruptedException;

	/**
	 * called after some data has been read
	 * 
	 * @param b the read buffers
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	void onRead(Buffers b) throws InterruptedException, IOException;

	/**
	 * called after data has been written
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	void onWrite() throws InterruptedException, IOException;

	/**
	 * check if the connection is closed and should be stoped
	 * 
	 * @param now currentTimeMillis
	 * @param stop if true the server is in stop phase
	 * @return true is the collection is closed
	 */
	boolean closed(long now, boolean stop);

	/**
	 * called when the connection is free
	 * 
	 * @throws IOException on io exception
	 */
	void onFree() throws IOException;
}
