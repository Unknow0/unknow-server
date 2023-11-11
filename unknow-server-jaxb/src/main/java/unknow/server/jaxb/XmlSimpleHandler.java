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
 */
public interface XmlSimpleHandler<T> extends XmlHandler<T> {

	@Override
	default void write(XMLStreamWriter w, T t, MarshallerImpl m, Marshaller.Listener listener) throws XMLStreamException, JAXBException {
		if (listener != null)
			listener.beforeMarshal(t);
		w.writeCharacters(toString(t));
		if (listener != null)
			listener.afterMarshal(t);
	}

	@Override
	default T read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException, JAXBException {
		T o = null;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.CHARACTERS) {
				o = toObject(StrReader.read(r));
				listener.beforeUnmarshal(o, parent);
				n = r.getEventType();
			}
			if (n == XMLStreamConstants.END_ELEMENT) {
				listener.afterUnmarshal(o, parent);
				return o;
			}
		}
		throw new XMLStreamException("EOF");
	}

	String toString(T t) throws XMLStreamException, JAXBException;

	T toObject(String s) throws XMLStreamException, JAXBException;
}
