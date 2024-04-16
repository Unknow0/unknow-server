package unknow.server.servlet;

import java.io.IOException;
import java.net.InetSocketAddress;

import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.out.AbstractServletOutput;
import unknow.server.servlet.utils.EventManager;

public interface HttpAdapter {
	ServletContextImpl ctx();

	EventManager events();

	ServletInputStream createInput();

	InetSocketAddress getRemote();

	InetSocketAddress getLocal();

	AbstractServletOutput createOutput();

	void commit() throws IOException;

	void sendError(int sc, Throwable t, String msg) throws IOException;
}
