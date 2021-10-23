/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author unknow
 */
public class XMLOutput implements XMLWriter {
	private static final int DOC = 0;
	private static final int START = 1;
	private static final int CONTENT = 2;

	private final Writer w;
	private final Map<String, String> ns;
	private int step;

	/**
	 * create new XMLOutput
	 * 
	 * @param w
	 * @param ns
	 */
	public XMLOutput(Writer w, Map<String, String> ns) {
		this.w = w;
		this.ns = ns;
		this.step = DOC;
	}

	@Override
	public void close() throws IOException {
		w.close();
	}

	@Override
	public void startElement(String name, String nsUri) throws IOException {
		if (step == START)
			w.write('>');
		w.write('<');
		String nsPrefix = getPrefix(nsUri);
		if (!nsPrefix.isEmpty())
			w.append(nsPrefix).write(':');
		w.write(name);
		if (step == DOC) {
			for (Entry<String, String> e : ns.entrySet()) {
				w.write(" xmlns");
				if (!e.getValue().isEmpty())
					w.append(':').write(e.getValue());
				w.write("=\"");
				encodeEntities(e.getKey().toCharArray());
				w.write('"');
			}
		}
		step = START;
	}

	@Override
	public void attribute(String name, String nsUri, String value) throws IOException {
		if (step != START)
			throw new IOException("can't write attribute on content");
		w.write(' ');
		if (nsUri != null && !nsUri.isEmpty()) {
			String nsPrefix = getPrefix(nsUri);
			if (!nsPrefix.isEmpty())
				w.append(nsPrefix).write(':');
		}
		// TODO encode value
		w.append(name).write("=\"");
		encodeEntities(value.toCharArray());
		w.write('"');
	}

	@Override
	public void text(String text) throws IOException {
		if (step == START)
			w.write('>');
		char[] data = text.toCharArray();
		if (cdata(data))
			w.append("<![CDATA[").append(text).append("]]>");
		else
			encodeEntities(data);
		step = CONTENT;
	}

	@Override
	public void endElement(String name, String nsUri) throws IOException {
		if (step == START) {
			w.write("/>");
			return;
		}
		w.write("</");
		String nsPrefix = getPrefix(nsUri);
		if (!nsPrefix.isEmpty())
			w.append(nsPrefix).write(':');
		w.append(name).write('>');
		step = CONTENT;
	}

	private void encodeEntities(char[] data) throws IOException {
		int e = 0;
		for (int i = 0; i < data.length; i++) {
			char c = data[i];
			if (c == '&' || c == '<' || c == '"') {
				w.write(data, e, i - e);
				if (c == '&')
					w.write("&amp;");
				else if (c == '<')
					w.write("&lt;");
				else
					w.write("&#34;");
				e = i + 1;
			}
		}
		if (e < data.length)
			w.write(data, e, data.length);
	}

	private String getPrefix(String nsUri) throws IOException {
		String nsPrefix = ns.get(nsUri);
		if (nsPrefix == null)
			throw new IOException("missing ns mapping for '" + nsUri + "'");
		return nsPrefix;
	}

	/**
	 * count the number of entity to encode
	 * 
	 * @param data
	 * @return
	 */
	private static boolean cdata(char[] data) {
		int e = 0;
		for (int i = 0; i < data.length; i++) {
			char c = data[i];
			if (c == '&' || c == '<' || c == '"') {
				if (++e == 4)
					return true;
			}
		}
		return false;
	}
}
