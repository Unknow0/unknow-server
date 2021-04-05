/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class Filter extends Handler {
	private InitParam initParam;
	private final SD filter;

	public Filter(HandlerContext ctx) {
		super(ctx);
		filter = new SD(ctx.descriptor.filters.size());
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement(uri, localName, qName, atts);
		if ("init-param".equals(localName))
			ctx.reader.setContentHandler(initParam == null ? initParam = new InitParam(ctx, filter) : initParam);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		System.out.println("Filter: '" + localName + "'");
		if ("filter-name".equals(localName))
			filter.name = lastContent();
		else if ("filter-class".equals(localName))
			filter.clazz = lastContent();
		else if ("filter".equals(localName)) {
			ctx.descriptor.filters.add(filter);
			ctx.reader.setContentHandler(previous);
		}
	}
}
