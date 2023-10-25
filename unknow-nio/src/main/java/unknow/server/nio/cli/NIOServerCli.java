/**
 * 
 */
package unknow.server.nio.cli;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerListener;
import unknow.server.nio.NIOWorker;
import unknow.server.nio.NIOWorkers;
import unknow.server.nio.NIOWorkers.RoundRobin;

/**
 * NioServer configuration
 * 
 * @author Unknow
 */
@Command(name = "nioserver", defaultValueProvider = DefaultOptionProvider.class)
public class NIOServerCli implements Callable<Integer> {

	/** address to bind to */
	@Option(names = { "-a", "--address" }, description = "address to bind to, default to all interface", descriptionKey = "address")
	public String address;

	/** port the server will listen on */
	@Option(names = { "-p", "--port" }, required = true, description = "port the server will listen on", descriptionKey = "port")
	public int port;

	/** handler factory */
	@Option(names = "--handler", descriptionKey = "handler", converter = ObjectConverter.class)
	public HandlerFactory handler;

	/** number of io thread to use, default to the number of CPU */
	@Option(names = "--iothread", description = "number of io thread to use, default to the number of CPU", descriptionKey = "iothread")
	public int iothread = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

	/** server listener */
	@Option(names = "--listener", description = "set the listener, default to NOP", descriptionKey = "listener", converter = NIOListenerConverter.class)
	public NIOServerListener listener = NIOServerListener.NOP;

	/** selectionTime */
	@Option(names = "--selecttime", description = "timeout on Selector.select, 0=unlimited", descriptionKey = "selecttime")
	public long selectTime = 200;

	/** shutdown port */
	@Option(names = "--shutdown-port", description = "port to open to gracefuly shutdown the server", descriptionKey = "shutdown-port")
	public int shutdownPort = 0;

	/**
	 * @return local address to bind to
	 */
	public InetSocketAddress getInetAddress() {
		if (address == null)
			return new InetSocketAddress(port);
		return new InetSocketAddress(address, port);
	}

	/**
	 * called before the server start but after the property are parsed
	 */
	protected void init() throws Exception { // OK
	}

	/**
	 * called after the server shut down
	 */
	protected void destroy() throws Exception { // OK
	}

	@Override
	public final Integer call() throws Exception {
		NIOServer nioServer = null;
		try {
			init();
			if (selectTime < 0)
				throw new IllegalArgumentException("selectTime should not be <0");

			NIOWorkers workers;
			if (iothread == 1)
				workers = new NIOWorker(0, listener, selectTime);
			else {
				NIOWorker[] w = new NIOWorker[iothread];
				for (int i = 0; i < iothread; i++)
					w[i] = new NIOWorker(i, listener, selectTime);
				workers = new RoundRobin(w);
			}
			nioServer = new NIOServer(workers, listener);
			nioServer.bind(getInetAddress(), handler);
			if (shutdownPort > 0)
				nioServer.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), shutdownPort), new ShutdownHandler(nioServer));
			nioServer.start();
			nioServer.await();
		} finally {
			if (nioServer != null) {
				nioServer.stop();
				nioServer.await();
			}
			destroy();
		}
		return 0;
	}

	/**
	 * the main
	 * 
	 * @param arg cli param
	 */
	public static void main(String[] arg) {
		System.exit(new CommandLine(new NIOServerCli()).execute(arg));
	}
}