/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import unknow.server.jaxb.NsCollector;
import unknow.server.maven.jaxb.model.XmlChoice;
import unknow.server.maven.jaxb.model.XmlCollection;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlEnum;
import unknow.server.maven.jaxb.model.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.jaxb.model.XmlTypeComplex;
import unknow.server.maven.jaxws.binding.Operation;
import unknow.server.maven.jaxws.binding.Parameter;
import unknow.server.maven.jaxws.binding.Service;

/**
 * @author unknow
 */
public class WsdlBuilder {
	private static final String XS = "http://www.w3.org/2001/XMLSchema";
	private static final String WS = "http://schemas.xmlsoap.org/wsdl/";
	private static final String WP = "http://schemas.xmlsoap.org/wsdl/soap/";

	private static final String ELEMENT = "element";
	private static final String RESPONSE = "Response";

	private final Service service;
	private final String address;

	private final Map<String, Set<XmlType>> types;

	private final Map<String, String> nsPrefix;

	public WsdlBuilder(Service service, String address) {
		this.service = service;
		this.address = address;

		this.types = new HashMap<>();

		Map<String, Integer> ns = new HashMap<>();
		ns.put(WS, 6);
		// message: op * (warpped * 4 || param+1 + result*2)
		// port:  op * (wapped*2 || result? + param?)
		// binding op * (param? + result?)
		ns.put(WP, 2);
		// binding  op * (param? + result?)

		for (Operation o : service.operations) {
			ns.merge(WS, 1, Integer::sum);
			ns.merge(WP, 1, Integer::sum);

			if (o.wrapped)
				ns.merge(o.name.getNamespaceURI(), 1, Integer::sum);
			for (Parameter p : o.params) {
				if (!o.wrapped)
					ns.merge(p.name.getNamespaceURI(), 1, Integer::sum);
				collectType(p.xml, ns);
			}

			if (o.result != null) {
				if (!o.wrapped)
					ns.merge(o.result.name.getNamespaceURI(), 1, Integer::sum);
				collectType(o.result.xml, ns);
			}
		}
		ns.merge(XS, ns.size() - 3, Integer::sum);
		this.nsPrefix = NsCollector.buildNsMapping(ns);
	}

	private void collectType(XmlType t, Map<String, Integer> ns) {
		if (t instanceof XmlCollection)
			t = ((XmlCollection) t).component();

		if (t instanceof XmlChoice) {
			for (XmlElement e : ((XmlChoice) t).choice())
				collectType(e.xmlType(), ns);
			return;
		}

		ns.merge(t.name().getNamespaceURI(), 1, Integer::sum);
		if (!types.computeIfAbsent(t.name().getNamespaceURI(), k -> new HashSet<>()).add(t))
			return;

		if (t instanceof XmlTypeComplex) {
			XmlTypeComplex x = (XmlTypeComplex) t;
			for (XmlElement e : x.getAttributes())
				collectType(e.xmlType(), ns);
			for (XmlElement e : x.getElements())
				collectType(e.xmlType(), ns);
			if (x.getValue() != null)
				collectType(x.getValue().xmlType(), ns);
		}
	}

	public void write(XMLStreamWriter out) throws XMLStreamException {
		out.writeStartElement(nsPrefix.get(WS), "definitions", WS);
		for (Entry<String, String> e : nsPrefix.entrySet())
			out.writeNamespace(e.getValue(), e.getKey());
		out.writeAttribute("targetNamespace", service.ns);
		out.writeAttribute("name", service.name);
		out.writeStartElement(WS, "types");

		for (String ns : nsPrefix.keySet()) {
			if (XS.equals(ns) || WS.equals(ns) || WP.equals(ns))
				continue;
			appendSchema(out, ns);
		}

		out.writeEndElement();
		for (Operation o : service.operations)
			appendMessage(out, o);

		appendPortType(out);
		appendBinding(out);
		appendService(out);

		out.writeEndElement();
		out.close();
	}

