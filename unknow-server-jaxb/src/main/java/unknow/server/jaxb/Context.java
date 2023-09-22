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

	private final Map<QName, XmlRootHandler<?>> handlers;
	private final Map<Class<?>, XmlHandler<?>> elements;

	public Context(Map<QName, XmlRootHandler<?>> handlers, Map<Class<?>, XmlHandler<?>> elements) {
		this.handlers = handlers;
		this.elements = elements;
	}

	@Override
	public Unmarshaller createUnmarshaller() throws JAXBException {
		return new UnmarshallerImpl(handlers, elements);
	}

	@Override
	public Marshaller createMarshaller() throws JAXBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Validator createValidator() throws JAXBException {
		// TODO Auto-generated method stub
		return null;
	}
}
