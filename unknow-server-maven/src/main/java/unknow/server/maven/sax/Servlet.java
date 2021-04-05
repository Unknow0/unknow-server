/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.SAXException;

import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class Servlet extends Handler {
	private InitParam initParam;
	private final SD servlet;

	public Servlet(HandlerContext ctx) {
		super(ctx);
		servlet = new SD(ctx.descriptor.filters.size());
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("servlet-name".equals(localName))
			servlet.name = lastContent();
		else if ("servlet-class".equals(localName))
			servlet.clazz = lastContent();
		else if ("jsp-file".equals(localName))
			servlet.jsp = lastContent();
		else if ("init-param".equals(localName))
			ctx.reader.setContentHandler(initParam == null ? initParam = new InitParam(ctx, servlet) : initParam);
		else if ("load-on-startup".equals(localName))
			servlet.loadOnStartup = Integer.parseInt(lastContent());
		else if ("servlet".equals(localName)) {
			ctx.descriptor.servlets.add(servlet);
			ctx.reader.setContentHandler(previous);
		}
	}
}
