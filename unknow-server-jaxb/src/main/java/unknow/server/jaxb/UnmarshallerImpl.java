/**
 * 
 */
package unknow.server.jaxb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.UnmarshallerHandler;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;

/**
 * @author unknow
 */
public class UnmarshallerImpl implements Unmarshaller {
	private static final XMLInputFactory f = XMLInputFactory.newInstance();

	private final Map<QName, XmlRootHandler<?>> rootElement;
	private final Map<Class<?>, XmlHandler<?>> elements;

	private Listener listener;
	private ValidationEventHandler validationHandler;
	private Schema schema;

	private final Map<String, Object> properties = new HashMap<>();

	public UnmarshallerImpl(Map<QName, XmlRootHandler<?>> rootElement, Map<Class<?>, XmlHandler<?>> elements) {
		this.rootElement = rootElement;
		this.elements = elements;
	}

	@Override
	public Object unmarshal(File file) throws JAXBException {
		try (InputStream is = Files.newInputStream(file.toPath())) {
			return unmarshal(f.createXMLStreamReader(is));
		} catch (IOException | XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(InputStream is) throws JAXBException {
		try {
			return unmarshal(f.createXMLStreamReader(is));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(Reader reader) throws JAXBException {
		try {
			return unmarshal(f.createXMLStreamReader(reader));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(URL url) throws JAXBException {
		try (InputStream is = url.openStream()) {
			return unmarshal(f.createXMLStreamReader(is));
		} catch (IOException | XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(InputSource source) throws JAXBException {
		Reader r = source.getCharacterStream();
		if (r != null)
			return unmarshal(r);
		return unmarshal(source.getByteStream());
	}

	@Override
	public Object unmarshal(Node node) throws JAXBException {
		// TODO convert to XMLStreamReader
		return null;
	}

	@Override
	public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType) throws JAXBException {
		// TODO convert to XMLStreamReader
		return null;
	}

	@Override
	public Object unmarshal(Source source) throws JAXBException {
		try {
			return unmarshal(f.createXMLStreamReader(source));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public <T> JAXBElement<T> unmarshal(Source source, Class<T> declaredType) throws JAXBException {
		try {
			return unmarshal(f.createXMLStreamReader(source), declaredType);
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(XMLStreamReader reader) throws JAXBException {
		try {
			if (reader.getEventType() == XMLStreamConstants.START_DOCUMENT)
				reader.nextTag();
			XmlRootHandler<?> h = rootElement.get(reader.getName());
			if (h == null)
				throw new JAXBException("No handler found for xml tag " + reader.getName());
			return h.read(reader);
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public <T> JAXBElement<T> unmarshal(XMLStreamReader reader, Class<T> declaredType) throws JAXBException {
		try {
			QName n = reader.getName();
			@SuppressWarnings("unchecked") XmlHandler<T> h = (XmlHandler<T>) elements.get(declaredType);
			if (h == null)
				throw new JAXBException("No handler found for class " + declaredType);
			return new JAXBElement<>(n, declaredType, h.read(reader));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public Object unmarshal(XMLEventReader reader) throws JAXBException {
		// TODO convert to XMLStreamReader
		return null;
	}

	@Override
	public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> declaredType) throws JAXBException {
		// TODO convert to XMLStreamReader
		return null;
	}

	@Override
	public UnmarshallerHandler getUnmarshallerHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
		validationHandler = handler;
	}

	@Override
	public ValidationEventHandler getEventHandler() throws JAXBException {
		return validationHandler;
	}

	@Override
	public void setProperty(String name, Object value) throws PropertyException {
		properties.put(name, value);
	}

	@Override
	public Object getProperty(String name) throws PropertyException {
		return properties.get(name);
	}

	@Override
	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	@Override
	public Schema getSchema() {
		return schema;
	}

	@Override
	public void setAdapter(XmlAdapter adapter) {
//	public <A extends XmlAdapter> void setAdapter(A adapter) {
	}

	@Override
	public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends XmlAdapter> A getAdapter(Class<A> type) {
		return null;
	}

	@Override
	public void setAttachmentUnmarshaller(AttachmentUnmarshaller au) {
	}

	@Override
	public AttachmentUnmarshaller getAttachmentUnmarshaller() {
		return null;
	}

	@Override
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public Listener getListener() {
		return listener;
	}

	@Override
	public void setValidating(boolean validating) throws JAXBException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isValidating() throws JAXBException {
		// TODO Auto-generated method stub
		return false;
	}
}
