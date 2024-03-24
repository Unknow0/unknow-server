package unknow.server.servlet.http2;

import java.io.IOException;
import java.net.InetSocketAddress;

import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpError;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.out.AbstractServletOutput;

public class Http2Stream extends HttpWorker implements Http2FlowControl {
	private int window;

	protected Http2Stream(HttpConnection co, int window) {
		super(co);
		this.window = window;
	}

	@Override
	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}

	@Override
	public ServletInputStream createInput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getLocal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractServletOutput createOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void commit() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doStart() throws IOException, InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doDone() {
		// TODO Auto-generated method stub

	}
}
