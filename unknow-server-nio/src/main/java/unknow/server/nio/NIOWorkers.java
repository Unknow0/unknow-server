/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import unknow.server.nio.NIOServer.ConnectionFactory;

/**
 * represent a worker groups
 * @author unknow
 */
public interface NIOWorkers {
	/**
	 * register a socket to an IOWorker
	 * 
	 * @param socket the socket to register
	 * @param factory the connection factory
	 * @throws IOException in case of error
	 */
	void register(SocketChannel socket, ConnectionFactory factory) throws IOException;

	/**
	 * start the IOWorker
	 */
	void start();

	/**
	 * gracefully stop the workers
	 */
	void stop();

	/**
	 * wait for the worker to stop
	 */
	void await();

	/**
	 * list all workers
	 * @return the workers
	 */
	Collection<NIOWorker> workers();
}
