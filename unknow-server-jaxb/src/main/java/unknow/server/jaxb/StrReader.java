/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author unknow
 */
public class StrReader {
	private static final ThreadLocal<StrReader> txt = new ThreadLocal<>() {
		protected StrReader initialValue() {
			return new StrReader();
		};
	};

	private final char[] buf = new char[1024];
	private final StringBuilder sb = new StringBuilder();

	public static final String read(XMLStreamReader r) throws XMLStreamException {
		return txt.get().doRead(r);
	}

	private StrReader() {
	}

	private String doRead(XMLStreamReader r) throws XMLStreamException {
		do {
			int len = r.getTextLength();
			int off = 0;
			while (off < len) {
				int l = r.getTextCharacters(off, buf, 0, buf.length);
				sb.append(buf, 0, l);
				off += l;
			}
		} while (r.hasNext() && r.next() == XMLStreamConstants.CHARACTERS);

		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
}
