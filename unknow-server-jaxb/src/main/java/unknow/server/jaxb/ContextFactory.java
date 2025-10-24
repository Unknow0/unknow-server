/**
 * 
 */
package unknow.server.jaxb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.JAXBException;
import unknow.server.jaxb.handler.BigDecimalHandler;
import unknow.server.jaxb.handler.BigIntegerHandler;
import unknow.server.jaxb.handler.BooleanHandler;
import unknow.server.jaxb.handler.ByteHandler;
import unknow.server.jaxb.handler.CharHandler;
import unknow.server.jaxb.handler.DoubleHandler;
import unknow.server.jaxb.handler.FloatHandler;
import unknow.server.jaxb.handler.IntHandler;
import unknow.server.jaxb.handler.LongHandler;
import unknow.server.jaxb.handler.ShortHandler;
import unknow.server.jaxb.handler.StringHandler;

/**
 * @author unknow
 */
public final class ContextFactory implements JAXBContextFactory {
	private static final Logger logger = LoggerFactory.getLogger(ContextFactory.class);

	private static final Map<Class<?>, XmlHandler<?>> handlers = new HashMap<>();
	private static final Map<String, Collection<Class<?>>> classes = new HashMap<>();

	static {
		handlers.put(BigDecimal.class, BigDecimalHandler.INSTANCE);
		handlers.put(BigInteger.class, BigIntegerHandler.INSTANCE);
		handlers.put(String.class, StringHandler.INSTANCE);
		handlers.put(boolean.class, BooleanHandler.INSTANCE);
		handlers.put(Boolean.class, BooleanHandler.INSTANCE);
		handlers.put(byte.class, ByteHandler.INSTANCE);
		handlers.put(Byte.class, ByteHandler.INSTANCE);
		handlers.put(short.class, ShortHandler.INSTANCE);
		handlers.put(Short.class, ShortHandler.INSTANCE);
		handlers.put(char.class, CharHandler.INSTANCE);
		handlers.put(Character.class, CharHandler.INSTANCE);
		handlers.put(int.class, IntHandler.INSTANCE);
		handlers.put(Integer.class, IntHandler.INSTANCE);
		handlers.put(long.class, LongHandler.INSTANCE);
		handlers.put(Long.class, LongHandler.INSTANCE);
		handlers.put(float.class, FloatHandler.INSTANCE);
		handlers.put(Float.class, FloatHandler.INSTANCE);
		handlers.put(double.class, DoubleHandler.INSTANCE);
		handlers.put(Double.class, DoubleHandler.INSTANCE);

		for (XmlHandlerLoader l : ServiceLoader.load(XmlHandlerLoader.class)) {
			String contextPath = l.contextPath();
			if (classes.containsKey(contextPath))
				logger.warn("Duplicate context {}", contextPath);
			Collection<XmlHandler<?>> list = l.handlers();
			Class<?>[] c = new Class[list.size()];
			int i = 0;
			for (XmlHandler<?> h : list) {
				if (handlers.containsKey(h.clazz()))
					logger.warn("Duplicate handled {}", h);
				c[i++] = h.clazz();
				handlers.put(h.clazz(), h);
			}
			classes.put(contextPath, Arrays.asList(c));
		}
	}

	@Override
	public JAXBContext createContext(Class<?>[] classesToBeBound, Map<String, ?> properties) throws JAXBException {
		return createContext(Arrays.asList(classesToBeBound));
	}

	private JAXBContext createContext(Collection<Class<?>> classesToBeBound) throws JAXBException {
		Map<Class<?>, XmlHandler<?>> h = new HashMap<>();
		Map<Class<?>, XmlRootHandler<?>> rh = new HashMap<>();
		Map<QName, XmlRootHandler<?>> re = new HashMap<>();

		for (Class<?> c : classesToBeBound) {
			XmlHandler<?> x = handlers.get(c);
			if (x == null)
				throw new JAXBException("No handler for class " + c);
			if (x instanceof XmlRootHandler) {
				XmlRootHandler<?> r = (XmlRootHandler<?>) x;
				re.put(r.qname(), r);
				rh.put(c, r);
			}
			h.put(c, x);
		}
		return new Context(re, rh, h);
	}

	@Override
	public JAXBContext createContext(String contextPath, ClassLoader classLoader, Map<String, ?> properties) throws JAXBException {
		List<Class<?>> list = new ArrayList<>();
		for (String s : contextPath.split(":")) {
			Collection<Class<?>> a = classes.get(s);
			if (a == null)
				throw new JAXBException("No classes mapped in context " + s);
			list.addAll(a);
		}

		return createContext(list);
	}

}
