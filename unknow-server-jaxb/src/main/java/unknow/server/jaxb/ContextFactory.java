/**
 * 
 */
package unknow.server.jaxb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

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
public abstract class ContextFactory implements JAXBContextFactory {
	private final Map<Class<?>, XmlHandler<?>> handlers = new HashMap<>();

	protected <T> void register(Class<T> cl, XmlHandler<T> h) {
		handlers.put(cl, h);
	}

	@Override
	public JAXBContext createContext(Class<?>[] classesToBeBound, Map<String, ?> properties) throws JAXBException {
		Map<Class<?>, XmlHandler<?>> h = new HashMap<>();
		Map<Class<?>, XmlRootHandler<?>> rh = new HashMap<>();
		Map<QName, XmlRootHandler<?>> re = new HashMap<>();

		for (Class<?> c : classesToBeBound) {
			XmlHandler<?> x = handlers.get(c);
			if (x == null)
				x = defaultHandler(c);
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

	private final XmlHandler<?> defaultHandler(Class<?> cl) {
		if (BigDecimal.class == cl)
			return BigDecimalHandler.INSTANCE;
		if (BigInteger.class == cl)
			return BigIntegerHandler.INSTANCE;
		if (String.class == cl)
			return StringHandler.INSTANCE;
		if (boolean.class == cl || Boolean.class == cl)
			return BooleanHandler.INSTANCE;
		if (byte.class == cl || Byte.class == cl)
			return ByteHandler.INSTANCE;
		if (short.class == cl || Short.class == cl)
			return ShortHandler.INSTANCE;
		if (char.class == cl || Character.class == cl)
			return CharHandler.INSTANCE;
		if (int.class == cl || Integer.class == cl)
			return IntHandler.INSTANCE;
		if (long.class == cl || Long.class == cl)
			return LongHandler.INSTANCE;
		if (float.class == cl || Float.class == cl)
			return FloatHandler.INSTANCE;
		if (double.class == cl || Double.class == cl)
			return DoubleHandler.INSTANCE;
		return null;
	}

	protected abstract Collection<Class<?>> getClasses(String contextPackage);

	@Override
	public JAXBContext createContext(String contextPath, ClassLoader classLoader, Map<String, ?> properties) throws JAXBException {
		Map<Class<?>, XmlHandler<?>> h = new HashMap<>();
		Map<Class<?>, XmlRootHandler<?>> rh = new HashMap<>();
		Map<QName, XmlRootHandler<?>> re = new HashMap<>();

		for (String s : contextPath.split(":")) {
			for (Class<?> c : getClasses(s)) {
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
		}

		return new Context(re, rh, h);
	}

}
