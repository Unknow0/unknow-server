package unknow.server.jaxb.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller.Listener;
import unknow.server.jaxb.MarshallerImpl;
import unknow.server.jaxb.UnmarshallerImpl;
import unknow.server.jaxb.XmlHandler;

public class MapHandler<K, V> implements XmlHandler<Map<K, V>> {
	public static final QName ENTRY = new QName("", "entry");
	public static final QName KEY = new QName("", "key");
	public static final QName VALUE = new QName("", "value");

	private final XmlHandler<K> key;
	private final XmlHandler<V> value;

	public MapHandler(XmlHandler<K> key, XmlHandler<V> value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public Map<K, V> read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException, JAXBException {
		Map<K, V> map = new HashMap<>();
		while (r.hasNext()) {
			int i = r.next();
			if (i == XMLStreamConstants.START_ELEMENT) {
				QName n = r.getName();
				if (ENTRY.equals(n))
					readEntry(r, map, listener);
				else
					throw new JAXBException("Expected entry instead of " + n);
			} else if (i == XMLStreamConstants.START_ELEMENT)
				return map;
		}
		throw new JAXBException("EOF");
	}

	private final void readEntry(XMLStreamReader r, Map<K, V> map, UnmarshallerImpl listener) throws XMLStreamException, JAXBException {
		K k = null;
		V v = null;
		while (r.hasNext()) {
			int i = r.next();
			if (i == XMLStreamConstants.START_ELEMENT) {
				QName n = r.getName();
				if (KEY.equals(n))
					k = key.read(r, map, listener);
				else if (VALUE.equals(n))
					v = value.read(r, map, listener);
				else
					throw new JAXBException("Expected one of key, value instead of " + n);
			} else if (i == XMLStreamConstants.START_ELEMENT) {
				map.put(k, v);
				return;
			}
		}
	}

	@Override
	public void write(XMLStreamWriter w, Map<K, V> o, MarshallerImpl m, Listener listener) throws XMLStreamException, JAXBException {
		for (Entry<K, V> e : o.entrySet()) {
			w.writeStartElement("entry");
			w.writeStartElement("key");
			key.write(w, e.getKey(), m, listener);
			w.writeEndElement();
			w.writeStartElement("value");
			value.write(w, e.getValue(), m, listener);
			w.writeEndElement();
			w.writeEndElement();
		}
	}

}
