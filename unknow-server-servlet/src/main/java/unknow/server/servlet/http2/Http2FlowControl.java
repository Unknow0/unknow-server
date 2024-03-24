package unknow.server.servlet.http2;

public abstract class Http2FlowControl {
	private int window;

	protected Http2FlowControl(int window) {
		this.window = window;
	}

	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}
}
