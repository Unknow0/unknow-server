/**
 * 
 */
package unknow.server.jaxb;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.Marshaller;

/**
 * @author unknow
 * @param <T> content type
 */
public abstract class XmlRootHandler<T> implements XmlHandler<T> {

	private final QName qname;

	/**
	 * create new XmlRootHandler
	 * 
	 * @param qname
	 */
	protected XmlRootHandler(QName qname) {
		this.qname = qname;
	}

	/**
	 * @return element qname
	 */
	public final QName qname() {
		return qname;
	}

	/**
	 * write root element
	 * 
	 * @param w        writer
	 * @param t        object
	 * @param listener
	 * @throws XMLStreamException on error
	 */
	public final void writeRoot(XMLStreamWriter w, T t, Marshaller.Listener listener) throws XMLStreamException {
		NsCollector c = new NsCollector();
		write(c, t, null);

		Map<String, String> ns = c.getNs();
		w.writeStartElement(ns.get(qname.getNamespaceURI()), qname.getLocalPart(), qname.getNamespaceURI());
		for (Entry<String, String> e : ns.entrySet())
			w.writeNamespace(e.getValue(), e.getKey());

		write(w, t, listener);
	}
}