	private void appendSchema(XMLStreamWriter out, String ns) throws XMLStreamException {
		out.writeStartElement(XS, "schema");
		out.writeAttribute("targetNamespace", ns);
		out.writeAttribute("elementFormDefault", "qualified");

		for (Operation o : service.operations) {
			if (o.wrapped) {
				if (!ns.equals(o.name.getNamespaceURI()))
					continue;
				out.writeStartElement(XS, ELEMENT);
				out.writeAttribute("name", o.name.getLocalPart());
				out.writeStartElement(XS, "complexType");
				for (Parameter p : o.params) {
					out.writeStartElement(XS, ELEMENT);
					out.writeAttribute("name", p.name.getLocalPart());
					out.writeAttribute("type", name(p.xml.name()));
					out.writeEndElement();
				}
				out.writeEndElement();
				out.writeEndElement();

				out.writeStartElement(XS, ELEMENT);
				out.writeAttribute("name", o.name.getLocalPart() + RESPONSE);
				out.writeStartElement(XS, "complexType");
				if (o.result != null) {
					out.writeStartElement(XS, ELEMENT);
					out.writeAttribute("name", o.result.name.getLocalPart());
					out.writeAttribute("type", name(o.result.xml.name()));
					out.writeEndElement();
				}
				// TODO result
				out.writeEndElement();
				out.writeEndElement();
				continue;
			}

			for (Parameter p : o.params) {
				if (!ns.equals(p.name.getNamespaceURI()))
					continue;
				out.writeStartElement(XS, ELEMENT);
				out.writeAttribute("name", p.name.getLocalPart());
				out.writeAttribute("type", name(p.xml.name()));
				out.writeEndElement();
			}
			if (o.result != null && ns.equals(o.result.name.getNamespaceURI())) {
				out.writeStartElement(XS, "elementR");
				out.writeAttribute("name", o.result.name.getLocalPart());
				out.writeAttribute("type", name(o.result.xml.name()));
				out.writeEndElement();
			}
		}

		for (XmlType t : types.getOrDefault(ns, Collections.emptySet()))
			appendType(out, t);
		out.writeEndElement();
	}

	private void appendType(XMLStreamWriter out, XmlType t) throws XMLStreamException {
		if (t instanceof XmlCollection)
			t = ((XmlCollection) t).component();

		if (t instanceof XmlEnum) {
			out.writeStartElement(XS, "simpleType");
			out.writeAttribute("name", t.name().getLocalPart());
			out.writeStartElement(XS, "restriction");
			out.writeAttribute("base", name(XS, "string"));
			for (XmlEnumEntry e : ((XmlEnum) t).entries()) {
				out.writeStartElement(XS, "enumeration");
				out.writeAttribute("value", e.value());
				out.writeEndElement();
			}
			out.writeEndElement();
			out.writeEndElement();
			return;
		}
		if (!(t instanceof XmlTypeComplex))
			return;

		XmlTypeComplex o = (XmlTypeComplex) t;

		out.writeStartElement(XS, "complexType");
		out.writeAttribute("name", o.name().getLocalPart());

		if (o.getElements() == null && o.getValue() != null) {
			out.writeStartElement(XS, "simpleContent");
			out.writeStartElement(XS, "extension");
			out.writeAttribute("base", name(o.getValue().qname()));
		}

		out.writeStartElement(XS, o.getElements().group().toString());
		for (XmlElement e : o.getElements())
			appendElement(out, e);
		out.writeEndElement();

		for (XmlElement e : o.getAttributes()) { // TODO list ?
			out.writeStartElement(XS, "attribute");
			out.writeAttribute("name", e.name());
			out.writeAttribute("type", name(e.xmlType().name()));
			out.writeEndElement();
		}
		if (o.getElements() == null && o.getValue() != null) {
			out.writeEndElement();
			out.writeEndElement();
		}
		out.writeEndElement();
	}

	private void appendElement(XMLStreamWriter out, XmlElement e) throws XMLStreamException {
		XmlType x = e.xmlType();
		if (x instanceof XmlCollection)
			x = ((XmlCollection) x).component();

		if (x instanceof XmlChoice) {
			out.writeStartElement(XS, "choice");
			if (e.xmlType() instanceof XmlCollection)
				out.writeAttribute("maxOccurs", "unbounded");

			for (XmlElement v : ((XmlChoice) x).choice())
				appendElement(out, v);
			out.writeEndElement();

			return;
		}

		out.writeStartElement(XS, ELEMENT);
		if (e.xmlType() instanceof XmlCollection)
			out.writeAttribute("maxOccurs", "unbounded");
		out.writeAttribute("name", e.name());
		out.writeAttribute("type", name(x.name()));
		out.writeEndElement();
	}

