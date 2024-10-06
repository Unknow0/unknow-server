package unknow.server.nio;

import java.io.IOException;

import javax.net.ssl.SSLEngine;

import unknow.server.util.io.Buffers;

public interface NIOConnectionHandler {

	/**
	 * called after the connection is initialized
	 * 
	 * @param sslEngine the sslEngine for ssl connection null for other
	 * @throws InterruptedException on interrupt
	 */
	void onInit(NIOConnectionAbstract co, SSLEngine sslEngine) throws InterruptedException;

	/**
	 * called when the handshake process finish
	 * @throws InterruptedException on interrupt
	 */
	void onHandshakeDone(SSLEngine sslEngine) throws InterruptedException;

	/**
	 * called after some data has been read
	 * 
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
	 * @param now System.currentMillis()
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
