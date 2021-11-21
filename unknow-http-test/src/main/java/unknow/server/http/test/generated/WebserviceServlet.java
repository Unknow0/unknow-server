package unknow.server.http.test.generated;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import unknow.sax.SaxContext;
import unknow.sax.SaxHandler;
import unknow.sax.SaxParser;
import unknow.server.http.test.Webservice;
import unknow.server.http.test.xml.Mixed;
import unknow.server.http.test.xml.Root;
import unknow.server.jaxws.Element;
import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.Envelope.Operation;
import unknow.server.jaxws.OperationWrapper;

@WebServlet(urlPatterns = { "/Webservice" }, name = "Webservice")
public final class WebserviceServlet extends HttpServlet implements SaxHandler<SaxContext> {

	private static final long serialVersionUID = 1;

	private static final Logger log = LoggerFactory.getLogger(WebserviceServlet.class);

	private static final Webservice WS = new Webservice();

	private static final String[] OP_SIG = new String[3];

	@SuppressWarnings("unchecked")
	private static final Function<Envelope, Envelope>[] OP_CALL = new Function[3];

	static {
		OP_SIG[0] = "#unknow.server.http.test.xml.Mixed;";
		OP_CALL[0] = (e) -> {
			Envelope r = new Envelope();
			Object ro = WS.mixed((Mixed) e.getBody(0));
			r.addBody(new Element("unknow.test", "mixedResponse", ro));
			return r;
		};
		OP_SIG[1] = "#unknow.server.http.test.xml.Root;";
		OP_CALL[1] = (e) -> {
			Envelope r = new Envelope();
			Object ro = WS.bare((Root) e.getBody(0));
			r.addBody(new Element("unknow.test", "bareResponse", ro));
			return r;
		};
		OP_SIG[2] = "{unknow.test}wrapped";
		OP_CALL[2] = (e) -> {
			Envelope r = new Envelope();
			Operation o = (Operation) e.getBody(0);
			Object ro = WS.wrapped((Root) o.get(0));
			r.addBody(new OperationWrapper("unknow.test", "wrapped", new Element("", "return", ro)));
			return r;
		};
	}

