/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.JAXBException;
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
	 * @param m TODO
	 * @param listener
	 * @throws XMLStreamException in case of error
	 */
	void write(XMLStreamWriter w, T t, MarshallerImpl m, Marshaller.Listener listener) throws XMLStreamException, JAXBException;

	/**
	 * read object content
	 * 
	 * @param r        reader to read from
	 * @param parent
	 * @param listener
	 * @return object read
	 * @throws XMLStreamException in case of error
	 */
	T read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException, JAXBException;

	/**
	 * read object content and validate it
	 * 
	 * @param r        reader to read from
	 * @param parent
	 * @param listener
	 * @return object read
	 * @throws XMLStreamException in case of error
	 */
	default T readValidate(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException, JAXBException {
		return read(r, parent, listener);
	}

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
