/**
 * 
 */
package unknow.server.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.Collections;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBException;
import unknow.server.jaxb.UnmarshallerImpl;

/**
 * @author unknow
 */
public class UnmarshallerImplTest {
	@Test
	public void test() throws JAXBException {
		UnmarshallerImpl m = new UnmarshallerImpl(Collections.singletonMap(new QName("", "o"), new OHandler()), Collections.emptyMap());
		Object r = m.unmarshal(new StringReader("<o a='4'>test</o>"));
		assertInstanceOf(O.class, r);
		O o = (O) r;
		assertEquals(o.a, 4);
		assertEquals(o.v, "test");

		r = m.unmarshal(new StringReader("<o a='4'/>"));
		assertInstanceOf(O.class, r);
		o = (O) r;
		assertEquals(o.a, 4);
		assertEquals(o.v, null);
	}

	@Test
	public void testError() {
		UnmarshallerImpl m = new UnmarshallerImpl(Collections.singletonMap(new QName("", "o"), new OHandler()), Collections.emptyMap());
		assertThrows(JAXBException.class, () -> m.unmarshal(new StringReader("<b a='4'>test</b>")));
		assertThrows(JAXBException.class, () -> m.unmarshal(new StringReader("<o a='4'><t/>test</o>")));
		assertThrows(JAXBException.class, () -> m.unmarshal(new StringReader("<o a='4'>")));
//		assertThrows(JAXBException.class, () -> m.unmarshal(new StringReader("<o a='4'/>")));
	}

	@Test
	public void write() throws XMLStreamException, FactoryConfigurationError {
		XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(System.err);

		w.setPrefix("n", "nouri");
		w.setDefaultNamespace("nouri");

		w.writeStartElement("nouri", "a");

		w.writeDefaultNamespace("nouri");
		w.writeNamespace("n", "nouri");

		w.writeEndElement();
		w.close();
	}
}
