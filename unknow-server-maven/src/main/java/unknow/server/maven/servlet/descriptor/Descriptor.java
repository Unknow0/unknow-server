/**
 * 
 */
package unknow.server.maven.servlet.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import unknow.server.maven.AbstractGeneratorMojo.TypeConsumer;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.servlet.impl.ServletCookieConfigImpl;
import unknow.server.servlet.utils.Resource;

/**
 * @author unknow
 */
public class Descriptor extends WithParams implements TypeConsumer {
	public static final List<Class<?>> LISTENERS = Arrays.asList(ServletContextListener.class, ServletContextAttributeListener.class, ServletRequestListener.class,
			ServletRequestAttributeListener.class, HttpSessionListener.class, HttpSessionAttributeListener.class, HttpSessionIdListener.class);

	public String name = "/";

	public final List<LD> listeners = new ArrayList<>();
	public final List<SD> servlets = new ArrayList<>();
	public final List<SD> filters = new ArrayList<>();
	public final Map<String, Resource> resources = new HashMap<>();

	public final Map<String, String> errorClass = new HashMap<>();
	public final Map<Integer, String> errorCode = new HashMap<>();

	public final Map<String, String> localeMapping = new HashMap<>();
	public final Map<String, String> mimeTypes = new HashMap<>();

	public String trackingMode;
	public int sessionTimeout = 60;

	public final ServletCookieConfigImpl cookieConfig = new ServletCookieConfigImpl();

	public final List<String> initializer = new ArrayList<>();

	/**
	 * create new Descriptor
	 */
	public Descriptor() {
		mimeTypes.put("aac", "audio/aac");
		mimeTypes.put("avi", "video/x-msvideo");
		mimeTypes.put("bmp", "image/bmp");
		mimeTypes.put("bz", "application/x-bzip");
		mimeTypes.put("bz2", "application/x-bzip2");
		mimeTypes.put("css", "text/css");
		mimeTypes.put("csv", "text/csc");
		mimeTypes.put("gif", "image/gif");
		mimeTypes.put("htm", "text/html");
		mimeTypes.put("html", "text/html");
		mimeTypes.put("ico", "image/x-icon");
		mimeTypes.put("ics", "text/calendar");
		mimeTypes.put("jar", "application/java-archive");
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("jpeg", "image/jpeg");
		mimeTypes.put("js", "application/javascript");
		mimeTypes.put("json", "application/json");
		mimeTypes.put("mid", "audio/midi");
		mimeTypes.put("midi", "audio/midi");
		mimeTypes.put("mpeg", "video/mpeg");
		mimeTypes.put("oga", "audio/ogg");
		mimeTypes.put("ogv", "video/ogg");
		mimeTypes.put("ogx", "application/ogg");
		mimeTypes.put("otf", "font/otf");
		mimeTypes.put("png", "image/png");
		mimeTypes.put("pdf", "application/pdf");
		mimeTypes.put("rar", "application/x-rar-compressed");
		mimeTypes.put("rtf", "application/rtf");
		mimeTypes.put("sh", "application/x-sh");
		mimeTypes.put("svg", "image/svg+xml");
		mimeTypes.put("tar", "application/x-tar");
		mimeTypes.put("tif", "image/tiff");
		mimeTypes.put("tiff", "image/tiff");
		mimeTypes.put("ts", "application/typescript");
		mimeTypes.put("ttf", "font/ttf");
		mimeTypes.put("wav", "audio/x-wav");
		mimeTypes.put("weba", "audio/webm");
		mimeTypes.put("webm", "video/webm");
		mimeTypes.put("webp", "image/webp");
		mimeTypes.put("woff", "font/woff");
		mimeTypes.put("woff2", "font/woff2");
		mimeTypes.put("xhtml", "application/xhtml+xml");
		mimeTypes.put("xml", "application/xml");
		mimeTypes.put("zip", "application/zip");
		mimeTypes.put("7z", "application/x-7z-compressed");
	}

	@Override
	public void accept(TypeModel t) {
		Optional<AnnotationModel> o = t.annotation(WebServlet.class);
		if (o.isPresent()) {
			SD sd = new SD(servlets.size(), o.get(), t);
			servlets.add(sd);
		}
		o = t.annotation(WebFilter.class);
		if (o.isPresent()) {
			SD sd = new SD(filters.size(), o.get(), t);
			if (sd.dispatcher.isEmpty())
				sd.dispatcher.add(DispatcherType.REQUEST);
			filters.add(sd);
		}
		o = t.annotation(WebListener.class);
		if (o.isPresent())
			processListener(t);
	}

	private void processListener(TypeModel t) {
		Set<Class<?>> listener = new HashSet<>();
		for (Class<?> cl : LISTENERS) {
			if (t.isAssignableTo(cl))
				listener.add(cl);
		}
		listeners.add(new LD(t.name(), listener));
	}

	public List<SD> findFilters(String path, DispatcherType type) {
		List<SD> matching = new ArrayList<>();
		for (SD f : filters) {
			if (!f.dispatcher.contains(type))
				continue;
			for (String p : f.pattern) {
				if (p.endsWith("/*") && (p.length() == 2 || path.startsWith(p.substring(0, p.length() - 2)))) {
					matching.add(f);
					break;
				} else if (p.startsWith("*") && path.endsWith(p.substring(1))) {
					matching.add(f);
					break;
				} else if (path.equals(p)) {
					matching.add(f);
					break;
				}
			}
		}
		return matching;
	}

	public SD findServlet(String path) {
		int l = 0;
		SD best = null;
		SD def = null;
		for (SD s : servlets) {
			for (String p : s.pattern) {
				if (p.endsWith("/*") && path.startsWith(p.substring(0, p.length() - 2)) && l < p.length() - 2) {
					best = s;
					l = p.length() - 2;
				} else if (p.startsWith("*") && path.endsWith(p.substring(1)) && l == 0)
					best = s;
				else if (path.equals(p))
					return s;
				else if (p.equals("/"))
					def = s;
			}
		}
		return best == null ? def : best;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name:\t").append(name).append("\n");
		sb.append("servlets:\t").append(servlets).append("\n");
		sb.append("filters:\t").append(filters).append("\n");
		sb.append("errorClass:\t").append(errorClass).append("\n");
		sb.append("errorCodes:\t").append(errorCode).append("\n");
		return sb.toString();
	}
}
