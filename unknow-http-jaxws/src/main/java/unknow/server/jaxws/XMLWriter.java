/**
 * 
 */
package unknow.server.jaxws;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author unknow
 */
public interface XMLWriter extends Closeable {
	/**
	 * start an elements
	 * 
	 * @param name  element name
	 * @param nsUri element ns
	 * @throws IOException in case of error
	 */
	void startElement(String name, String nsUri) throws IOException;

	/**
	 * add an attribute
	 * 
	 * @param name  the name
	 * @param nsUri the ns
	 * @param value the value
	 * @throws IOException in case of error
	 */
	void attribute(String name, String nsUri, String value) throws IOException;

	/**
	 * write some text content
	 * 
	 * @param text the content
	 * @throws IOException in case of eror
	 */
	void text(String text) throws IOException;

	/**
	 * end an element
	 * 
	 * @param name  element name
	 * @param nsUri element ns
	 * @throws IOException in case of error
	 */
	void endElement(String name, String nsUri) throws IOException;
}
