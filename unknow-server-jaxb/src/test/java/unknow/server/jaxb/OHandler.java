/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.Marshaller;

/**
 * @author unknow
 */
public class OHandler extends XmlRootHandler<O> {
	public static final OHandler INSTANCE = new OHandler();

	private static final QName A = new QName("", "a");

	/**
	 * create new OHandler
	 */
	public OHandler() {
		super(new QName("", "o"));
	}

	@Override
	public void write(XMLStreamWriter w, O t, MarshallerImpl m, Marshaller.Listener listener) throws XMLStreamException {
		w.writeAttribute("a", Integer.toString(t.a));
		// or write CDATA
		w.writeCharacters(t.v);
	}

	@Override
	public O read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException {
		O o = new O();
		for (int i = 0; i < r.getAttributeCount(); i++) {
			QName n = r.getAttributeName(i);
			if (A.equals(n))
				o.a = Integer.parseInt(r.getAttributeValue(i));
		}

		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.CHARACTERS) {
				o.v = StrReader.read(r);
				n = r.getEventType();
			}
			if (n == XMLStreamConstants.START_ELEMENT) {
				if (A.equals(r.getName()))
					INSTANCE.read(r, o, listener);
				else
					throw new XMLStreamException("Extra element " + r.getName() + " in " + O.class);
			} else if (n == XMLStreamConstants.END_ELEMENT)
				return o;
		}
		throw new XMLStreamException("EOF");
	}
}
