/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class ContextParam extends Handler {
	public String key;
	public String value;

	public ContextParam(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("param-name".equals(localName))
			key = lastContent();
		else if ("param-value".equals(localName))
			value = lastContent();
		else if ("context-param".equals(localName)) {
			ctx.descriptor.param.put(key, value);
			ctx.reader.setContentHandler(previous);
		}
	}
}
