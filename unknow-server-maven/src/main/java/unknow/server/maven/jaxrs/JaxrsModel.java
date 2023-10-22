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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.api.plugin.MojoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import io.swagger.v3.oas.annotations.Hidden;
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
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam.JaxrsBeanFieldParam;
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
	private static final Logger logger = LoggerFactory.getLogger(JaxrsModel.class);
	private static final String[] ALL = { "*/*" };

	private static final Set<String> JAXRS_ANNOTATIONS = new HashSet<>(Arrays.asList(GET.class.getName(), POST.class.getName(), PUT.class.getName(), DELETE.class.getName(),
			PATCH.class.getName(), HEAD.class.getName(), OPTIONS.class.getName(), HttpMethod.class.getName()));

	private static final Set<String> JARXS_PARAM = new HashSet<>(Arrays.asList(BeanParam.class.getName(), PathParam.class.getName(), QueryParam.class.getName(),
			CookieParam.class.getName(), HeaderParam.class.getName(), FormParam.class.getName(), MatrixParam.class.getName()));

	private final List<JaxrsMapping> mappings = new ArrayList<>();
	// path, method
	private final ModelLoader loader;
	private final TypeModel paramProvider;
	private final TypeModel exceptionMapper;
	private final TypeModel bodyReader;
	private final TypeModel bodyWriter;
	private final TypeModel string;

	public final List<String> converter = new ArrayList<>();
	public final Map<ClassModel, List<String>> readers = new HashMap<>();
	public final Map<ClassModel, List<String>> writers = new HashMap<>();
	public final Map<TypeModel, ClassModel> exceptions = new HashMap<>();

	public final Set<String> implicitConstructor = new HashSet<>();
	public final Set<String> implicitFromString = new HashSet<>();
	public final Set<String> implicitValueOf = new HashSet<>();

	/**
	 * create new JaxrsModel
	 * 
	 * @param loader
	 */
	public JaxrsModel(ModelLoader loader, ClassLoader cl) {
		this.loader = loader;
		this.paramProvider = loader.get(ParamConverterProvider.class.getName());
		this.exceptionMapper = loader.get(ExceptionMapper.class.getName());
		this.bodyReader = loader.get(MessageBodyReader.class.getName());
		this.bodyWriter = loader.get(MessageBodyWriter.class.getName());
		this.string = loader.get(String.class.getName());

		loadService(cl, MessageBodyReader.class, l -> {
			ClassModel c = loader.get(l).asClass();
			String[] v = c.annotation(Consumes.class).flatMap(a -> a.value()).map(a -> a.asArrayLiteral()).orElse(ALL);
			List<String> list = new ArrayList<>();
			for (int i = 0; i < v.length; i++)
				list.addAll(Arrays.asList(v[i].split(" *, *")));
			readers.put(c, list);
		});
		loadService(cl, MessageBodyWriter.class, l -> {
			ClassModel c = loader.get(l).asClass();
			String[] v = c.annotation(Produces.class).flatMap(a -> a.value()).map(a -> a.asArrayLiteral()).orElse(ALL);
			List<String> list = new ArrayList<>();
			for (int i = 0; i < v.length; i++)
				list.addAll(Arrays.asList(v[i].split(" *, *")));
			writers.put(c, list);
		});
	}

	private static void loadService(ClassLoader loader, Class<?> clazz, Consumer<String> v) {
		try {
			Enumeration<URL> e = loader.getResources("META-INF/services/" + clazz.getName());
			while (e.hasMoreElements()) {
				URL u = e.nextElement();
				try {
					URLConnection uc = u.openConnection();
					uc.setUseCaches(false);
					try (InputStream in = uc.getInputStream(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
						String l;
						while ((l = r.readLine()) != null) {
							String c = parseLine(u, l);
							if (c != null)
								v.accept(c);
						}
					}
				} catch (IOException x) {
					logger.error("Failed to read service {}", u, x);
				}
			}
		} catch (IOException e) {
			logger.error("Failed to load service {}", clazz.getName(), e);
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
			logger.warn("Ignoring invalid line '{}' in {}", ln, url);
			return null;
		}
		int cp = ln.codePointAt(0);
		if (!Character.isJavaIdentifierStart(cp)) {
			logger.warn("Ignoring invalid class name {} in {}", ln, url);
			return null;
		}
		int start = Character.charCount(cp);
		for (int i = start; i < n; i += Character.charCount(cp)) {
			cp = ln.codePointAt(i);
			if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
				logger.warn("Ignoring invalid class name {} in {}", ln, url);
				return null;
			}
		}
		return ln;
	}

	public List<JaxrsMapping> mappings() {
		return mappings;
	}

	public void process(ClassModel clazz) {
		if (clazz.annotation(Hidden.class).isPresent())
			return;
		clazz.annotation(Provider.class).ifPresent(a -> {
			if (paramProvider.isAssignableFrom(clazz))
				converter.add(clazz.name());
			ClassModel e = clazz.ancestor(exceptionMapper);
			if (e != null) {
				if (exceptions.containsKey(e))
					logger.error("Duplicate exception mapping for '{}'", e);
				else
					exceptions.put(e.parameter(0).type(), clazz);
			}
			if (bodyReader.isAssignableFrom(clazz)) {
				List<String> list = new ArrayList<>();
				for (String s : clazz.annotation(Consumes.class).flatMap(v -> v.value()).map(v -> v.asArrayLiteral()).orElse(ALL))
					list.addAll(Arrays.asList(s.trim().split(" *, *")));
				readers.put(clazz, list);
			}
			if (bodyWriter.isAssignableFrom(clazz)) {
				List<String> list = new ArrayList<>();
				for (String s : clazz.annotation(Produces.class).flatMap(v -> v.value()).map(v -> v.asArrayLiteral()).orElse(ALL))
					list.addAll(Arrays.asList(s.trim().split(" *, *")));
				writers.put(clazz, list);
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
				basePath = next.annotation(Path.class).flatMap(a -> a.value()).map(v -> v.asLiteral()).orElse(null);
			if (baseConsume == null)
				baseConsume = next.annotation(Consumes.class).flatMap(a -> a.value()).map(v -> v.asArrayLiteral()).orElse(null);
			if (baseProduce == null)
				baseProduce = next.annotation(Produces.class).flatMap(a -> a.value()).map(v -> v.asArrayLiteral()).orElse(null);
			if (method == null)
				method = getMethod(next, next.name());

			for (MethodModel m : next.methods()) {
				if (!m.isPublic() || m.isStatic() || !isMapped(m))
					continue;
				methods.putIfAbsent(m.signature(), m);
			}
		}
		if (basePath == null || basePath.isEmpty())
			basePath = "/";
		if (basePath.length() > 1 && basePath.endsWith("/"))
			basePath = basePath.substring(0, basePath.length() - 1);
		if (basePath.charAt(0) != '/')
			basePath = '/' + basePath;
		if (baseConsume == null)
			baseConsume = ALL;
		if (baseProduce == null)
			baseProduce = ALL;

		for (MethodModel m : methods.values()) {
			if (m.annotation(Hidden.class).isPresent())
				return;
			process(method, basePath, baseConsume, baseProduce, clazz, m);
		}
	}

	private void process(String defaultMethod, String path, String[] consume, String[] produce, ClassModel clazz, MethodModel m) {
		Optional<AnnotationModel> a = m.annotation(Path.class);
		if (path == null && a.isEmpty())
			throw new MojoException("no path mapping found on " + clazz.name() + " " + m.signature());

		if (a.isPresent()) {
			String s = a.flatMap(v -> v.value()).map(v -> v.asLiteral()).orElse("");
			if (s.charAt(0) != '/')
				path += "/";
			path += s;
		}

		String errorName = clazz.name() + "." + m.signature();
		String method = getMethod(m, errorName);
		if (method == null)
			method = defaultMethod;
		if (method == null)
			throw new MojoException("no method mapped on " + errorName);
		List<JaxrsParam<?>> params = new ArrayList<>();
		for (ParamModel<MethodModel> p : m.parameters()) {
			List<AnnotationModel> l = p.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
			if (l.size() > 1)
				throw new MojoException("Duplicate parameter annotation on " + errorName + " " + p.name());
			JaxrsParam<?> param = l.isEmpty() ? new JaxrsBodyParam<>(p) : buildParam(p, l.get(0));
			params.add(param);
		}

		consume = m.annotation(Consumes.class).flatMap(v -> v.value()).map(v -> v.asArrayLiteral()).orElse(consume);
		produce = m.annotation(Produces.class).flatMap(v -> v.value()).map(v -> v.asArrayLiteral()).orElse(produce);

		mappings.add(new JaxrsMapping("m$" + mappings.size(), clazz, m, method, params, path, consume, produce));
	}

	private static Optional<MethodModel> getSetter(ClassModel cl, String name, TypeModel type) {
		String set = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		return cl.method(set, type);
	}

	public static TypeModel getParamType(TypeModel type) {
		if (type.isArray())
			type = type.asArray().type();
		if (type.isPrimitive())
			return type.asPrimitive().boxed();

		if (!type.isClass())
			return type;

		ClassModel ancestor = type.asClass().ancestor("java.util.Collection");
		if (ancestor == null)
			return type;
		return ancestor.parameter(0).type();
	}

	public static Expression getParam(JaxrsParam<?> p, TypeCache types, Map<JaxrsParam<?>, String> converterVar) {
		NodeList<Expression> list = Utils.list(Utils.text(p.value), p.def == null ? new NullLiteralExpr() : Utils.text(p.def));
		String m = "get" + p.getClass().getSimpleName().substring(5, p.getClass().getSimpleName().length() - 5);

		if (p.type.isArray()) {
			m += "Array";
			list.add(new ClassExpr(types.get(p.type.asArray().type())));
		}
		if (p.type.isClass()) {
			ClassModel c = p.type.asClass().ancestor(Collection.class.getName());
			if (c != null) {
				m += "List";
			}
		}
		list.add(new NameExpr(converterVar.get(p)));
		Expression e = new MethodCallExpr(new NameExpr("r"), m, list);
		if (p.type.isAssignableTo(SortedSet.class.getName()))
			e = new ObjectCreationExpr(null, types.getClass(TreeSet.class), Utils.list(e));
		else if (p.type.isAssignableTo(Set.class.getName()))
			e = new ObjectCreationExpr(null, types.getClass(HashSet.class), Utils.list(e));
		return e;
	}

	private <T extends WithName & WithAnnotation & WithType> JaxrsParam<?> buildParam(T p, AnnotationModel a) {
		if (BeanParam.class.getName().equals(a.name())) {
			ClassModel cl = p.type().asClass();
			List<JaxrsBeanFieldParam> params = new ArrayList<>();
			Set<MethodModel> setters = new HashSet<>();
			for (FieldModel f : cl.fields()) {
				List<AnnotationModel> l = f.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
				if (l.isEmpty())
					continue;
				if (l.size() > 1)
					throw new MojoException("Duplicate parameter annotation on " + f.parent() + "." + f.name());

				Optional<MethodModel> s = getSetter(cl, f.name(), f.type());
				if (!f.isPublic() && s.isEmpty())
					throw new MojoException("Can't find setter for " + f + " in " + cl);
				params.add(new JaxrsBeanFieldParam(buildParam(f, l.get(0)), f, s.orElse(null)));
				s.ifPresent(setters::add);
			}
			for (MethodModel m : cl.methods()) {
				if (setters.contains(m))
					continue;
				List<AnnotationModel> l = m.annotations().stream().filter(v -> JARXS_PARAM.contains(v.name())).collect(Collectors.toList());
				if (l.isEmpty())
					continue;
				if (l.size() > 1)
					throw new MojoException("Duplicate parameter annotation on " + m.parent() + "." + m.name());
				String n = m.name();
				if (n.startsWith("get")) {
					m = getSetter(cl, n.substring(3), m.type()).orElseThrow(() -> new MojoException("Can't find setter for " + n + " in " + cl));
				} else if (!n.startsWith("set")) {
					m = getSetter(cl, n, m.type()).orElseThrow(() -> new MojoException("Can't find setter for " + n + " in " + cl));
				}

				setters.add(m);
				params.add(new JaxrsBeanFieldParam(buildParam(m, l.get(0)), null, m));
			}
			for (JaxrsBeanFieldParam i : params)
				processParamConvert(i.param.type);

			return new JaxrsBeanParam<>(p, cl, params);
		}
		processParamConvert(p.type());
		if (PathParam.class.getName().equals(a.name()))
			return new JaxrsPathParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));
		if (QueryParam.class.getName().equals(a.name()))
			return new JaxrsQueryParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));
		if (CookieParam.class.getName().equals(a.name()))
			return new JaxrsCookieParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));
		if (HeaderParam.class.getName().equals(a.name()))
			return new JaxrsHeaderParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));
		if (FormParam.class.getName().equals(a.name()))
			return new JaxrsFormParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));
		if (MatrixParam.class.getName().equals(a.name()))
			return new JaxrsMatrixParam<>(p, a.value().map(v -> v.asLiteral()).orElse(""));

		throw new MojoException("Unknow annotation " + a);
	}

	private void processParamConvert(TypeModel t) {
		TypeModel type = getParamType(t);
		if (type.isWildCard())
			type = type.asWildcard().bound();
		if (!type.isClass())
			return;

		ClassModel cl = type.asClass();
		if (cl.isBoxedPrimitive() || converter.contains(cl.name()))
			return;

		if (cl.constructors(string).filter(c -> c.isPublic()).isPresent())
			implicitConstructor.add(type.name());
		else {
			if (!cl.isEnum() && cl.method("valueOf", string).filter(c -> c.isPublic() && c.isStatic()).isPresent())
				implicitValueOf.add(type.name());
			else if (cl.method("fromString", string).filter(c -> c.isPublic() && c.isStatic()).isPresent())
				implicitFromString.add(type.name());
			else if (cl.isEnum())
				implicitValueOf.add(type.name());
		}
	}

	private String getMethod(WithAnnotation v, String name) {
		String m = null;
		for (AnnotationModel a : v.annotations()) {
			if (HttpMethod.class.getName().equals(a.name())) {
				if (m != null)
					throw new MojoException("Duplicate mapping on " + name);
				m = a.value().map(n -> n.asLiteral()).orElse(null);
				continue;
			}
			Optional<AnnotationModel> o = loader.get(a.name()).asClass().annotation(HttpMethod.class);
			if (o.isPresent()) {
				if (m != null)
					throw new MojoException("Duplicate mapping on " + name);
				m = o.flatMap(n -> n.value()).map(n -> n.asLiteral()).orElse(null);
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
}
