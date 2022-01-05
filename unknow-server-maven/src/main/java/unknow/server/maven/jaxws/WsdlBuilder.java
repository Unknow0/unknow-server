/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import unknow.server.jaxws.XMLNsCollector;
import unknow.server.jaxws.XMLOutput;
import unknow.server.maven.jaxws.model.SchemaData;
import unknow.server.maven.jaxws.model.Service;
import unknow.server.maven.jaxws.model.Service.Op;
import unknow.server.maven.jaxws.model.Service.Param;
import unknow.server.maven.jaxws.model.XmlEnum;
import unknow.server.maven.jaxws.model.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;
import unknow.server.maven.jaxws.model.XmlType.XmlList;

/**
 * @author unknow
 */
public class WsdlBuilder {
	private final Service service;
	private final String address;
	private final ByteArrayOutputStream out;
	private final XMLOutput x;
	private final Map<String, String> nsPrefix;

	private final String ws;
	private final String wp;
	private final String xs;

	public WsdlBuilder(Service service, String address) {
		Map<String, Integer> collectNs = collectNs(service);
		collectNs.merge("http://schemas.xmlsoap.org/wsdl/", 1, Integer::sum);
		collectNs.merge("http://schemas.xmlsoap.org/wsdl/soap/", 1, Integer::sum);
		nsPrefix = XMLNsCollector.buildNsMapping(collectNs);

		this.service = service;
		this.address = address;
		this.out = new ByteArrayOutputStream();
		this.x = new XMLOutput(new OutputStreamWriter(out, StandardCharsets.UTF_8), nsPrefix);

		ws = "http://schemas.xmlsoap.org/wsdl/";
		wp = "http://schemas.xmlsoap.org/wsdl/soap/";
		xs = "http://www.w3.org/2001/XMLSchema";
	}

