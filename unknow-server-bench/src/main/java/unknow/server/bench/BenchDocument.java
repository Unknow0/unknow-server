package unknow.server.bench;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

public class BenchDocument {
	private static final ClassLoader cl = BenchDocument.class.getClassLoader();

	@Benchmark
	public void xmlBean() throws XmlException, IOException {
		XmlObject o;
		try (InputStream is = cl.getResourceAsStream("complex.xml")) {
			o = XmlObject.Factory.parse(is);
		}

		o.save(DUMP);
	}

	private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	private static final TransformerFactory tranformer = TransformerFactory.newInstance();

	@Benchmark
	public void document() throws SAXException, IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
		Document o;
		try (InputStream is = cl.getResourceAsStream("complex.xml")) {
			o = docFactory.newDocumentBuilder().parse(is);
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
