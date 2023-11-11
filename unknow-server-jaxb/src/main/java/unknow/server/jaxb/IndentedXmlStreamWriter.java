package unknow.server.jaxb;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class IndentedXmlStreamWriter implements XMLStreamWriter {
	private static final int WROTE_MARKUP = 1;
	private static final int WROTE_DATA = 2;

	private static final String NL = "\n";
	private static final String INDENT = "  ";

	private final XMLStreamWriter out;

	/** How deeply nested the current scope is. The root element is depth 1. */
	private int depth = 0; // document scope
	private int[] stack = new int[] { 0, 0, 0, 0 }; // nothing written yet

	public IndentedXmlStreamWriter(XMLStreamWriter out) {
		this.out = out;
	}

	/** newLine followed by copies of indent. */
	private char[] linePrefix = null;

	@Override
	public NamespaceContext getNamespaceContext() {
		return out.getNamespaceContext();
	}

	@Override
	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		out.setNamespaceContext(context);
	}

	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		out.setDefaultNamespace(uri);
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		out.writeDefaultNamespace(namespaceURI);
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		out.writeNamespace(prefix, namespaceURI);
	}

	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		return out.getPrefix(uri);
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		out.setPrefix(prefix, uri);
	}

	@Override
	public void writeAttribute(String localName, String value) throws XMLStreamException {
		out.writeAttribute(localName, value);
	}

	@Override
	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		out.writeAttribute(namespaceURI, localName, value);
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
		out.writeAttribute(prefix, namespaceURI, localName, value);
	}

	@Override
	public void flush() throws XMLStreamException {
		out.flush();
	}

	@Override
	public void close() throws XMLStreamException {
		out.close();
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		beforeMarkup();
		out.writeStartDocument();
		afterMarkup();
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		beforeMarkup();
		out.writeStartDocument(version);
		afterMarkup();
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		beforeMarkup();
		out.writeStartDocument(encoding, version);
		afterMarkup();
	}

	@Override
	public void writeDTD(String dtd) throws XMLStreamException {
		beforeMarkup();
		out.writeDTD(dtd);
		afterMarkup();
	}

	@Override
	public void writeProcessingInstruction(String target) throws XMLStreamException {
		beforeMarkup();
		out.writeProcessingInstruction(target);
		afterMarkup();
	}

	@Override
	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		beforeMarkup();
		out.writeProcessingInstruction(target, data);
		afterMarkup();
	}

	@Override
	public void writeComment(String data) throws XMLStreamException {
		beforeMarkup();
		out.writeComment(data);
		afterMarkup();
	}

	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		beforeMarkup();
		out.writeEmptyElement(localName);
		afterMarkup();
	}

	@Override
	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		beforeMarkup();
		out.writeEmptyElement(namespaceURI, localName);
		afterMarkup();
	}

	@Override
	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		beforeMarkup();
		out.writeEmptyElement(prefix, localName, namespaceURI);
		afterMarkup();
	}

	@Override
	public void writeStartElement(String localName) throws XMLStreamException {
		beforeStartElement();
		out.writeStartElement(localName);
		afterStartElement();
	}

	@Override
	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		beforeStartElement();
		out.writeStartElement(namespaceURI, localName);
		afterStartElement();
	}

	@Override
	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		beforeStartElement();
		out.writeStartElement(prefix, localName, namespaceURI);
		afterStartElement();
	}

	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		out.writeCharacters(text);
		afterData();
	}

	@Override
	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		out.writeCharacters(text, start, len);
		afterData();
	}

	@Override
	public void writeCData(String data) throws XMLStreamException {
		out.writeCData(data);
		afterData();
	}

	@Override
	public void writeEntityRef(String name) throws XMLStreamException {
		out.writeEntityRef(name);
		afterData();
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		beforeEndElement();
		out.writeEndElement();
		afterEndElement();
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		try {
			while (depth > 0) {
				writeEndElement(); // indented
			}
		} catch (@SuppressWarnings("unused") Exception e) { // ok
		}
		out.writeEndDocument();
		afterEndDocument();
	}

	/** Prepare to write markup, by writing a new line and indentation. */
	protected final void beforeMarkup() {
		int soFar = stack[depth];
		if ((soFar & WROTE_DATA) != 0 || (depth > 0 && soFar != 0)) // not the first line
			return; // no data in this scope or not

		try {
			writeNewLine(depth);
			if (depth > 0) {
				afterMarkup(); // indentation was written
			}
		} catch (@SuppressWarnings("unused") Exception e) { // ok
		}
	}

	/** Note that markup or indentation was written. */
	protected void afterMarkup() {
		stack[depth] |= WROTE_MARKUP;
	}

	/** Note that data were written. */
	protected void afterData() {
		stack[depth] |= WROTE_DATA;
	}

	/** Prepare to start an element, by allocating stack space. */
	protected void beforeStartElement() {
		beforeMarkup();
		if (stack.length <= depth + 1) {
			// Allocate more space for the stack:
			int[] newStack = new int[stack.length * 2];
			System.arraycopy(stack, 0, newStack, 0, stack.length);
			stack = newStack;
		}
		stack[depth + 1] = 0; // nothing written yet
	}

	/** Note that an element was started. */
	protected void afterStartElement() {
		afterMarkup();
		++depth;
	}

	/** Prepare to end an element, by writing a new line and indentation. */
	protected void beforeEndElement() {
		if (depth > 0 && stack[depth] == WROTE_MARKUP) { // but not data
			try {
				writeNewLine(depth - 1);
			} catch (Exception ignored) { // ok
			}
		}
	}

	/** Note that an element was ended. */
	protected void afterEndElement() {
		if (depth > 0) {
			--depth;
		}
	}

	/** Note that a document was ended. 
	 * @throws XMLStreamException */
	protected void afterEndDocument() throws XMLStreamException {
		if (stack[depth = 0] == WROTE_MARKUP) { // but not data
			writeNewLine(0);
		}
		stack[depth] = 0; // start fresh
	}

	/** Write a line separator followed by indentation. */
	protected void writeNewLine(int indentation) throws XMLStreamException {
		final int newLineLength = NL.length();
		final int prefixLength = newLineLength + (INDENT.length() * indentation);
		if (prefixLength > 0) {
			if (linePrefix == null) {
				linePrefix = (NL + INDENT).toCharArray();
			}
			while (prefixLength > linePrefix.length) {
				// make linePrefix longer:
				char[] newPrefix = new char[newLineLength + ((linePrefix.length - newLineLength) * 2)];
				System.arraycopy(linePrefix, 0, newPrefix, 0, linePrefix.length);
				System.arraycopy(linePrefix, newLineLength, newPrefix, linePrefix.length, linePrefix.length - newLineLength);
				linePrefix = newPrefix;
			}
			out.writeCharacters(linePrefix, 0, prefixLength);
		}
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		return out.getProperty(name);
	}
}
