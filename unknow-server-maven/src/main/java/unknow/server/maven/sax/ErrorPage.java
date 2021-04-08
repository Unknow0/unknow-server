/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class ErrorPage extends Handler {
	private int code = -1;
	private String clazz;
	private String location;

	public ErrorPage(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("error-code".equals(localName))
			code = Integer.parseInt(lastContent());
		else if ("exception-type".equals(localName))
			clazz = lastContent();
		else if ("location".equals(localName))
			location = lastContent();
		else if ("error-page".equals(localName)) {
			if (code > 0)
				ctx.descriptor.errorCode.put(code, location);
			if (clazz != null)
				ctx.descriptor.errorClass.put(clazz, location);
			code = -1;
			clazz = null;
			ctx.reader.setContentHandler(previous);
		}
	}
}
