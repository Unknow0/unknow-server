/**
 * 
 */
package unknow.server.jaxb;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;

/**
 * @author unknow
 */
public class MarshallerImpl implements Marshaller {
	private static final XMLOutputFactory f = XMLOutputFactory.newInstance();

	private final Map<Class<?>, XmlRootHandler<?>> rootHandlers;
	private final Map<Class<?>, XmlHandler<?>> handlers;

	private String encoding = "utf8";
	private boolean indented = false;
	private boolean fragment = false;
	private Listener listener;

	public MarshallerImpl(Map<Class<?>, XmlRootHandler<?>> rootHandlers, Map<Class<?>, XmlHandler<?>> handlers) {
		this.rootHandlers = rootHandlers;
		this.handlers = handlers;
	}

	@Override
	public void marshal(Object jaxbElement, Result result) throws JAXBException {
		try {
			marshal(jaxbElement, f.createXMLStreamWriter(result));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public void marshal(Object jaxbElement, OutputStream os) throws JAXBException {
		try {
			marshal(jaxbElement, f.createXMLStreamWriter(os, encoding));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public void marshal(Object jaxbElement, File output) throws JAXBException {
		try (OutputStream os = Files.newOutputStream(output.toPath())) {
			marshal(jaxbElement, f.createXMLStreamWriter(os, encoding));
		} catch (XMLStreamException | IOException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public void marshal(Object jaxbElement, Writer writer) throws JAXBException {
		try {
			marshal(jaxbElement, f.createXMLStreamWriter(writer));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {
		try {
			marshal(jaxbElement, f.createXMLStreamWriter(new SAXResult(handler)));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@Override
	public void marshal(Object jaxbElement, Node node) throws JAXBException {
		try {
			marshal(jaxbElement, f.createXMLStreamWriter(new DOMResult(node)));
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
		try {
			if (indented)
				writer = new IndentedXmlStreamWriter(writer);

			Map<String, String> ns = collectNs(jaxbElement);
			if (!fragment)
				writer.writeStartDocument(encoding, "1.0");
			if (jaxbElement instanceof JAXBElement) {
				write((JAXBElement) jaxbElement, writer, ns);
			} else {
				XmlRootHandler h = rootHandlers.get(jaxbElement.getClass());
				if (h == null)
					throw new JAXBException("Unknown class '" + jaxbElement.getClass());

				for (Entry<String, String> e : ns.entrySet())
					writer.setPrefix(e.getValue(), e.getKey());
				writer.writeStartElement(ns.get(h.ns()), h.localName(), h.ns());
				for (Entry<String, String> e : ns.entrySet())
					writer.writeNamespace(e.getValue(), e.getKey());
				h.write(writer, jaxbElement, this, listener);
			}
			if (!fragment)
				writer.writeEndDocument();
		} catch (XMLStreamException e) {
			throw new JAXBException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final void write(JAXBElement e, XMLStreamWriter writer, Map<String, String> ns) throws JAXBException, XMLStreamException {
		writer.writeStartElement(e.getName().getNamespaceURI(), e.getName().getLocalPart());
		if (ns != null) {
			for (Entry<String, String> n : ns.entrySet())
				writer.writeNamespace(n.getValue(), n.getKey());
		}
		if (e.getDeclaredType() == JAXBElement.class) {
			write((JAXBElement) e.getValue(), writer, null);
		} else {
			XmlHandler h = handlers.get(e.getDeclaredType());
			if (h == null)
				throw new JAXBException("Unknown class '" + e.getDeclaredType());
			h.write(writer, e.getValue(), this, listener);
		}
		writer.writeEndElement();
	}

	@Override
	public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
		// TODO Auto-generated method stub

	}

	@Override
	public Node getNode(Object contentTree) throws JAXBException {
		throw new JAXBException("Unsupported operation");
	}

	@Override
	public void setProperty(String name, Object value) throws PropertyException {
		switch (name) {
			case "jaxb.encoding":
				encoding = (String) value;
				break;
			case "jaxb.formatted.output":
				indented = (boolean) value;
				break;
			case "jaxb.schemaLocation":
				//This property allows the client application to specify an xsi:schemaLocation attribute in the generated XML data.
				break;
			case "jaxb.noNamespaceSchemaLocation":
				//This property allows the client application to specify an xsi:noNamespaceSchemaLocation attribute in the generated XML data.
				break;
			case "jaxb.fragment":
				fragment = (boolean) value;
				break;
			default:
				throw new PropertyException(name);
		}
	}

	@Override
	public Object getProperty(String name) throws PropertyException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
		// TODO Auto-generated method stub

	}

	@Override
	public ValidationEventHandler getEventHandler() throws JAXBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttachmentMarshaller(AttachmentMarshaller am) {
		// TODO Auto-generated method stub

	}

	@Override
	public AttachmentMarshaller getAttachmentMarshaller() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSchema(Schema schema) {
		// TODO Auto-generated method stub

	}

	@Override
	public Schema getSchema() {
		// TODO Auto-generated method stub
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private final Map<String, String> collectNs(Object t) throws XMLStreamException, JAXBException {
		NsCollector c = new NsCollector();
		if (t instanceof JAXBElement) {
			write((JAXBElement) t, c, null);
		} else {
			XmlRootHandler h = rootHandlers.get(t.getClass());
			if (h == null)
				throw new JAXBException("Unknown class '" + t.getClass());
			c.writeEmptyElement(h.ns(), h.localName());
			h.write(c, t, this, null);
		}
		return c.getNs();
	}

	@Override
	public <A extends XmlAdapter<?, ?>> void setAdapter(A adapter) {
		// TODO Auto-generated method stub

	}

	@Override
	public <A extends XmlAdapter<?, ?>> void setAdapter(Class<A> type, A adapter) {
		// TODO Auto-generated method stub

	}

	@Override
	public <A extends XmlAdapter<?, ?>> A getAdapter(Class<A> type) {
		// TODO Auto-generated method stub
		return null;
	}
}
