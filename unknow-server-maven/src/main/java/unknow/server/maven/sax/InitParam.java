/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class InitParam extends Handler {
	private final SD sd;
	private String key;
	private String value;

	public InitParam(HandlerContext ctx, SD sd) {
		super(ctx);
		this.sd = sd;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("param-name".equals(localName))
			key = lastContent();
		else if ("param-value".equals(localName))
			value = lastContent();
		else if ("init-param".equals(localName)) {
			sd.param.put(key, value);
			key = value = null;
			ctx.reader.setContentHandler(previous);
		}
	}
}
