/**
 * 
 */
package unknow.server.jaxb;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author unknow
 * @param <T> xmlElement
 */
public interface XmlHandler<T> {

	void collectNS(Consumer<String> c);

	/**
	 * write object content
	 * 
	 * @param w writer to write to
	 * @param t object to write
	 * @throws XMLStreamException in case of error
	 */
	void write(XMLStreamWriter w, T t) throws XMLStreamException;

	/**
	 * read object content
	 * 
	 * @param r reader to read from
	 * @return object read
	 * @throws XMLStreamException in case of error
	 */
	T read(XMLStreamReader r) throws XMLStreamException;
}
