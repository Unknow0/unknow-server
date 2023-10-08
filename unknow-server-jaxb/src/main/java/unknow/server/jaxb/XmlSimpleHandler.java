/**
 * 
 */
package unknow.server.jaxb;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author unknow
 */
public interface XmlSimpleHandler<T> extends XmlHandler<T> {

	@Override
	default void collectNS(Consumer<String> c) {
		c.accept("http://www.w3.org/2001/XMLSchema");
	}

	@Override
	default void write(XMLStreamWriter w, T t, MarshallerImpl listener) throws XMLStreamException {
		listener.beforeMarshal(t);
		w.writeCharacters(toString(t));
		listener.afterMarshal(t);
	}

	@Override
	default T read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException {
		T o = null;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.CHARACTERS) {
				o = toObject(StrReader.read(r));
				listener.beforeUnmarshal(o, parent);
				n = r.getEventType();
			}
//			if (n == XMLStreamConstants.START_ELEMENT)
//				throw new XMLStreamException("Extra element " + r.getName() + " in " + O.class);
			if (n == XMLStreamConstants.END_ELEMENT) {
				listener.afterUnmarshal(o, parent);
				return o;
			}
		}
		throw new XMLStreamException("EOF");
	}

	String toString(T t) throws XMLStreamException;

	T toObject(String s) throws XMLStreamException;
}