/**
 * 
 */
package unknow.server.http;

import java.io.IOException;

/**
 * @author unknow
 */
public interface HttpRawProcessor {
	/**
	 * called when we have to process a new request
	 * /!\ should be thread safe 
	 * 
	 * @param handler
	 * @throws IOException
	 */
	void process(HttpHandler handler) throws IOException;
}