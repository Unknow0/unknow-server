package unknow.server.servlet;

public interface HttpProcessor {

	/**
	 * called when we have more data
	 * @throws InterruptedException on interrupt
	 */
	void process() throws InterruptedException;

	/** @return true if the connection is closed */
	boolean isClosed();

	/** close the process */
	void close();

	public static interface HttpProcessorFactory {
		/**
		 * create a processor if it can process it
		 * @param co the connection
		 * @return the processor or null
		 * @throws InterruptedException on interrupt
		 */
		HttpProcessor create(HttpConnection co) throws InterruptedException;
	}
}
