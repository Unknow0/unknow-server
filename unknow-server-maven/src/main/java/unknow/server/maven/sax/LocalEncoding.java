/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class LocalEncoding extends Handler {
	private String locale;
	private String encoding;

	public LocalEncoding(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("locale".equals(localName))
			locale = lastContent().replace('_', '-');
		else if ("encoding".equals(localName))
			encoding = lastContent();
		else if ("locale-encoding-mapping".equals(localName)) {
			ctx.descriptor.localeMapping.put(locale, encoding);
			ctx.reader.setContentHandler(previous);
		}
	}
}
