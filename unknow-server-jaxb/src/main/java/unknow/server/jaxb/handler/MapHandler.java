package unknow.server.jaxb.handler;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller.Listener;
import unknow.server.jaxb.MarshallerImpl;
import unknow.server.jaxb.UnmarshallerImpl;
import unknow.server.jaxb.XmlHandler;

@SuppressWarnings("rawtypes")
public class MapHandler implements XmlHandler<Map> {
	public static final MapHandler INSTANCE = new MapHandler();

	@Override
	public Map read(XMLStreamReader r, Object parent, UnmarshallerImpl listener) throws XMLStreamException, JAXBException {
		Map map = new HashMap();
		
		return null;
	}

	@Override
	public void write(XMLStreamWriter w, Map o, MarshallerImpl m, Listener arg3) throws XMLStreamException, JAXBException {
		// TODO Auto-generated method stub

	}

}
