/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import unknow.server.http.jaxrs.impl.DefaultConvert;
import unknow.server.http.jaxrs.impl.MessageByte;
import unknow.server.http.jaxrs.impl.MessageRW;
import unknow.server.http.jaxrs.impl.MessageReader;
import unknow.server.http.jaxrs.impl.MessageStream;
import unknow.server.http.jaxrs.impl.MessageStreamOutput;
import unknow.server.http.jaxrs.impl.MessageString;

/**
 * @author unknow
 */
@SuppressWarnings("rawtypes")
public class JaxrsContext {
	private static final Logger logger = LoggerFactory.getLogger(JaxrsContext.class);
	private static final String[] ALL = { "*/*" };
	private static final List<ParamConverterProvider> params = new ArrayList<>(Arrays.asList(new DefaultConvert()));
	private static final Map<Class, ExceptionMapper> exceptions = new HashMap<>();

	private static final Map<String, List<MessageBodyWriter>> writers = new HashMap<>();
	private static final Map<String, List<MessageBodyReader>> readers = new HashMap<>();
	private static final Map<Object, Integer> priorities = new HashMap<>();

	static {
		exceptions.put(WebApplicationException.class, new WebAppExceptionMapping());

		// TODO default reader/writer:
		// MultivaluedMap<String,String> (application/x-www-form-urlencoded)
		// java.util.List<EntityPart> (multipart/form-data)
		// Boolean, Character, Number (plain/text) => NoContentException for empty body with primitive
		for (MessageRW m : Arrays.asList(new MessageString(), new MessageByte(), new MessageStream(), new MessageReader())) {
			registerWriter(m, 99999, ALL);
			registerReader(m, 99999, ALL);
		}

		registerWriter(new MessageStreamOutput(), 9999, ALL);

		// Maybe add MessageBodyReader/Writer
		// jakarta.activation.DataSource, java.util.File
		// javax.xml.transform.Source (text/xml, application/xml and media types of the form application/*+xml),

		// TODO PathSegment for param
	}

	public static <T extends Throwable> void registerException(Class<T> clazz, ExceptionMapper<T> e) {
		if (exceptions.containsKey(clazz))
			logger.warn("Duplicate ExceptionMapper for exception '" + clazz + "'");
		exceptions.put(clazz, e);
	}

	public static void registerConverter(ParamConverterProvider param) {
		params.add(param);
	}

	public static void registerReader(MessageBodyReader reader, int prio, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		priorities.put(reader, prio);
		for (String mime : mimes) {
			List<MessageBodyReader> list = readers.get(mime);
			if (list == null)
				readers.put(mime, list = new ArrayList<>(1));
			list.add(reader);
		}
	}

	public static void registerWriter(MessageBodyWriter writer, int prio, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		priorities.put(writer, prio);
		for (String mime : mimes) {
			List<MessageBodyWriter> list = writers.get(mime);
			if (list == null)
				writers.put(mime, list = new ArrayList<>(1));
			list.add(writer);
		}
	}

	public static <T> ParamConverter<T> converter(Class<T> clazz, Type type, Annotation[] a) {
		for (ParamConverterProvider p : params) {
			ParamConverter<T> c = p.getConverter(clazz, type, a);
			if (c != null)
				return c;
		}
		throw new RuntimeException("No converter for " + type);
	}

	@SuppressWarnings("unchecked")
	public static void sendError(JaxrsReq r, Throwable t, HttpServletResponse res) throws IOException {
		Class<?> c = t.getClass();
		do {
			ExceptionMapper<Throwable> m = exceptions.get(c);
			if (m != null) {
				JaxrsEntityWriter.RESPONSE.write(r, m.toResponse(t), res);
				return;
			}
		} while ((c = c.getSuperclass()) != Object.class);
		res.sendError(500);
	}

	@SuppressWarnings("unchecked")
	public static <T> MessageBodyReader<T> reader(Class<T> clazz, Type genericType, Annotation[] annotations, MediaType t) {
		int prio = Integer.MAX_VALUE;
		MessageBodyReader<T> r = null;
		for (MessageBodyReader<?> m : readers.getOrDefault(t.getType() + "/" + t.getSubtype(), Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = (MessageBodyReader<T>) m;
			}
		}
		for (MessageBodyReader<?> m : readers.getOrDefault(t.getType() + "/*", Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = (MessageBodyReader<T>) m;
			}
		}
		for (MessageBodyReader<?> m : readers.getOrDefault("*/*", Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = (MessageBodyReader<T>) m;
			}
		}
		if (r == null)
			throw new NotSupportedException("No reader for " + clazz + " " + t);
		return r;
	}

	@SuppressWarnings("unchecked")
	public static MessageBodyWriter<Object> writer(Class clazz, Type genericType, Annotation[] annotations, MediaType t) {
		int prio = Integer.MAX_VALUE;
		MessageBodyWriter<Object> r = null;
		for (MessageBodyWriter m : writers.getOrDefault(t.getType() + "/" + t.getSubtype(), Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = m;
			}
		}
		for (MessageBodyWriter m : writers.getOrDefault(t.getType() + "/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = m;
			}
		}
		for (MessageBodyWriter m : writers.getOrDefault("*/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			int p = priorities.get(m);
			if (prio > p) {
				prio = p;
				r = m;
			}
		}
		if (r == null)
			throw new InternalServerErrorException("No writer for " + clazz + " " + t);
		return r;
	}

	public static Type getParamType(Type t) {
		if (t instanceof GenericArrayType)
			return ((GenericArrayType) t).getGenericComponentType();
		if (t instanceof Class) {
			Class<?> cl = (Class) t;
			if (cl.isArray())
				return cl.getComponentType();
		}

		Type r = getCollectionType(t, Collections.emptyMap());
		return r != null ? r : t;
	}

	private static Type getCollectionType(Type p, Map<String, Type> params) {
		if (p instanceof ParameterizedType)
			return getCollectionType((ParameterizedType) p, params);
		if (p instanceof Class)
			return getCollectionType((Class) p, params);
		return p;
	}

	private static Type getCollectionType(Class c, Map<String, Type> params) {
		Type[] inter = c.getGenericInterfaces();
		for (int i = 0; i < inter.length; i++) {
			Type t = inter[i];
			Type r = getCollectionType(t, params);
			if (r != null)
				return r;
		}
		return getParamType(c.getGenericSuperclass());
	}

	private static Type getCollectionType(ParameterizedType p, Map<String, Type> params) {
		Class c = (Class) p.getRawType();
		Type[] a = p.getActualTypeArguments();
		if (c.equals(Collection.class)) {
			Type b = a[0];
			if (b instanceof TypeVariable)
				b = params.getOrDefault(((TypeVariable) b).getName(), Object.class);
			return b;
		}

		Map<String, Type> map = new HashMap<>();
		TypeVariable[] t = c.getTypeParameters();
		for (int i = 0; i < t.length; i++) {
			Type b = a[i];
			if (b instanceof TypeVariable)
				b = params.getOrDefault(((TypeVariable) b).getName(), Object.class);
			if (b instanceof WildcardType) {
				Type[] w = ((WildcardType) b).getUpperBounds();
				b = w.length == 1 ? w[0] : Object.class;
			}
			map.put(t[i].getName(), b);
		}
		return getCollectionType(c, map);
	}

	private static class WebAppExceptionMapping implements ExceptionMapper<WebApplicationException> {

		@Override
		public Response toResponse(WebApplicationException e) {
			return e.getResponse();
		}
	}
}
