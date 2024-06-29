/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

/**
 * @author unknow
 */
public interface MessageRW<T> extends MessageBodyWriter<T>, MessageBodyReader<T> {
	// ok
}
