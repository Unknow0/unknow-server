/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author unknow
 */
public interface HttpRawProcessor {
	void process(HttpRawRequest request, OutputStream out) throws IOException;
}