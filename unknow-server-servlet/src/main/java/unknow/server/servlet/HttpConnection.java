/**
 * 
 */
package unknow.server.servlet;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import unknow.server.nio.NIOConnection.Out;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.io.Buffers;

public interface HttpConnection {
	InetSocketAddress getRemote();

	InetSocketAddress getLocal();

	Buffers pendingRead();

	Buffers pendingWrite();

	InputStream getIn();

	Out getOut();

	void flush();

	void toggleKeyOps();

	<T> Future<T> submit(Runnable r);

	ServletContextImpl getCtx();

	ServletManager getServlet();

	EventManager getEvents();

	int getkeepAlive();

}