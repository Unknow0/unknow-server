package unknow.server.jaxws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public abstract class AbstractWs extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AbstractWs.class);
	private static final long serialVersionUID = 1L;

	private static final XMLInputFactory XML_IN = XMLInputFactory.newInstance();
	private static final XMLOutputFactory XML_OUT = XMLOutputFactory.newInstance();

	private static final QName ENVELOPE = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
	private static final QName HEADER = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Header");
	private static final QName BODY = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

	private final String wsdl;
	private final JAXBContext ctx;
	private int size;

	protected AbstractWs(String wsdl) {
		this.wsdl = wsdl;
		this.ctx = getCtx();
	}

	@Override
	public final void init() throws ServletException {
		if (wsdl == null)
			return;
		try (InputStream is = getServletContext().getResourceAsStream(wsdl)) {
			if (is == null)
				throw new ServletException("WSDL not found '" + wsdl + "'");
			int l;
			byte[] b = new byte[4096];
			while ((l = is.read(b)) > 0)
				size += l;
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	protected abstract JAXBContext getCtx();

	@Override
	public final void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		if (wsdl == null || req.getParameter("wsdl") == null) {
			res.sendError(405);
			return;
		}
		res.setContentType("text/xml");
		res.setContentLength(size);

		try (OutputStream out = res.getOutputStream(); InputStream is = getServletContext().getResourceAsStream(wsdl)) {
			int l;
			byte[] b = new byte[4096];
			while ((l = is.read(b)) > 0)
				out.write(b, 0, l);
		}
	}

	@Override
	public final void doPost(HttpServletRequest req, HttpServletResponse res) {
		Envelope e = new Envelope();

		try (InputStream is = req.getInputStream()) {
			XMLStreamReader r = XML_IN.createXMLStreamReader(is);

			while (r.hasNext()) {
				int n = r.next();
				if (n == XMLStreamConstants.START_ELEMENT) {
					QName q = r.getName();
					if (ENVELOPE.equals(q))
						readEnvelope(r, e);
					else
						throw new IOException("expected " + HEADER + ", " + BODY + " instead of " + q);
				}
			}
			r.close();
		} catch (IOException | XMLStreamException | JAXBException ex) {
			fault(res, ex.getMessage());
		}

		String action = req.getHeader("soapaction");
		StringBuilder sig = new StringBuilder();
		if (action != null)
			sig.append(action);
		e.sig(sig.append('/'));
		WSMethod m = getCall(sig.toString());
		if (m != null) {
			try (OutputStream out = res.getOutputStream()) {
				XMLStreamWriter w = XML_OUT.createXMLStreamWriter(out);
				writeEnvelope(w, m.call(e));
				w.close();
			} catch (Exception ex) {
				logger.error("Failed to call operation " + sig, ex);
				fault(res, ex.getMessage());
			}
		} else
			fault(res, "No operation found '" + sig + "'");
	}

	protected abstract WSMethod getCall(String sig);

	protected abstract Object read(XMLStreamReader r, Unmarshaller u) throws XMLStreamException, JAXBException, IOException;

	private final void readEnvelope(XMLStreamReader r, Envelope e) throws XMLStreamException, IOException, JAXBException, IOException {
		Unmarshaller u = ctx.createUnmarshaller();
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.START_ELEMENT) {
				QName q = r.getName();
				if (HEADER.equals(q))
					readHeader(r, u, e);
				else if (BODY.equals(q))
					readBody(r, u, e);
				else
					throw new IOException("expected " + HEADER + ", " + BODY + " instead of " + q);
			} else if (n == XMLStreamConstants.END_ELEMENT)
				return;
		}
		throw new IOException("EOF");
	}

	private final void readHeader(XMLStreamReader r, Unmarshaller u, Envelope e) throws XMLStreamException, JAXBException, IOException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.START_ELEMENT)
				e.addHeader(u.unmarshal(r));
			else if (n == XMLStreamConstants.END_ELEMENT)
				return;
		}
		throw new IOException("EOF");
	}

	private final void readBody(XMLStreamReader r, Unmarshaller u, Envelope e) throws XMLStreamException, JAXBException, IOException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.START_ELEMENT) {
				e.addBody(read(r, u));
			} else if (n == XMLStreamConstants.END_ELEMENT)
				return;
		}
		throw new IOException("EOF");
	}

	protected final OperationWrapper read(XMLStreamReader r, Unmarshaller u, OperationWrapper o) throws XMLStreamException, JAXBException, IOException {
		while (r.hasNext()) {
			int n = r.next();
			if (n == XMLStreamConstants.START_ELEMENT) {
				o.add(read(r, u));
			} else if (n == XMLStreamConstants.END_ELEMENT)
				return o;
		}
		throw new IOException("EOF");
	}

	private final void writeEnvelope(XMLStreamWriter w, Envelope e) throws XMLStreamException, JAXBException {
		Marshaller m = ctx.createMarshaller();
		m.setProperty("jaxb.fragment", true);

		w.writeStartElement("e", ENVELOPE.getLocalPart(), ENVELOPE.getNamespaceURI());
		w.writeNamespace("e", ENVELOPE.getNamespaceURI());
		if (e.getHeaderSize() > 0) {
			w.writeStartElement(HEADER.getNamespaceURI(), HEADER.getLocalPart());
			for (int i = 0; i < e.getHeaderSize(); i++)
				m.marshal(e.getHeader(i), w);
			w.writeEndElement();
		}
		w.writeStartElement(BODY.getNamespaceURI(), BODY.getLocalPart());
		for (int i = 0; i < e.getBodySize(); i++)
			m.marshal(e.getBody(i), w);
		w.writeEndElement();
		w.writeEndElement();
	}

	private static final void fault(HttpServletResponse res, String err) {
		res.setStatus(500);
		try (Writer w = res.getWriter()) {
			w.append(
					"<e:Envelope xmlns:e=\"http://schemas.xmlsoap.org/soap/envelope/\">" //@formatter:off
					+  "<e:Body>"
					+   "<e:Fault>"
					+    "<faultcode>Server</faultcode>"
					+    "<faultstring>").append(err).write("</faultstring>"
					+   "</e:Fault>"
					+  "</e:Body>"
					+ "</e:Envelope>");//@formatter:on
		} catch (@SuppressWarnings("unused") Exception e) { // OK
		}
	}

}
