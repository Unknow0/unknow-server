package unknow.server.nio;

public class ConnectionStats {
	private final boolean hasPengingWrite;
	private final boolean closing;
	private final long lastCheck;
	private final long closingTime;

	public ConnectionStats(boolean hasPengingWrite, boolean closing, long lastCheck, long closingTime) {
		this.hasPengingWrite = hasPengingWrite;
		this.closing = closing;
		this.lastCheck = lastCheck;
		this.closingTime = closingTime;
	}

	public boolean hasPengingWrite() {
		return hasPengingWrite;
	}

	public boolean isClosing() {
		return closing;
	}

	public long lastCheck() {
		return lastCheck;
	}

	public long closingTime() {
		return closingTime;
	}

}
