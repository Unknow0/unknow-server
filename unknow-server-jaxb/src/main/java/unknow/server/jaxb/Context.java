/**
 * 
 */
package unknow.server.jaxb;

import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.Validator;

/**
 * @author unknow
 */
public class Context extends JAXBContext {

	private final Map<QName, XmlRootHandler<?>> rootElements;
	private final Map<Class<?>, XmlRootHandler<?>> rootHandlers;
	private final Map<Class<?>, XmlHandler<?>> handlers;

	public Context(Map<QName, XmlRootHandler<?>> rootElements, Map<Class<?>, XmlRootHandler<?>> rootHandlers, Map<Class<?>, XmlHandler<?>> handlers) {
		this.rootElements = rootElements;
		this.rootHandlers = rootHandlers;
		this.handlers = handlers;
	}

	@Override
	public Unmarshaller createUnmarshaller() throws JAXBException {
		return new UnmarshallerImpl(rootElements, handlers);
	}

	@Override
	public Marshaller createMarshaller() throws JAXBException {
		return new MarshallerImpl(rootHandlers);
	}

	@Override
	public Validator createValidator() throws JAXBException {
		// TODO Auto-generated method stub
		return null;
	}
}
