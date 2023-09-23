/**
 * 
 */
package unknow.server.jaxb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.JAXBException;

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

	protected abstract Collection<Class<?>> getClasses(@SuppressWarnings("unused") String contextPackage);

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
