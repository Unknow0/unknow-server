/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;

/**
 * @author unknow
 */
public interface Marshaler<T> {
	public void marshall(MarshalerRegistry m, T t, XMLWriter w) throws IOException;

}
