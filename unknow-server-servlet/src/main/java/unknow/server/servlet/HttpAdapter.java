package unknow.server.servlet;

import java.io.IOException;

import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletContextImpl;

public interface HttpAdapter {
	ServletContextImpl ctx();

	AbstractServletOutput createOutput();

	void commit() throws IOException;

	void sendError(int sc, Throwable t, String msg) throws IOException;
}
