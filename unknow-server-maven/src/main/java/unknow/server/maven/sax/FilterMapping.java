/**
 * 
 */
package unknow.server.maven.sax;

import javax.servlet.DispatcherType;

import org.xml.sax.SAXException;

import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class FilterMapping extends Handler {
	private SD filter;

	public FilterMapping(HandlerContext ctx) {
		super(ctx);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("filter-name".equals(localName)) {
			String n = lastContent();
			for (SD f : ctx.descriptor.filters) {
				if (f.name.equals(n)) {
					filter = f;
					return;
				}
			}
			throw new SAXException("no filter '" + n + "' found");
		} else if ("servlet-name".equals(localName))
			filter.servletNames.add(lastContent());
		else if ("url-pattern".equals(localName))
			filter.pattern.add(lastContent());
		else if ("dispatcher".equals(localName))
			filter.dispatcher.add(DispatcherType.valueOf(lastContent()));
		else if ("filter-mapping".equals(localName))
			ctx.reader.setContentHandler(previous);
	}
}
