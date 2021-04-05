/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author unknow
 */
public class WebApp extends Handler {

	public WebApp(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if ("context-param".equals(localName))
			ctx.reader.setContentHandler(new ContextParam(ctx));
		else if ("display-name".equals(localName))
			ctx.reader.setContentHandler(new DisplayName(ctx));
		else if ("filter".equals(localName))
			ctx.reader.setContentHandler(new Filter(ctx));
		else if ("filter-mapping".equals(localName))
			ctx.reader.setContentHandler(new FilterMapping(ctx));

		else if ("servlet".equals(localName))
			ctx.reader.setContentHandler(new Servlet(ctx));
	}
}