	private static final SaxHandler<SaxContext> $1$ = new SaxHandler<SaxContext>() {

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			context.push(context.textContent());
			context.previous();
		}
	};

	private static final SaxHandler<SaxContext> $0$ = new SaxHandler<SaxContext>() {

		@Override
		public final void attributes(String qname, String name, Attributes attrs, SaxContext context) throws SAXException {
			Mixed o = new Mixed();
			context.push(o);
		}

		@Override
		public final void startElement(String qname, String name, SaxContext context) throws SAXException {
			if ("elem".equals(qname))
				context.next($1$);
			else
				throw new SAXException("Invalid tag " + qname);
		}

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			if ("elem".equals(qname)) {
				String v = context.pop();
				((Mixed) context.peek()).setElem(v);
			} else {
				((Mixed) context.peek()).setValue(Integer.parseInt(context.textContent()));
				context.previous();
			}
		}
	};

	private static final SaxHandler<SaxContext> $2$ = new SaxHandler<SaxContext>() {

		@Override
		public final void attributes(String qname, String name, Attributes attrs, SaxContext context) throws SAXException {
			Root o = new Root();
			context.push(o);
			String a = attrs.getValue("", "value");
			if (a != null)
				o.setValue(Integer.parseInt(a));
		}

		@Override
		public final void startElement(String qname, String name, SaxContext context) throws SAXException {
			if ("elem".equals(qname))
				context.next($1$);
			else
				throw new SAXException("Invalid tag " + qname);
		}

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			if ("elem".equals(qname)) {
				String v = context.pop();
				((Root) context.peek()).setElem(v);
			} else
				context.previous();
		}
	};

	private static final SaxHandler<SaxContext> $3$ = new SaxHandler<SaxContext>() {

		@Override
		public final void attributes(String qname, String name, Attributes attrs, SaxContext context) throws SAXException {
			Operation o = new Operation();
			context.push(o);
			o.setQName(qname);
		}

		@Override
		public final void startElement(String qname, String name, SaxContext context) throws SAXException {
			if ("{webservice.unknow}Root".equals(qname))
				context.next($2$);
			else
				throw new SAXException("Invalid tag " + qname);
		}

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			if ("{webservice.unknow}Root".equals(qname)) {
				Object v = context.pop();
				((Operation) context.peek()).add(v);
			} else
				context.previous();
		}
	};

	@Override
	public final void startElement(String qname, String name, SaxContext context) throws SAXException {
		if ("{http://www.w3.org/2001/12/soap-envelope}Header".equals(qname))
			context.next(HEADER);
		else if ("{http://www.w3.org/2001/12/soap-envelope}Body".equals(qname))
			context.next(BODY);
		else if ("{http://www.w3.org/2001/12/soap-envelope}Envelope".equals(qname))
			context.push(new Envelope());
		else
			throw new SAXException("Invalid tag " + qname);
	}

	private static final SaxHandler<SaxContext> HEADER = new SaxHandler<SaxContext>() {

		@Override
		public final void startElement(String qname, String name, SaxContext context) throws SAXException {
			throw new SAXException("Invalid tag " + qname);
		}

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			context.previous();
		}
	};

	private static final SaxHandler<SaxContext> BODY = new SaxHandler<SaxContext>() {

		@Override
		public final void startElement(String qname, String name, SaxContext context) throws SAXException {
			if ("{unknow.test}wrapped".equals(qname))
				context.next($3$);
			else if ("{webservice.unknow}Root".equals(qname))
				context.next($2$);
			else if ("{webservice.unknow}Mixed".equals(qname))
				context.next($0$);
			else
				throw new SAXException("Invalid tag " + qname);
		}

		@Override
		public final void endElement(String qname, String name, SaxContext context) throws SAXException {
			if ("{unknow.test}wrapped".equals(qname)) {
				Object v = context.pop();
				((Envelope) context.peek()).addBody(v);
			} else if ("{webservice.unknow}Root".equals(qname)) {
				Object v = context.pop();
				((Envelope) context.peek()).addBody(v);
			} else if ("{webservice.unknow}Mixed".equals(qname)) {
				Object v = context.pop();
				((Envelope) context.peek()).addBody(v);
			} else
				context.previous();
		}
	};

	private static final byte[] WSDL = new byte[] { 60, 119, 115, 58, 100, 101, 102, 105, 110, 105, 116, 105, 111, 110, 115, 32, 110, 97, 109, 101, 61, 34, 87, 101, 98, 115, 101, 114, 118, 105, 99, 101, 34, 32, 120, 109, 108, 110, 115, 58, 119, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 115, 99, 104, 101, 109, 97, 115, 46, 120, 109, 108, 115, 111, 97, 112, 46, 111, 114, 103, 47, 119, 115, 100, 108, 47, 34, 32, 120, 109, 108, 110, 115, 58, 115, 111, 97, 112, 61, 34, 104, 116, 116, 112, 58, 47, 47, 115, 99, 104, 101, 109, 97, 115, 46, 120, 109, 108, 115, 111, 97, 112, 46, 111, 114, 103, 47, 119, 115, 100, 108, 47, 115, 111, 97, 112, 47, 34, 32, 120, 109, 108, 110, 115, 58, 120, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 119, 51, 46, 111, 114, 103, 47, 50, 48, 48, 49, 47, 88, 77, 76, 83, 99, 104, 101, 109, 97, 34, 62, 60, 119, 115, 58, 116, 121, 112, 101, 115, 62, 60, 120, 115, 58, 115, 99, 104, 101, 109, 97, 32, 120, 109, 108, 110, 115, 61, 34, 34, 62, 60, 47, 120, 115, 58, 115, 99, 104, 101, 109, 97, 62, 60, 120, 115, 58, 115, 99, 104, 101, 109, 97, 32, 120, 109, 108, 110, 115, 61, 34, 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 119, 51, 46, 111, 114, 103, 47, 50, 48, 48, 49, 47, 88, 77, 76, 83, 99, 104, 101, 109, 97, 34, 62, 60, 47, 120, 115, 58, 115, 99, 104, 101, 109, 97, 62, 60, 120, 115, 58, 115, 99, 104, 101, 109, 97, 32, 120, 109, 108, 110, 115, 61, 34, 119, 101, 98, 115, 101, 114, 118, 105, 99, 101, 46, 117, 110, 107, 110, 111, 119, 34, 62, 60, 47, 120, 115, 58, 115, 99, 104, 101, 109, 97, 62, 60, 120, 115, 58, 115, 99, 104, 101, 109, 97, 32, 120, 109, 108, 110, 115, 61, 34, 117, 110, 107, 110, 111, 119, 46, 116, 101, 115, 116, 34, 62, 60, 120, 115, 58, 99, 111, 109, 112, 108, 101, 120, 84, 121, 112, 101, 32, 110, 97, 109, 101, 61, 34, 119, 114, 97, 112, 112, 101, 100, 34, 62, 60, 120, 115, 58, 115, 101, 113, 117, 101, 110, 99, 101, 62, 60, 120, 115, 58, 101, 108, 101, 109, 101, 110, 116, 32, 110, 97, 109, 101, 61, 34, 82, 111, 111, 116, 34, 32, 116, 121, 112, 101, 61, 34, 110, 117, 108, 108, 58, 82, 111, 111, 116, 34, 47, 62, 60, 47, 120, 115, 58, 115, 99, 104, 101, 109, 97, 62, 60, 47, 119, 115, 58, 116, 121, 112, 101, 115, 62, 60, 47, 119, 115, 58, 100, 101, 102, 105, 110, 105, 116, 105, 111, 110, 115, 62 };

	@Override
	public final void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("text/xml");
		res.setContentLength(485);
		res.getOutputStream().write(WSDL);
	}

	@Override
	public final void doPost(HttpServletRequest req, HttpServletResponse res) {
		try {
			Envelope e = SaxParser.parse(this, new InputSource(req.getInputStream()));
			System.out.println(e);
			int i = Arrays.binarySearch(OP_SIG, e.sig());
			Marshallers.marshall(OP_CALL[i].apply(e), res.getWriter());
		} catch (Exception e) {
			res.setStatus(500);
			try {
				res.getWriter().append("<e:Envelope xmlns:e=\"http://schemas.xmlsoap.org/soap/envelope/\"><e:Body><e:Fault><faultcode>Client</faultcode><faultstring>").append(e.getMessage()).append("</faultstring></e:Fault></e:Body></e:Envelope>");
			} catch (Exception ignore) {
			}
			log.warn("failed to service request", e);
		}
	}
}
