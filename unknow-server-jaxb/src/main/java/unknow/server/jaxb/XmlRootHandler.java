/**
 * 
 */
package unknow.server.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
	public final void writeRoot(XMLStreamWriter w, T t, MarshallerImpl listener) throws XMLStreamException {
		Map<String, Integer> nscount = new HashMap<>();
		nscount.put(qname.getNamespaceURI(), 1);
		collectNS(n -> nscount.merge(n, 1, Integer::sum));
		Map<String, String> ns = buildNsMapping(nscount);
		for (Entry<String, String> e : ns.entrySet())
			w.setPrefix(e.getKey(), e.getValue());

		w.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
		for (Entry<String, String> e : ns.entrySet())
			w.writeNamespace(e.getKey(), e.getValue());

		write(w, t, listener);
	}

	public static Map<String, String> buildNsMapping(Map<String, Integer> ns) {
		if (ns.isEmpty())
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<>();

		if (ns.containsKey("")) {
			map.put("", "");
			ns.remove("");
		}
		List<String> list = new ArrayList<>(ns.keySet());
		Collections.sort(list, (a, b) -> ns.get(b) - ns.get(a));
		Iterator<String> it = list.iterator();
		if (map.isEmpty())
			map.put("", it.next());
		int i = 0;
		while (it.hasNext())
			map.put(prefix(i++), it.next());
		return map;
	}

	private static final char[] PREFIX_FIRST = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b', 'n', '_', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N' };
	private static final char[] PREFIX_OTHER = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b', 'n', '_', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.' };

	/**
	 * generate a xmlns prefix
	 * 
	 * @param t the value to encode
	 * @return the prefix
	 */
	private static final String prefix(int t) {
		if (t < PREFIX_FIRST.length)
			return new String(PREFIX_FIRST, t, 1);
		StringBuilder sb = new StringBuilder();
		int i = t % PREFIX_FIRST.length;
		sb.append(PREFIX_FIRST[i]);
		t -= i;
		while (t > PREFIX_OTHER.length) {
			i = t % PREFIX_OTHER.length;
			sb.append(PREFIX_OTHER[i]);
			t -= i;
		}
		sb.append(PREFIX_OTHER[t]);
		return sb.toString();
	}
}
