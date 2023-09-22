/**
 * 
 */
package unknow.server.jaxb.handler;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import unknow.server.jaxb.StrReader;
import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public abstract class XmlDefaultHandler<T> implements XmlSimpleHandler<T> {
	@Override
	public void collectNS(Consumer<String> c) {
		c.accept("http://www.w3.org/2001/XMLSchema");
	}

	@Override
	public final void write(XMLStreamWriter w, T t) throws XMLStreamException {
		w.writeCharacters(toString(t));
	}

	@Override
	public final T read(XMLStreamReader r) throws XMLStreamException {
		T o = null;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.CHARACTERS) {
				o = toObject(StrReader.read(r));
				n = r.getEventType();
			}
//			if (n == XMLStreamConstants.START_ELEMENT)
//				throw new XMLStreamException("Extra element " + r.getName() + " in " + O.class);
			if (n == XMLStreamConstants.END_ELEMENT)
				return o;
		}
		throw new XMLStreamException("EOF");
	}
}
