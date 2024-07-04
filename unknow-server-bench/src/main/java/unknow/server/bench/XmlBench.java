package unknow.server.bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openjdk.jmh.annotations.Benchmark;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import unknow.server.http.test.xml.Complex;

public class XmlBench {
	private static final ClassLoader cl = XmlBench.class.getClassLoader();
	private static final Class<?>[] CLASS = { Complex.class };
	static final JAXBContext UNKNOW;
	static final JAXBContext JAXB;
	static final JAXBContext MOXY;
	static final String XML;

	static {
		try {
			UNKNOW = new unknow.server.http.test.generated.JaxbContextFactory().createContext(CLASS, null);
			JAXB = new org.glassfish.jaxb.runtime.v2.JAXBContextFactory().createContext(CLASS, null);
			MOXY = new org.eclipse.persistence.jaxb.XMLBindingContextFactory().createContext(CLASS, null);
		} catch (JAXBException e) {
			throw new ExceptionInInitializerError(e);
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); InputStream is = cl.getResourceAsStream("complex.xml")) {
			byte[] b = new byte[4096];
			int l = 0;
			while ((l = is.read(b)) != -1)
				out.write(b, 0, l);
			XML = out.toString("utf8");
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static void bench(JAXBContext c) throws JAXBException, IOException {
		Object o;
		try (Reader r = new StringReader(XML)) {
			o = c.createUnmarshaller().unmarshal(r);
		}
		if (!(o instanceof Complex))
			throw new JAXBException(o.getClass().getName());
		c.createMarshaller().marshal(o, DUMP);
	}

	@Benchmark
	public void unknow() throws JAXBException, IOException {
		bench(UNKNOW);
	}

	@Benchmark
	public void reference() throws JAXBException, IOException {
		bench(JAXB);
	}

	@Benchmark
	public void moxy() throws JAXBException, IOException {
		bench(MOXY);
	}

	@Benchmark
	public void xmlBean() throws XmlException, IOException {
		XmlObject o;
		try (Reader r = new StringReader(XML)) {
			o = XmlObject.Factory.parse(r);
		}

		o.save(DUMP);
	}

	private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	private static final TransformerFactory tranformer = TransformerFactory.newInstance();

	@Benchmark
	public void document() throws SAXException, IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
		Document o;
		try (InputStream r = new ByteArrayInputStream(XML.getBytes())) {
			o = docFactory.newDocumentBuilder().parse(r);
		}

		tranformer.newTransformer().transform(new DOMSource(o), new StreamResult(DUMP));
	}

	private static final OutputStream DUMP = new OutputStream() {
		@Override
		public void write(int b) throws IOException { // OK
		}

		@Override
		public void write(byte[] b) throws IOException { // OK
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException { // OK
		}
	};
}
