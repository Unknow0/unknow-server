/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class MimeType extends Handler {
	private String ext;
	private String mime;

	public MimeType(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("extension".equals(localName))
			ext = lastContent();
		else if ("mime-type".equals(localName))
			mime = lastContent();
		else if ("mime-mapping".equals(localName)) {
			ctx.descriptor.mimeTypes.put(ext, mime);
			ctx.reader.setContentHandler(previous);
		}
	}
}
