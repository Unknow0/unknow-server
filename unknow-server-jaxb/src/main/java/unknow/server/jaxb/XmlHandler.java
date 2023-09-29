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

	/**
	 * @param c
	 */
	void collectNS(Consumer<String> c);

	/**
	 * write object content
	 * 
	 * @param w        writer to write to
	 * @param t        object to write
	 * @param listener
	 * @throws XMLStreamException in case of error
	 */
	void write(XMLStreamWriter w, T t, MarshallerImpl listener) throws XMLStreamException;

	/**
	 * read object content
	 * 
	 * @param r        reader to read from
	 * @param parent
	 * @param listener
	 * @return object read
	 * @throws XMLStreamException in case of error
	 */
	T read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException;
}
