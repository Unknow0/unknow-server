package unknow.server.nio;

/**
 * statistics of a nio connection
 */
public class ConnectionStats {
	private final boolean hasPengingWrite;
	private final boolean closing;
	private final long lastCheck;
	private final long lastAction;

	public ConnectionStats(boolean hasPengingWrite, boolean closing, long lastCheck, long lastAction) {
		this.hasPengingWrite = hasPengingWrite;
		this.closing = closing;
		this.lastCheck = lastCheck;
		this.lastAction = lastAction;
	}

	/**
	 * @return true if the connection has pending write
	 */
	public boolean hasPengingWrite() {
		return hasPengingWrite;
	}

	/**
	 * @return true if the connection is closing
	 */
	public boolean isClosing() {
		return closing;
	}

	/**
	 * @return nanotime of last connection check
	 */
	public long lastCheck() {
		return lastCheck;
	}

	/**
	 * @return nano time of last action  on the connection
	 */
	public long lastAction() {
		return lastAction;
	}

}