	/**
	 * @param sb
	 * @param o
	 * @throws XMLStreamException 
	 * @throws IOException
	 */
	private void appendMessage(XMLStreamWriter out, Operation o) throws XMLStreamException {
		if (o.wrapped || !o.params.isEmpty()) {
			out.writeStartElement(WS, "message");
			out.writeAttribute("name", o.name.getLocalPart());
			if (o.wrapped) {
				out.writeStartElement(WS, "part");
				out.writeAttribute("name", "param");
				out.writeAttribute(ELEMENT, name(o.name));
				out.writeEndElement();
			} else {
				for (Parameter p : o.params) {
					out.writeStartElement(WS, "part");
					out.writeAttribute("name", p.name.getLocalPart());
					out.writeAttribute(ELEMENT, name(p.name));
					out.writeEndElement();
				}
			}
			out.writeEndElement();
		}
		if (o.wrapped || o.result != null) {
			out.writeStartElement(WS, "message");
			out.writeAttribute("name", o.name.getLocalPart() + RESPONSE);
			out.writeStartElement(WS, "part");
			out.writeAttribute("name", "result");
			out.writeAttribute(ELEMENT, name(o.wrapped ? o.name : o.result.name));
			out.writeEndElement();
			out.writeEndElement();
		}
	}

	private void appendPortType(XMLStreamWriter out) throws XMLStreamException {
		out.writeStartElement(WS, "portType");
		out.writeAttribute("name", service.portType);
		for (Operation o : service.operations) {
			out.writeStartElement(WS, "operation");
			out.writeAttribute("name", o.name.getLocalPart());
			if (o.wrapped || !o.params.isEmpty()) {
				out.writeStartElement(WS, "input");
				out.writeAttribute("name", o.name.getLocalPart());
				out.writeAttribute("message", name(service.ns, o.name.getLocalPart()));
				out.writeEndElement();
			}
			if (o.wrapped || o.result != null) {
				out.writeStartElement(WS, "output");
				out.writeAttribute("name", o.name.getLocalPart() + RESPONSE);
				out.writeAttribute("message", name(service.ns, o.name.getLocalPart()) + RESPONSE);
				out.writeEndElement();
			}
			out.writeEndElement();
		}
		out.writeEndElement();
	}

	private void appendBinding(XMLStreamWriter out) throws XMLStreamException {
		out.writeStartElement(WS, "binding");
		out.writeAttribute("name", service.name + "SoapBinding");
		out.writeAttribute("type", name(service.ns, service.portType));

		out.writeStartElement(WP, "binding");
		out.writeAttribute("style", "document");
		out.writeAttribute("transport", "http://schemas.xmlsoap.org/soap/http");
		out.writeEndElement();

		for (Operation o : service.operations) {
			out.writeStartElement(WS, "operation");
			out.writeAttribute("name", o.name.getLocalPart());
			out.writeStartElement(WP, "operation");
			out.writeAttribute("soapAction", o.action);
			out.writeEndElement();
			if (o.wrapped || !o.params.isEmpty()) {
				out.writeStartElement(WS, "input");
				out.writeAttribute("name", o.name.getLocalPart());
				out.writeStartElement(WP, "body");
				out.writeAttribute("use", "literal");
				out.writeEndElement();
				out.writeEndElement();
			}
			if (o.wrapped || o.result != null) {
				out.writeStartElement(WS, "output");
				out.writeAttribute("name", o.name.getLocalPart() + RESPONSE);
				out.writeStartElement(WP, "body");
				out.writeAttribute("use", "literal");
				out.writeEndElement();
				out.writeEndElement();
			}
			out.writeEndElement();
		}
		out.writeEndElement();
	}

	private void appendService(XMLStreamWriter out) throws XMLStreamException {
		out.writeStartElement(WS, "service");
		out.writeAttribute("name", service.name);

		out.writeStartElement(WS, "port");
		out.writeAttribute("name", service.portName);
		out.writeAttribute("binding", name(service.ns, service.name + "Binding"));

		out.writeStartElement(WP, "address");
		out.writeAttribute("location", address + service.urls[0]);
		out.writeEndElement();
		out.writeEndElement();
		out.writeEndElement();
	}

	private String name(QName n) {
		return name(n.getNamespaceURI(), n.getLocalPart());
	}

	private String name(String ns, String n) {
		String p = nsPrefix.get(ns);
		if (p == null || p.isEmpty())
			return n;
		return p + ':' + n;
	}
}
