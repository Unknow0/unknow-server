package unknow.server.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public final class NsCollector implements XMLStreamWriter {
	private final Map<String, Integer> ns = new HashMap<>();

	public final Map<String, String> getNs() {
		return buildNsMapping(ns);
	}

	@Override
	public final void writeStartElement(String localName) throws XMLStreamException { //ok
	}

	@Override
	public final void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeEmptyElement(String localName) throws XMLStreamException { //ok
	}

	@Override
	public final void writeEndElement() throws XMLStreamException { //ok
	}

	@Override
	public final void writeEndDocument() throws XMLStreamException { //ok
	}

	@Override
	public final void close() throws XMLStreamException { //ok
	}

	@Override
	public final void flush() throws XMLStreamException { //ok
	}

	@Override
	public final void writeAttribute(String localName, String value) throws XMLStreamException { // ok
	}

	@Override
	public final void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
		if (!namespaceURI.isEmpty())
			ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		if (!namespaceURI.isEmpty())
			ns.merge(namespaceURI, 1, Integer::sum);
	}

	@Override
	public final void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException { //ok
	}

	@Override
	public final void writeDefaultNamespace(String namespaceURI) throws XMLStreamException { //ok
	}

	@Override
	public final void writeComment(String data) throws XMLStreamException { //ok
	}

	@Override
	public final void writeProcessingInstruction(String target) throws XMLStreamException { //ok
	}

	@Override
	public final void writeProcessingInstruction(String target, String data) throws XMLStreamException { //ok
	}

	@Override
	public final void writeCData(String data) throws XMLStreamException { //ok
	}

	@Override
	public final void writeDTD(String dtd) throws XMLStreamException { //ok
	}

	@Override
	public final void writeEntityRef(String name) throws XMLStreamException { //ok
	}

	@Override
	public final void writeStartDocument() throws XMLStreamException { //ok
	}

	@Override
	public final void writeStartDocument(String version) throws XMLStreamException { //ok
	}

	@Override
	public final void writeStartDocument(String encoding, String version) throws XMLStreamException { //ok
	}

	@Override
	public final void writeCharacters(String text) throws XMLStreamException { //ok
	}

	@Override
	public final void writeCharacters(char[] text, int start, int len) throws XMLStreamException { //ok
	}

	@Override
	public final String getPrefix(String uri) throws XMLStreamException {
		return null;
	}

	@Override
	public final void setPrefix(String prefix, String uri) throws XMLStreamException { //ok
	}

	@Override
	public final void setDefaultNamespace(String uri) throws XMLStreamException { //ok
	}

	@Override
	public final void setNamespaceContext(NamespaceContext context) throws XMLStreamException { // ok
	}

	@Override
	public final NamespaceContext getNamespaceContext() {
		return null;
	}

	@Override
	public final Object getProperty(String name) throws IllegalArgumentException {
		return null;
	}

	public final static Map<String, String> buildNsMapping(Map<String, Integer> ns) {
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
			map.put(it.next(), "");
		int i = 0;
		while (it.hasNext())
			map.put(it.next(), prefix(i++));
		return map;
	}

	private static final char[] PREFIX_FIRST = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b',
			'n', '_', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N' };
	private static final char[] PREFIX_OTHER = { 'a', 'z', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'w', 'x', 'c', 'v', 'b',
			'n', 'A', 'Z', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'Q', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'W', 'X', 'C', 'V', 'B', 'N', '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', '-', '.' };

	/**
	 * generate a xmlns prefix
	 * 
	 * @param t the value to encode
	 * @return the prefix
	 */
	public final static String prefix(int t) {
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
