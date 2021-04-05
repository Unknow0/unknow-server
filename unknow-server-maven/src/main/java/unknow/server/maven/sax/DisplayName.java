/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class DisplayName extends Handler {
	public DisplayName(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		ctx.descriptor.name = lastContent();
		ctx.reader.setContentHandler(previous);
	}
}
