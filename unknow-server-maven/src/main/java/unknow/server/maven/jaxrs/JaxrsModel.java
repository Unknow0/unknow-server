/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsCookieParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsFormParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsHeaderParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsMatrixParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsPathParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsQueryParam;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.util.AncestrorIterator;
import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public class JaxrsModel {
	private static final Logger log = LoggerFactory.getLogger(JaxrsModel.class);
	private static final String[] ALL = { "*/*" };
	private static final Pattern p = Pattern.compile("(?<=/)\\{\\s*(\\w[\\w\\.\\-]*)\\s*(?::\\s*((?:[^\\{\\}]|\\{[^\\{\\}]*\\})*)\\s*)?\\}(?=/|$)");

	private static final Set<String> JAXRS_ANNOTATIONS = new HashSet<>(Arrays.asList(GET.class.getName(), POST.class.getName(), PUT.class.getName(), DELETE.class.getName(),
			PATCH.class.getName(), HEAD.class.getName(), OPTIONS.class.getName(), HttpMethod.class.getName()));

	private static final Set<String> JARXS_PARAM = new HashSet<>(Arrays.asList(BeanParam.class.getName(), PathParam.class.getName(), QueryParam.class.getName(),
			CookieParam.class.getName(), HeaderParam.class.getName(), FormParam.class.getName(), MatrixParam.class.getName()));

	private final List<JaxrsMapping> mappings = new ArrayList<>();
	// path, method
	private final Map<String, Map<String, List<JaxrsMapping>>> paths = new HashMap<>();
	private final ModelLoader loader;
	private final TypeModel paramProvider;
	private final TypeModel exceptionMapper;
	private final TypeModel bodyReader;
	private final TypeModel bodyWriter;
	private final TypeModel string;

	public final List<String> converter = new ArrayList<>();
	public final Map<String, List<String>> readers = new HashMap<>();
	public final Map<String, List<String>> writers = new HashMap<>();
	public final Map<TypeModel, ClassModel> exceptions = new HashMap<>();

	public final Set<String> implicitConstructor = new HashSet<>();
	public final Set<String> implicitFromString = new HashSet<>();
	public final Set<String> implicitValueOf = new HashSet<>();

	/**
	 * create new JaxrsModel
	 * 
	 * @param loader
	 */
	public JaxrsModel(ModelLoader loader) {
		this.loader = loader;
		this.paramProvider = loader.get(ParamConverterProvider.class.getName());
		this.exceptionMapper = loader.get(ExceptionMapper.class.getName());
		this.bodyReader = loader.get(MessageBodyReader.class.getName());
		this.bodyWriter = loader.get(MessageBodyWriter.class.getName());
		this.string = loader.get(String.class.getName());

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		loadService(cl, MessageBodyReader.class, c -> {
			Consumes a = c.getAnnotation(Consumes.class);
			String[] v = a == null ? ALL : a.value();
			List<String> list = new ArrayList<>();
			for (int i = 0; i < v.length; i++)
				list.addAll(Arrays.asList(v[i].split(" *, *")));
			readers.put(c.getName(), list);
		});
		loadService(cl, MessageBodyWriter.class, c -> {
			Produces a = c.getAnnotation(Produces.class);
			String[] v = a == null ? ALL : a.value();
			List<String> list = new ArrayList<>();
			for (int i = 0; i < v.length; i++)
				list.addAll(Arrays.asList(v[i].split(" *, *")));
			writers.put(c.getName(), list);
		});
	}

	private static void loadService(ClassLoader loader, Class<?> clazz, Consumer<Class<?>> v) {
		try {
			Enumeration<URL> e = loader.getResources("META-INF/services/" + clazz.getName());
			while (e.hasMoreElements()) {
				URL u = e.nextElement();
				log.error("	found {}", u);
				try {
					URLConnection uc = u.openConnection();
					uc.setUseCaches(false);
					try (InputStream in = uc.getInputStream(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
						String l;
						while ((l = r.readLine()) != null) {
							String c = parseLine(u, l);
							if (c == null)
								continue;
							try {
								v.accept(Class.forName(c));
							} catch (Exception e2) {
								log.warn("Failed to process service {}", clazz.getName(), e2);
							}
						}
					}
				} catch (IOException x) {
					log.error("Failed to read service {}", u, e);
				}
			}
		} catch (IOException e) {
			log.error("Failed to load service {}", clazz.getName(), e);
		}
	}

	private static String parseLine(URL url, String ln) {
		int ci = ln.indexOf('#');
		if (ci >= 0)
			ln = ln.substring(0, ci);
		ln = ln.trim();
		int n = ln.length();
		if (n == 0)
			return null;
		if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0)) {
			log.warn("Ignoring invalid line '{}' in {}", ln, url);
			return null;
		}
		int cp = ln.codePointAt(0);
		if (!Character.isJavaIdentifierStart(cp)) {
			log.warn("Ignoring invalid class name {} in {}", ln, url);
			return null;
		}
		int start = Character.charCount(cp);
		for (int i = start; i < n; i += Character.charCount(cp)) {
			cp = ln.codePointAt(i);
			if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
				log.warn("Ignoring invalid class name {} in {}", ln, url);
				return null;
			}
		}
		return ln;
	}

	public Set<String> paths() {
		return paths.keySet();
	}

	public List<JaxrsMapping> mappings() {
		return mappings;
	}

	public Set<String> methods(String path) {
		return paths.getOrDefault(path, Collections.emptyMap()).keySet();
	}

	public List<JaxrsMapping> mapping(String path, String method) {
		return paths.getOrDefault(path, Collections.emptyMap()).getOrDefault(method, null);
	}

	public void process(ClassModel clazz) {
		clazz.annotation(Provider.class).ifPresent(a -> {
			if (paramProvider.isAssignableFrom(clazz))
				converter.add(clazz.name());
			ClassModel e = clazz.ancestror(exceptionMapper);
			if (e != null) {
				if (exceptions.containsKey(e))
					log.error("Duplicate exception mapping for '" + e + "'");
				else
					exceptions.put(e.parameter(0).type(), clazz);
			}
			if (bodyReader.isAssignableFrom(clazz)) {
				List<String> list = new ArrayList<>();
				for (String s : clazz.annotation(Consumes.class).flatMap(v -> v.values()).orElse(ALL))
					list.addAll(Arrays.asList(s.trim().split(" *, *")));
				readers.put(clazz.name(), list);
			}
			if (bodyWriter.isAssignableFrom(clazz)) {
				List<String> list = new ArrayList<>();
				for (String s : clazz.annotation(Produces.class).flatMap(v -> v.values()).orElse(ALL))
					list.addAll(Arrays.asList(s.trim().split(" *, *")));
				writers.put(clazz.name(), list);
			}
			// TODO other
			// ContainerRequestFilter, ContainerResponseFilter, ReaderInterceptor, WriterInterceptor
			// ContextResolver<?>
		});

		Iterator<ClassModel> it = new AncestrorIterator(clazz);
		String basePath = null;
		String[] baseConsume = null;
		String[] baseProduce = null;
		String method = null;
		Map<String, MethodModel> methods = new HashMap<>();
		while (it.hasNext()) {
			ClassModel next = it.next();
			if (basePath == null)
				basePath = next.annotation(Path.class).flatMap(a -> a.value()).orElse(null);
			if (baseConsume == null)
				baseConsume = next.annotation(Consumes.class).flatMap(a -> a.values()).orElse(null);
			if (baseProduce == null)
				baseProduce = next.annotation(Produces.class).flatMap(a -> a.values()).orElse(null);
			if (method == null)
				method = getMethod(next, next.name());

			for (MethodModel m : next.methods()) {
				if (!m.isPublic() || m.isStatic() || !isMapped(m))
					continue;
				methods.putIfAbsent(m.signature(), m);
			}
		}
		if (basePath == null)
			basePath = "";
		if (basePath.endsWith("/"))
			basePath = basePath.substring(0, basePath.length() - 1);
		if (baseConsume == null)
			baseConsume = ALL;
		if (baseProduce == null)
			baseProduce = ALL;

		for (MethodModel m : methods.values())
			process(method, basePath, baseConsume, baseProduce, clazz, m);
	}

	private void process(String defaultMethod, String path, String[] consume, String[] produce, ClassModel clazz, MethodModel m) {
		Optional<AnnotationModel> a = m.annotation(Path.class);
		if (path == null && a.isEmpty())
			throw new RuntimeException("no path mapping found on " + clazz.name() + " " + m.signature());

		if (a.isPresent()) {
			String s = a.flatMap(v -> v.value()).orElse("");
			if (s.charAt(0) != '/')
				path += "/";
			path += s;
		}
		StringBuilder sb = new StringBuilder();
		List<PathPart> parts = parsePath(sb, path);
		String normalizedPath = sb.toString();
		sb.setLength(0);

		Map<String, List<JaxrsMapping>> map = paths.get(normalizedPath);
		if (map == null)
			paths.put(normalizedPath, map = new HashMap<>());

		String errorName = clazz.name() + "." + m.signature();
		String method = getMethod(m, errorName);
		if (method == null)
			method = defaultMethod;
		if (method == null)
			throw new RuntimeException("no method mapped on " + errorName);
		List<JaxrsParam> params = new ArrayList<>();
		for (ParamModel<MethodModel> p : m.parameters()) {
			List<AnnotationModel> l = p.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
			if (l.size() > 1)
				throw new RuntimeException("Duplicate parameter annotation on " + errorName + " " + p.name());
			JaxrsParam param = l.isEmpty() ? new JaxrsBodyParam(p) : buildParam(p, l.get(0));
			params.add(param);
		}

		List<PathPart> pp = Collections.emptyList();
		if (!parts.isEmpty()) {
			Set<String> set = new HashSet<>();
			pp = new ArrayList<>();
			for (PathPart p : parts) {
				if (!set.add(p.name))
					throw new RuntimeException("duplicate path pattern name " + p.name + " on " + m);
				pp.add(p);
			}
		}

		consume = m.annotation(Consumes.class).flatMap(v -> v.values()).orElse(consume);
		produce = m.annotation(Produces.class).flatMap(v -> v.values()).orElse(produce);

		List<JaxrsMapping> list = map.get(method);
		if (list == null)
			map.put(method, list = new ArrayList<>());

		for (JaxrsMapping j : list) {
			if (hasEq(j.consume, consume) && hasEq(j.produce, produce))
				throw new RuntimeException("Duplicate mapping for '" + j.m + "' and '" + m + "'");
		}

		JaxrsMapping jaxrsMapping = new JaxrsMapping("m$" + mappings.size(), clazz, m, method, params, path, pp, consume, produce);
		list.add(jaxrsMapping);
		mappings.add(jaxrsMapping);
	}

	private static MethodModel getSetter(ClassModel cl, String name, TypeModel type) {
		String set = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		return cl.method(set, type).orElseThrow(() -> new RuntimeException("Can't find setter " + set + "(" + type + ") in " + cl));
	}

	public static TypeModel getParamType(TypeModel type) {
		if (type.isArray())
			return type.asArray().type();
		if (!type.isClass())
			return type;

		Iterator<ClassModel> it = type.asClass().ancestor().iterator();
		while (it.hasNext()) {
			ClassModel next = it.next();
			if ("java.util.Collection".equals(next.name()))
				return next.parameter(0).type();
		}
		return type;
	}

	private <T extends WithName & WithAnnotation & WithType> JaxrsParam buildParam(T p, AnnotationModel a) {
		JaxrsParam param = _buildParam(p, a);
		if (!BeanParam.class.getName().equals(a.name())) {
			TypeModel type = getParamType(p.type());
			if (!type.isClass())
				return param;

			ClassModel cl = type.asClass();
			if (cl.isBoxedPrimitive())
				return param;
			if (cl.constructors(string).filter(c -> c.isPublic()).isPresent())
				implicitConstructor.add(type.name());
			else {
				if (cl.method("valueOf", string).filter(c -> c.isPublic() && c.isStatic()).isPresent())
					implicitValueOf.add(type.name());
				else if (cl.method("fromString", string).filter(c -> c.isPublic() && c.isStatic()).isPresent())
					implicitFromString.add(type.name());
				else if (cl.isEnum())
					implicitValueOf.add(type.name());
			}
		}
		return param;
	}

	private <T extends WithName & WithAnnotation & WithType> JaxrsParam _buildParam(T p, AnnotationModel a) {
		if (BeanParam.class.getName().equals(a.name())) {
			ClassModel cl = p.type().asClass();
			Map<FieldModel, JaxrsParam> fields = new HashMap<>();
			Map<MethodModel, JaxrsParam> setter = new HashMap<>();
			for (FieldModel f : cl.fields()) {
				List<AnnotationModel> l = f.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
				if (l.isEmpty())
					continue;
				if (l.size() > 1)
					throw new RuntimeException("Duplicate parameter annotation on " + f.parent() + "." + f.name());
				if (f.isPublic()) {
					fields.put(f, buildParam(f, l.get(0)));
					continue;
				}

				setter.put(getSetter(cl, f.name(), f.type()), buildParam(f, l.get(0)));
			}
			for (MethodModel m : cl.methods()) {
				if (setter.containsKey(m))
					continue;
				List<AnnotationModel> l = m.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
				if (l.isEmpty())
					continue;
				if (l.size() > 1)
					throw new RuntimeException("Duplicate parameter annotation on " + m.parent() + "." + m.name());
				String n = m.name();
				if (n.startsWith("get"))
					m = getSetter(cl, n.substring(3), m.type());
				else if (!n.startsWith("set"))
					m = getSetter(cl, n, m.type());

				setter.put(m, buildParam(m.parameter(0), l.get(0)));
			}

			return new JaxrsBeanParam(p, cl, fields, setter);
		}
		if (PathParam.class.getName().equals(a.name()))
			return new JaxrsPathParam(p, a.value().orElse(""));
		if (QueryParam.class.getName().equals(a.name()))
			return new JaxrsQueryParam(p, a.value().orElse(""));
		if (CookieParam.class.getName().equals(a.name()))
			return new JaxrsCookieParam(p, a.value().orElse(""));
		if (HeaderParam.class.getName().equals(a.name()))
			return new JaxrsHeaderParam(p, a.value().orElse(""));
		if (FormParam.class.getName().equals(a.name()))
			return new JaxrsFormParam(p, a.value().orElse(""));
		if (MatrixParam.class.getName().equals(a.name()))
			return new JaxrsMatrixParam(p, a.value().orElse(""));

		throw new RuntimeException("Unknow annotation " + a);
	}

	private String getMethod(WithAnnotation v, String name) {
		String m = null;
		for (AnnotationModel a : v.annotations()) {
			if (HttpMethod.class.getName().equals(a.name())) {
				if (m != null)
					throw new RuntimeException("Duplicate mapping on " + name);
				m = a.value().orElse(null);
				continue;
			}
			Optional<AnnotationModel> o = loader.get(a.name()).asClass().annotation(HttpMethod.class);
			if (o.isPresent()) {
				if (m != null)
					throw new RuntimeException("Duplicate mapping on " + name);
				m = o.flatMap(n -> n.value()).orElse(null);
			}
		}
		return m;
	}

	private static boolean isMapped(MethodModel m) {
		for (AnnotationModel a : m.annotations()) {
			if (JAXRS_ANNOTATIONS.contains(a.name()))
				return true;
		}
		return false;
	}

	private static List<PathPart> parsePath(StringBuilder sb, String path) {
		Matcher m = p.matcher(path);
		List<PathPart> parts = new ArrayList<>();
		int i = 0;
		while (m.find()) {
			parts.add(new PathPart(m.group(1), m.group(2), m.start() - i));
			i = m.end();
			m.appendReplacement(sb, "{}");
		}
		m.appendTail(sb);
		return parts;
	}

	private static boolean hasEq(String[] a, String[] b) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < b.length; j++)
				if (a[i].equals(b[i]))
					return true;
		}
		return false;
	}

	public static class PathPart {
		public final String name;
		public final String pattern;
		public final int i;

		/**
		 * create new PathPart
		 * 
		 * @param name
		 * @param pattern
		 * @param i
		 */
		public PathPart(String name, String pattern, int i) {
			this.name = name;
			this.pattern = pattern;
			this.i = i;
		}
	}
}
