/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.Marshaller;

/**
 * @author unknow
 * @param <T> xmlElement
 */
public interface XmlHandler<T> {

	/**
	 * write object content
	 * 
	 * @param w        writer to write to
	 * @param t        object to write
	 * @param listener
	 * @throws XMLStreamException in case of error
	 */
	void write(XMLStreamWriter w, T t, Marshaller.Listener listener) throws XMLStreamException;

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

	public static void skipTag(XMLStreamReader r) throws XMLStreamException {
		int i = 1;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.START_ELEMENT)
				i++;
			else if (n == XMLStreamConstants.END_ELEMENT && --i == 0)
				return;
		}
	}
}
