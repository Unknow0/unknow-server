/**
 * 
 */
package unknow.server.nio.cli;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.NIOServerListener;

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

	/** number of io thread to use, default to 10 */
	@Option(names = "--iothread", description = "number of io thread to use, default to 10", descriptionKey = "iothread")
	public int iothread = 10;

	/** server listener */
	@Option(names = "--listener", description = "set the listener, default to NOP", descriptionKey = "listener", converter = NIOListenerConverter.class)
	public NIOServerListener listener = NIOServerListener.NOP;

	/** selectionTime */
	@Option(names = "--selecttime", description = "timeout on Selector.select, 0=unlimited", descriptionKey = "selecttime")
	public long selectTime = 0;

	/**
	 * @return local address to bind to
	 */
	public InetSocketAddress getInetAddress() {
		if (address == null)
			return new InetSocketAddress(port);
		return new InetSocketAddress(address, port);
	}

	@Override
	public Integer call() throws Exception {
		if (selectTime < 0)
			throw new IllegalArgumentException("selectTime should not be <0");

		new unknow.server.nio.NIOServer(getInetAddress(), iothread, handler, listener, selectTime).run();
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