/**
 * 
 */
package unknow.server.maven.servlet.sax;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

import unknow.sax.SaxHandler;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.LD;
import unknow.server.maven.servlet.descriptor.SD;

/**
 * @author unknow
 */
public class WebAppHandler implements SaxHandler<Context> {
	public static final WebAppHandler INSTANCE = new WebAppHandler();

	private static final SaxHandler<Context> CONTEXT_PARAM = new SaxHandler<Context>() {
		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("context-param".equals(name))
				context.push(new KV());
		}

		@Override
		public void endElement(String uri, String name, Context context) {
			if ("param-name".equals(name))
				((KV) context.peek()).key = context.textContent();
			else if ("param-value".equals(name))
				((KV) context.peek()).value = context.textContent();
			else if ("context-param".equals(name)) {
				KV kv = (KV) context.pop();
				context.descriptor.param.put(kv.key, kv.value);
				context.previous();
			}
		}
	};
	private static final SaxHandler<Context> INIT_PARAM = new SaxHandler<Context>() {
		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("init-param".equals(name))
				context.push(new KV());
		}

		@Override
		public void endElement(String uri, String name, Context context) {
			if ("param-name".equals(name))
				((KV) context.peek()).key = context.textContent();
			else if ("param-value".equals(name))
				((KV) context.peek()).value = context.textContent();
			else if ("init-param".equals(name)) {
				KV kv = (KV) context.pop();
				((SD) context.peek()).param.put(kv.key, kv.value);
				context.previous();
			}
		}
	};

	private static final SaxHandler<Context> DISPLAY = new SaxHandler<Context>() {
		@Override
		public void endElement(String uri, String name, Context context) {
			context.descriptor.name = context.textContent();
		}
	};

	private static final SaxHandler<Context> ERROR_PAGE = new SaxHandler<Context>() {
		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("error-page".equals(name))
				context.push(new ErrorPage());
		}

		@Override
		public void endElement(String uri, String name, Context context) {
			if ("error-code".equals(name))
				((ErrorPage) context.peek()).code = Integer.parseInt(context.textContent());
			else if ("exception-type".equals(name))
				((ErrorPage) context.peek()).clazz = context.textContent();
			else if ("location".equals(name))
				((ErrorPage) context.peek()).location = context.textContent();
			else if ("error-page".equals(name)) {
				ErrorPage e = (ErrorPage) context.pop();
				if (e.code > 0)
					context.descriptor.errorCode.put(e.code, e.location);
				if (e.clazz != null)
					context.descriptor.errorClass.put(e.clazz, e.location);
				context.previous();
			}
		}
	};

	private static final SaxHandler<Context> FILTER = new SaxHandler<Context>() {
		@Override
		public void startElement(String ns, String name, Context context) {
			if ("init-param".equals(name))
				context.next(INIT_PARAM);
		}

		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("filter".equals(name))
				context.push(new SD(context.descriptor.filters.size()));
		}

		@Override
		public void endElement(String uri, String name, Context context) {
			if ("filter-name".equals(name))
				((SD) context.peek()).name = context.textContent();
			else if ("filter-class".equals(name))
				((SD) context.peek()).clazz = context.textContent();
			else if ("enabled".equals(name))
				((SD) context.peek()).enabled = Boolean.parseBoolean(context.textContent());
			else if ("filter".equals(name)) {
				SD sd = (SD) context.pop();
				context.descriptor.filters.add(sd);
				context.previous();
			}
		}
	};

	private static final SaxHandler<Context> FILTER_MAPPING = new SaxHandler<Context>() {
		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("filter-name".equals(name)) {
				String n = context.textContent();
				for (SD f : context.descriptor.filters) {
					if (f.name.equals(n)) {
						context.push(f);
						return;
					}
				}
				throw new SAXException("no filter '" + n + "' found");
			}
			if ("servlet-name".equals(name))
				((SD) context.peek()).servletNames.add(context.textContent());
			else if ("url-pattern".equals(name))
				((SD) context.peek()).pattern.add(context.textContent());
			else if ("dispatcher".equals(name))
				((SD) context.peek()).dispatcher.add(DispatcherType.valueOf(context.textContent()));
			else if ("filter-mapping".equals(name))
				context.previous();
		}
	};

	private static final SaxHandler<Context> LISTENER = new SaxHandler<Context>() {
		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("listener-class".equals(name)) {
				ResolvedReferenceTypeDeclaration descl = context.resolver.tryToSolveType(context.textContent()).getCorrespondingDeclaration();
				Set<Class<?>> impl = new HashSet<>();
				for (ResolvedReferenceType a : descl.getAllAncestors()) {
					String n = a.getQualifiedName();
					for (Class<?> c : Descriptor.LISTENERS) {
						if (n.equals(c.getName())) {
							impl.add(c);
							break;
						}
					}
				}
				if (!impl.isEmpty())
					context.descriptor.listeners.add(new LD(context.textContent(), impl));
			} else if ("listener".equals(name)) {
				context.previous();
			}
		}
	};

	private static final SaxHandler<Context> LOCAL_ENCODING = new SaxHandler<Context>() {
		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("locale-encoding-mapping".equals(name))
				context.push(new KV());
		}

		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("locale".equals(name))
				((KV) context.peek()).key = context.textContent().replace('_', '-');
			else if ("encoding".equals(name))
				((KV) context.peek()).value = context.textContent();
			else if ("locale-encoding-mapping".equals(name)) {
				KV kv = (KV) context.pop();
				context.descriptor.localeMapping.put(kv.key, kv.value);
				context.previous();
			}
		}
	};
	private static final SaxHandler<Context> MIMETYPE = new SaxHandler<Context>() {
		@Override
		public void attributes(String uri, String name, Attributes atts, Context context) {
			if ("mime-mapping".equals(name))
				context.push(new KV());
		}

		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("extension".equals(name))
				((KV) context.peek()).key = context.textContent().replace('_', '-');
			else if ("mime-type".equals(name))
				((KV) context.peek()).value = context.textContent();
			else if ("mime-mapping".equals(name)) {
				KV kv = (KV) context.pop();
				context.descriptor.mimeTypes.put(kv.key, kv.value);
				context.previous();
			}
		}
	};

	private static final SaxHandler<Context> SERVLET = new SaxHandler<Context>() {
		@Override
		public void startElement(String ns, String name, Context context) {
			if ("init-param".equals(name))
				context.next(INIT_PARAM);
		}

		@Override
		public void endElement(String uri, String name, Context context) {
			if ("servlet-name".equals(name))
				((SD) context.peek()).name = context.textContent();
			else if ("servlet-class".equals(name))
				((SD) context.peek()).clazz = context.textContent();
			else if ("enabled".equals(name))
				((SD) context.peek()).enabled = Boolean.parseBoolean(context.textContent());
			else if ("jsp-file".equals(name))
				((SD) context.peek()).jsp = context.textContent();
			else if ("load-on-startup".equals(name))
				((SD) context.peek()).loadOnStartup = Integer.parseInt(context.textContent());
			else if ("servlet".equals(name)) {
				context.descriptor.servlets.add((SD) context.pop());
				context.previous();
			}
		}
	};
	private static final SaxHandler<Context> SERVLET_MAPPING = new SaxHandler<Context>() {
		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("servet-name".equals(name)) {
				String n = context.textContent();
				for (SD f : context.descriptor.servlets) {
					if (f.name.equals(n)) {
						context.push(f);
						return;
					}
				}
				throw new SAXException("no servlet '" + n + "' found");
			} else if ("url-pattern".equals(name))
				((SD) context.peek()).pattern.add(context.textContent());
			else if ("servlet-mapping".equals(name))
				context.previous();
		}
	};

	private static final SaxHandler<Context> SESSION_CONFIG = new SaxHandler<Context>() {
		@Override
		public void startElement(String ns, String name, Context context) {
			if ("cookie-config".equals(name))
				context.next(COOKIE_CONFIG);
		}

		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("tracking-mode".equals(name))
				context.descriptor.trackingMode = context.textContent();
			else if ("session-timeout".equals(name))
				context.descriptor.sessionTimeout = Integer.parseInt(context.textContent());
			if ("session-config".equals(name))
				context.previous();
		}
	};

	private static final SaxHandler<Context> COOKIE_CONFIG = new SaxHandler<Context>() {
		@Override
		public void endElement(String uri, String name, Context context) throws SAXException {
			if ("name".equals(name))
				context.descriptor.cookieConfig.setName(context.textContent());
			else if ("comment".equals(name))
				context.descriptor.cookieConfig.setComment(context.textContent());
			else if ("domain".equals(name))
				context.descriptor.cookieConfig.setDomain(context.textContent());
			else if ("path".equals(name))
				context.descriptor.cookieConfig.setPath(context.textContent());
			else if ("max-age".equals(name))
				context.descriptor.cookieConfig.setMaxAge(Integer.parseInt(context.textContent()));
			else if ("secure".equals(name))
				context.descriptor.cookieConfig.setSecure(Boolean.parseBoolean(context.textContent()));
			else if ("http-only".equals(name))
				context.descriptor.cookieConfig.setHttpOnly(Boolean.parseBoolean(context.textContent()));
			if ("cookie-config".equals(name))
				context.previous();
		}
	};

	private WebAppHandler() {
	}

	@Override
	public void startElement(String ns, String name, Context context) {
		if ("context-param".equals(name))
			context.next(CONTEXT_PARAM);
		else if ("display-name".equals(name))
			context.next(DISPLAY);
		else if ("error-page".equals(name))
			context.next(ERROR_PAGE);
		else if ("filter".equals(name)) {
			context.push(new SD(context.descriptor.filters.size()));
			context.next(FILTER);
		} else if ("filter-mapping".equals(name))
			context.next(FILTER_MAPPING);
		else if ("listener".equals(name))
			context.next(LISTENER);
		else if ("locale-encoding-mapping".equals(name))
			context.next(LOCAL_ENCODING);
		else if ("mime-mapping".equals(name))
			context.next(MIMETYPE);
		else if ("servlet".equals(name)) {
			context.push(new SD(context.descriptor.servlets.size()));
			context.next(SERVLET);
		} else if ("servlet-mapping".equals(name))
			context.next(SERVLET_MAPPING);
		else if ("session-config".equals(name))
			context.next(SESSION_CONFIG);
		else if ("welcome-file-list".equals(name))
			;
	}

	private static class KV {
		public String key;
		public String value;
	}

	private static class ErrorPage {
		int code = -1;
		String clazz;
		String location;
	}
}
