/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author unknow
 */
public class MarshalerRegistry implements Marshaler<Object> {

	private final Map<Class<?>, Marshaler<?>> marshalers = new HashMap<>();

	/**
	 * create new MarshalerRegistry
	 */
	public MarshalerRegistry() {
		register(Envelope.class, ENVELOPE);
		register(OperationWrapper.class, OPERATION);
		register(Element.class, ELEMENT);
	}

	/**
	 * register a marshaler
	 * 
	 * @param <T>
	 * @param clazz
	 * @param m
	 */
	public <T> void register(Class<T> clazz, Marshaler<T> m) {
		marshalers.put(clazz, m);
	}

	/**
	 * @param cl a class
	 * @return the marshaler
	 * @throws IOException if no marshaler exists
	 */
	@SuppressWarnings("unchecked")
	public <T> Marshaler<T> get(Class<T> cl) throws IOException {
		Marshaler<T> m = (Marshaler<T>) marshalers.get(cl);
		if (m == null)
			throw new IOException("not marshaler found for '" + cl + "'");
		return m;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void marshall(MarshalerRegistry m, Object t, XMLWriter w) throws IOException {
		get((Class<Object>) t.getClass()).marshall(m, t, w);
	}

	/**
	 * marshal envelope
	 */
	public static final Marshaler<Envelope> ENVELOPE = (m, t, w) -> {
		w.startElement("Envelope", "http://schemas.xmlsoap.org/soap/envelope/");
		int l = t.getHeaderSize();
		if (l > 0) {
			w.startElement("Header", "http://schemas.xmlsoap.org/soap/envelope/");
			for (int i = 0; i < l; i++) {
				Object o = t.getHeader(i);
				m.marshall(m, o, w);
			}
			w.endElement("Header", "http://schemas.xmlsoap.org/soap/envelope/");
		}
		w.startElement("Body", "http://schemas.xmlsoap.org/soap/envelope/");
		l = t.getBodySize();
		for (int i = 0; i < l; i++) {
			Object o = t.getBody(i);
			m.marshall(m, o, w);
		}
		w.endElement("Body", "http://schemas.xmlsoap.org/soap/envelope/");
		w.endElement("Envelope", "http://schemas.xmlsoap.org/soap/envelope/");
	};
	/**
	 * marshal operationWrapper
	 */
	public static final Marshaler<OperationWrapper> OPERATION = (m, t, w) -> {
		w.startElement(t.getName(), t.getNs());
		for (Object o : t.getValues())
			m.marshall(m, o, w);
		w.endElement(t.getName(), t.getNs());
	};
	/**
	 * marshal element
	 */
	public static final Marshaler<Element> ELEMENT = (m, t, w) -> {
		w.startElement(t.name, t.ns);
		m.marshall(m, t.value, w);
		w.endElement(t.name, t.ns);
	};
}
