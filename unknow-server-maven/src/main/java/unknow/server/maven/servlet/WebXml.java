package unknow.server.maven.servlet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.servlet.DispatcherType;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.LD;
import unknow.server.maven.servlet.descriptor.SD;
import unknow.server.maven.servlet.descriptor.WithParams;

public class WebXml {
	private WebXml() {
	}

	public static void parse(ModelLoader loader, Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				return;
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			switch (r.getLocalName()) {
				case "web-app":
					parseWebApp(loader, descriptor, r);
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void skipTag(XMLStreamReader r) throws XMLStreamException {
		int d = 1;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT && --d == 0)
				break;
			if (n == XMLStreamConstants.START_ELEMENT)
				d++;
		}
	}

	private static String parseContent(XMLStreamReader r) throws XMLStreamException {
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[1024];
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				break;
			if (n != XMLStreamConstants.CHARACTERS)
				continue;

			int len = r.getTextLength();
			int off = 0;
			while (off < len) {
				int l = r.getTextCharacters(off, buf, 0, buf.length);
				sb.append(buf, 0, l);
				off += l;
			}
		}
		return sb.toString();
	}

	private static void parseWebApp(ModelLoader loader, Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				return;
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			switch (r.getLocalName()) {
				case "context-param":
					parseParam(descriptor, r);
					break;
				case "display-name":
					descriptor.name = parseContent(r);
					break;
				case "error-page":
					parseErrorPage(descriptor, r);
					break;
				case "filter":
					parseFilters(descriptor, r);
					break;
				case "filter-mapping":
					parseFilterMapping(descriptor, r);
					break;
				case "listener":
					parseListener(loader, descriptor, r);
					break;
				case "locale-encoding-mapping":
					parseLocale(descriptor, r);
					break;
				case "mime-mapping":
					parseMime(descriptor, r);
					break;
				case "servlet":
					parseServlet(descriptor, r);
					break;
				case "servlet-mapping":
					parseServletMapping(descriptor, r);
					break;
				case "session-config":
					parseSession(descriptor, r);
					break;
				case "welcome-file-list":
				default:
					skipTag(r);
			}
		}
	}

	private static void parseParam(WithParams descriptor, XMLStreamReader r) throws XMLStreamException {
		String key = null;
		String value = null;
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				descriptor.param.put(key, value);
				return;
			}

			if (n != XMLStreamConstants.START_ELEMENT)
				continue;
			switch (r.getLocalName()) {
				case "param-name":
					key = parseContent(r);
					break;
				case "param-value":
					value = parseContent(r);
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseErrorPage(Descriptor descriptor, XMLStreamReader r) throws NumberFormatException, XMLStreamException {
		int code = -1;
		String clazz = null;
		String location = null;

		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				if (code > 0)
					descriptor.errorCode.put(code, location);
				if (clazz != null)
					descriptor.errorClass.put(clazz, location);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;
			switch (r.getLocalName()) {
				case "error-code":
					code = Integer.parseInt(parseContent(r));
					break;
				case "exception-type":
					clazz = parseContent(r);
					break;
				case "location":
					location = parseContent(r);
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseFilters(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		SD f = new SD(descriptor.filters.size());
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				descriptor.filters.add(f);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;
			switch (r.getLocalName()) {
				case "init-param":
					parseParam(f, r);
					break;
				case "filter-name":
					f.name = parseContent(r);
					break;
				case "filter-class":
					f.clazz = parseContent(r);
					break;
				case "enabled":
					f.enabled = Boolean.parseBoolean(parseContent(r));
					break;
				default:
					skipTag(r);
			}

		}
	}

	private static void parseFilterMapping(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		SD filter = null;
		List<String> servletNames = new ArrayList<>();
		List<String> urls = new ArrayList<>();
		List<DispatcherType> dispatchers = new ArrayList<>();
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				if (dispatchers.isEmpty())
					dispatchers.add(DispatcherType.REQUEST);
				if (filter == null)
					throw new XMLStreamException("Missing filter-name");
				filter.pattern.addAll(urls);
				filter.dispatcher.addAll(dispatchers);
				filter.servletNames.addAll(servletNames);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;
			s: switch (r.getLocalName()) {
				case "filter-name":
					String name = parseContent(r);
					for (SD f : descriptor.filters) {
						if (f.name.equals(name)) {
							filter = f;
							break s;
						}
					}
					throw new XMLStreamException("no filter named '" + n + "' found");
				case "servlet-name":
					servletNames.add(parseContent(r));
					break;
				case "url-pattern":
					urls.add(parseContent(r));
					break;
				case "dispatcher":
					dispatchers.add(DispatcherType.valueOf(parseContent(r)));
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseListener(ModelLoader loader, Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				return;
			if (n != XMLStreamConstants.START_ELEMENT || !"listener-class".equals(r.getLocalName()))
				continue;
			ClassModel type = loader.get(parseContent(r)).asClass();
			Set<Class<?>> impl = new HashSet<>();
			for (Class<?> c : Descriptor.LISTENERS) {
				if (loader.get(c.getName()).isAssignableFrom(type)) {
					impl.add(c);
					break;
				}
			}
			if (!impl.isEmpty())
				descriptor.listeners.add(new LD(type.name(), impl));
		}
	}

	private static void parseLocale(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		String locale = null;
		String encoding = null;

		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				descriptor.localeMapping.put(locale, encoding);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			String name = r.getLocalName();
			if ("locale".equals(name))
				locale = parseContent(r).replace('_', '-');
			else if ("encoding".equals(name))
				encoding = parseContent(r);
			else
				skipTag(r);
		}
	}

	private static void parseMime(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		String ext = null;
		String mime = null;

		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				descriptor.mimeTypes.put(ext, mime);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			String name = r.getLocalName();
			if ("extension".equals(name))
				ext = parseContent(r).replace('_', '-');
			else if ("mime-type".equals(name))
				mime = parseContent(r);
			else
				skipTag(r);
		}
	}

	private static void parseServlet(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		SD s = new SD(descriptor.servlets.size());

		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				descriptor.servlets.add(s);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			switch (r.getLocalName()) {
				case "init-param":
					parseParam(s, r);
					break;
				case "servlet-name":
					s.name = parseContent(r);
					break;
				case "servlet-class":
					s.clazz = parseContent(r);
					break;
				case "enabled":
					s.enabled = Boolean.parseBoolean(parseContent(r));
					break;
				case "jsp-file":
					s.jsp = parseContent(r);
					break;
				case "load-on-startup":
					s.loadOnStartup = Integer.parseInt(parseContent(r));
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseServletMapping(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		SD servlet = null;
		List<String> urls = new ArrayList<>();
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT) {
				if (servlet == null)
					throw new XMLStreamException("missing servlet-mapping");
				servlet.pattern.addAll(urls);
				return;
			}
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			s: switch (r.getLocalName()) {
				case "servlet-name":
					String name = parseContent(r);
					for (SD f : descriptor.servlets) {
						if (f.name.equals(name)) {
							servlet = f;
							break s;
						}
					}
					throw new XMLStreamException("no servlet named '" + n + "' found");
				case "url-pattern":
					urls.add(parseContent(r));
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseSession(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				return;
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			switch (r.getLocalName()) {
				case "cookie-config":
					parseCookie(descriptor, r);
					break;
				case "tracking-mode":
					descriptor.trackingMode = parseContent(r);
					break;
				case "session-timeout":
					descriptor.sessionTimeout = Integer.parseInt(parseContent(r));
					break;
				default:
					skipTag(r);
			}
		}
	}

	private static void parseCookie(Descriptor descriptor, XMLStreamReader r) throws XMLStreamException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.END_ELEMENT)
				return;
			if (n != XMLStreamConstants.START_ELEMENT)
				continue;

			switch (r.getLocalName()) {
				case "name":
					descriptor.cookieConfig.setName(parseContent(r));
					break;
				case "domain":
					descriptor.cookieConfig.setDomain(parseContent(r));
					break;
				case "path":
					descriptor.cookieConfig.setPath(parseContent(r));
					break;
				case "max-age":
					descriptor.cookieConfig.setMaxAge(Integer.parseInt(parseContent(r)));
					break;
				case "secure":
					descriptor.cookieConfig.setSecure(Boolean.parseBoolean(parseContent(r)));
					break;
				case "http-only":
					descriptor.cookieConfig.setHttpOnly(Boolean.parseBoolean(parseContent(r)));
					break;
				default:
					skipTag(r);
			}
		}
	}

}
