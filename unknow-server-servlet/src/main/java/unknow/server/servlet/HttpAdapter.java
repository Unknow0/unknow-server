package unknow.server.servlet;

import java.io.IOException;
import java.net.InetSocketAddress;

import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.out.AbstractServletOutput;

public interface HttpAdapter {
	ServletContextImpl ctx();

	ServletInputStream createInput();

	InetSocketAddress getRemote();

	InetSocketAddress getLocal();

	AbstractServletOutput createOutput();

	void commit() throws IOException;
}
