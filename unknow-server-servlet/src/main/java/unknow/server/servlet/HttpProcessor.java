package unknow.server.servlet;

public interface HttpProcessor {

	/**
	 * called when we have more data
	 * @throws InterruptedException on interrupt
	 */
	void process() throws InterruptedException;

	/** 
	 * check if the connection can be closed
	 * @param stop if true the server want to stop
	 * @return true if the connection is closed
	 */
	boolean isClosable(boolean stop);

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
