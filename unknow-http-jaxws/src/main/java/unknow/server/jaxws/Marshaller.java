/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;

/**
 * @author unknow
 */
public interface Marshaller<T> {
	public void marshall(MarshallerRegistry m, T t, XMLWriter w) throws IOException;
	
	@SuppressWarnings("unchecked")
	public static final Marshaller<Envelope> ENVELOPE = (m, t, w) -> {
		w.startElement("Envelope", "http://www.w3.org/2001/12/soap-envelope");
		int l = t.getHeaderSize();
		if (l > 0) {
			w.startElement("Header", "http://www.w3.org/2001/12/soap-envelope");
			for (int i = 0; i < l; i++) {
				Object o = t.getHeader(i);
				m.get((Class<Object>) o.getClass()).marshall(m, o, w);
			}
			w.endElement("Header", "http://www.w3.org/2001/12/soap-envelope");
		}
		w.startElement("Body", "http://www.w3.org/2001/12/soap-envelope");
		l = t.getBodySize();
		for (int i = 0; i < l; i++) {
			Object o = t.getBody(i);
			m.get((Class<Object>) o.getClass()).marshall(m, o, w);
		}
		w.endElement("Body", "http://www.w3.org/2001/12/soap-envelope");
		w.endElement("Envelope", "http://www.w3.org/2001/12/soap-envelope");
	};

	@SuppressWarnings("unchecked")
	public static final Marshaller<Element> ELEMENT = (m, t, w) -> {
		w.startElement(t.name, t.ns);
		m.get((Class<Object>) t.value.getClass()).marshall(m, t.value, w);
		w.endElement(t.name, t.ns);
	};

	public static final Marshaller<OperationWrapper> OPERATION = (m, t, w) -> {
		w.startElement(t.name, t.ns);
		for (Element e : t.values)
			ELEMENT.marshall(m, e, w);
		w.endElement(t.name, t.ns);
	};
}