	public byte[] build() {
		try {
			x.startElement("definitions", ws);
			x.attribute("targetNamespace", "", service.ns);
			x.startElement("types", ws);
			for (String s : nsPrefix.keySet())
				appendSchema(s);
			x.endElement("types", ws);

			for (Op o : service.operations)
				appendMessage(o);

			appendPortType();
			appendBinding();

			x.startElement("service", ws);
			x.attribute("name", "", service.name);

			x.startElement("port", ws);
			x.attribute("name", "", service.name + "PortType");
			x.attribute("binding", "", name(service.ns, service.name + "Binding"));

			x.startElement("address", wp);
			x.attribute("location", "", address + service.urls[0]);
			x.endElement("address", wp);

			x.endElement("port", ws);
			x.endElement("service", ws);
			x.endElement("definitions", ws);
			x.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	/**
	 * @param sb
	 * @param service
	 * @param s
	 * @throws IOException
	 */
	private void appendSchema(String ns) throws IOException {
		if (xs.equals(ns) || !schemaHasData(service, ns))
			return;
		x.startElement("schema", xs);
		x.attribute("targetNamespace", "", ns);

		Set<SchemaData> set = new HashSet<>();
		for (Service.Op o : service.operations) {
			if (o.result != null)
				appendType(o.result.type, ns, set);
			for (Service.Param p : o.params)
				appendType(p.type, ns, set);

			if (o.paramStyle == ParameterStyle.WRAPPED) {
				if (!ns.equals(o.ns))
					continue;
				x.startElement("element", xs);
				x.attribute("name", "", o.name);
				x.startElement("complexType", xs);
				x.startElement("sequence", xs);
				for (Param p : o.params) {
					x.startElement("element", xs);
					x.attribute("name", "", p.name);
					x.attribute("type", "", name(p.type));
					x.endElement("element", xs);
				}
				x.endElement("sequence", xs);
				x.endElement("complexType", xs);
				x.endElement("element", xs);

				x.startElement("element", xs);
				x.attribute("name", "", o.name + "Response");
				x.startElement("complexType", xs);
				x.startElement("sequence", xs);
				if (o.result != null) {
					x.startElement("element", ns);
					x.attribute("name", "", o.result.name);
					x.attribute("type", "", name(o.result.type));
					x.endElement("element", xs);
				}
				// TODO out param
				x.endElement("sequence", xs);
				x.endElement("complexType", xs);
				x.endElement("element", xs);
			} else {
				for (Service.Param p : o.params) {
					if (!ns.equals(p.ns))
						continue;
					x.startElement("element", xs);
					x.attribute("name", "", p.name);
					x.attribute("type", "", name(p.ns, p.name));
					x.endElement("element", xs);
				}
				if (o.result != null && ns.equals(o.result.ns)) {
					x.startElement("element", xs);
					x.attribute("name", "", o.result.name);
					x.attribute("type", "", name(o.result.type));
					x.endElement("element", xs);
				}
			}
		}
		x.endElement("schema", xs);
	}

	/**
	 * @param type
	 * @param nsPrefix
	 * @param ns
	 * @throws IOException
	 */
	private void appendType(XmlType type, String ns, Set<SchemaData> processed) throws IOException {
		SchemaData s = type.schema();
		if (processed.contains(s))
			return;
		processed.add(s);
		if (ns.equals(s.rootNs)) {
			x.startElement("element", xs);
			x.attribute("name", "", s.rootElement);
			x.attribute("type", "", name(type));
			x.endElement("element", xs);
		}
		if (!ns.equals(s.ns))
			return;

		if (type instanceof XmlEnum) {
			x.startElement("simpleType", xs);
			x.attribute("name", "", s.name);
			x.startElement("restriction", xs);
			x.attribute("base", "", name(xs, "string"));
			for (XmlEnumEntry e : ((XmlEnum) type).entries) {
				x.startElement("enumeration", xs);
				x.attribute("value", "", e.value);
				x.endElement("enumeration", xs);
			}
			x.endElement("restriction", xs);
			x.endElement("simpleType", xs);
		} else if (type instanceof XmlObject) {
			XmlObject o = (XmlObject) type;
			x.startElement("complexType", xs);
			x.attribute("name", "", s.name);
			if (o.elems.isEmpty() && o.value != null) {
				x.startElement("simpleContent", xs);
				x.startElement("extension", xs);
				x.attribute("base", "", name(o.value.type));
				for (XmlField e : o.attrs) { // TODO list ?
					x.startElement("attribute", xs);
					x.attribute("name", "", e.name);
					x.attribute("type", "", name(e.type));
					x.endElement("attribute", xs);
				}
				x.endElement("extension", xs);
				x.endElement("simpleType", xs);
			} else {
				x.startElement("sequence", xs);
				for (XmlField e : o.elems) {// TODO list
					x.startElement("element", xs);
					x.attribute("name", "", e.name);
					x.attribute("type", "", name(e.type));
					x.endElement("element", xs);
				}
				x.endElement("sequence", xs);
				for (XmlField e : o.attrs) {// TODO list ?
					x.startElement("attribute", xs);
					x.attribute("name", "", e.name);
					x.attribute("type", "", name(e.type));
					x.endElement("attribute", xs);
				}
			}
			x.endElement("complexType", xs);
			if (o.value != null)
				appendType(o.value.type, ns, processed);
			for (XmlField e : o.elems)
				appendType(e.type, ns, processed);
			for (XmlField e : o.attrs)
				appendType(e.type, ns, processed);
		}
	}

	/**
	 * @param sb
	 * @param o
	 * @throws IOException
	 */
	private void appendMessage(Op o) throws IOException {

		if (o.paramStyle == ParameterStyle.WRAPPED || o.params.size() > 0) {
			x.startElement("message", ws);
			x.attribute("name", "", o.name);
			x.startElement("part", ws);
			x.attribute("name", "", "param");
			x.attribute("element", "", name(o.ns, o.name));
			x.endElement("part", ws);
			x.endElement("message", ws);
			x.startElement("message", ws);
			x.attribute("name", "", o.name + "Response");
			x.startElement("part", ws);
			x.attribute("name", "", "param");
			x.attribute("element", "", name(o.ns, o.name + "Response"));
			x.endElement("part", ws);
			x.endElement("message", ws);
		} else {
			for (Param p : o.params) {
				x.startElement("part", ws);
				x.attribute("name", "", p.name);
				x.attribute("element", "", name(p.type));
				x.endElement("part", ws);
			}
			x.endElement("message", ws);
			if (o.result != null) {
				x.startElement("message", ws);
				x.attribute("name", "", o.name + "Response");
				x.startElement("part", ws);
				x.attribute("name", "", "param");
				x.attribute("element", "", name(o.result.ns, o.result.name));
				x.endElement("part", ws);
				x.endElement("message", ws);
			}
		}
	}

	private void appendPortType() throws IOException {
		x.startElement("portType", ws);
		x.attribute("name", "", service.name + "PortType");
		for (Op o : service.operations) {
			x.startElement("operation", ws);
			x.attribute("name", "", o.name);
			if (o.paramStyle == ParameterStyle.WRAPPED || !o.params.isEmpty()) {
				x.startElement("input", ws);
				x.attribute("name", "", o.name);
				x.attribute("message", "", name(service.ns, o.name));
				x.endElement("input", ws);
			}
			if (o.paramStyle == ParameterStyle.WRAPPED || o.result != null) {
				x.startElement("output", ws);
				x.attribute("name", "", o.name + "Response");
				x.attribute("message", "", name(service.ns, o.name) + "Response");
				x.endElement("output", ws);
			}
			x.endElement("operation", ws);
		}
		x.endElement("portType", ws);
	}

	private void appendBinding() throws IOException {
		x.startElement("binding", ws);
		x.attribute("name", "", service.name + "Binding");
		x.attribute("type", "", name(service.ns, service.name + "PortType"));

		x.startElement("binding", wp);
		x.attribute("style", "", "document");
		x.attribute("transport", "", "http://schemas.xmlsoap.org/soap/http");
		x.endElement("binding", wp);

		for (Op o : service.operations) {
			x.startElement("operation", ws);
			x.attribute("name", "", o.name);
			x.startElement("operation", wp);
			x.attribute("soapAction", "", o.action);
			x.endElement("operation", wp);
			if (o.paramStyle == ParameterStyle.WRAPPED || !o.params.isEmpty()) {
				x.startElement("input", ws);
				x.attribute("name", "", o.name);
				x.startElement("body", wp);
				x.attribute("use", "", "literal");
				x.endElement("body", wp);
				x.endElement("input", ws);
			}
			if (o.paramStyle == ParameterStyle.WRAPPED || o.result != null) {
				x.startElement("output", ws);
				x.attribute("name", "", o.name + "Response");
				x.startElement("body", wp);
				x.attribute("use", "", "literal");
				x.endElement("body", wp);
				x.endElement("output", ws);
			}
			x.endElement("operation", ws);
		}
		x.endElement("binding", ws);
	}

	private String name(String ns, String name) {
		String n = nsPrefix.get(ns);
		if (n == null || ns.isEmpty())
			return name;
		return n + ':' + name;
	}

	private String name(XmlType type) {
		SchemaData d = type.schema();
		return name(d.ns, d.name);
	}

	private static Map<String, Integer> collectNs(Service service) {
		Map<String, Integer> ns = new HashMap<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				ns.merge(o.ns, 1, Integer::sum);
				for (Service.Param p : o.params)
					ns.merge(p.ns, 1, Integer::sum);
			} else if (o.result != null) {
				ns.merge(o.result.ns, 1, Integer::sum);
				collectNs(o.result.type, ns);
			}
			for (Service.Param p : o.params) {
				collectNs(p.type, ns);
			}
		}
		return ns;
	}

	/**
	 * @param type
	 * @param ns
	 * @param limit
	 */
	private static void collectNs(XmlType type, Map<String, Integer> ns) {
		// TODO loop protection
		SchemaData schema = type.schema();
		ns.merge(schema.ns, 1, Integer::sum);

		if (type instanceof XmlList)
			type = ((XmlList) type).component;
		if (!(type instanceof XmlObject))
			return;

		if (schema.rootNs != null)
			ns.merge(schema.rootNs, 1, Integer::sum);

		XmlObject o = (XmlObject) type;
		for (XmlField e : o.elems) {
			ns.merge(e.ns, 1, Integer::sum);
			collectNs(e.type, ns);
		}
		for (XmlField e : o.attrs) {
			ns.merge(e.ns, 1, Integer::sum);
			collectNs(e.type, ns);
		}
		if (o.value != null)
			collectNs(o.value.type, ns);
	}

	private static boolean schemaHasData(Service service, String ns) {
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				if (ns.equals(o.ns))
					return true;
				for (Service.Param p : o.params) {
					if (ns.equals(p.ns))
						return true;
				}
			} else if (o.result != null && (ns.equals(o.result.ns) || schemaHasData(o.result.type, ns)))
				return true;
			for (Service.Param p : o.params) {
				if (schemaHasData(p.type, ns))
					return true;
			}
		}
		return false;
	}

	private static boolean schemaHasData(XmlType type, String ns) {
		SchemaData schema = type.schema();
		if (ns.equals(schema.ns))
			return true;

		if (type instanceof XmlList)
			type = ((XmlList) type).component;
		if (!(type instanceof XmlObject))
			return false;

		if (schema.rootNs != null && ns.equals(schema.rootNs))
			return true;

		XmlObject o = (XmlObject) type;
		for (XmlField e : o.elems) {
			if (ns.equals(e.ns) || schemaHasData(e.type, ns))
				return true;
		}
		for (XmlField e : o.attrs) {
			if (ns.equals(e.ns) || schemaHasData(e.type, ns))
				return true;
		}
		if (o.value != null && schemaHasData(o.value.type, ns))
			return true;
		return false;
	}

}
