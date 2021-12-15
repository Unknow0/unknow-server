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
import java.util.Map.Entry;
import java.util.Set;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import unknow.server.jaxws.XMLNsCollector;
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
	private final OutputStreamWriter sb;
	private final Map<String, String> nsPrefix;

	private final String ws;
	private final String wp;
	private final String xs;

	public WsdlBuilder(Service service, String address) {
		this.service = service;
		this.address = address;
		this.out = new ByteArrayOutputStream();
		this.sb = new OutputStreamWriter(out, StandardCharsets.UTF_8);

		Map<String, Integer> collectNs = collectNs(service);
		collectNs.merge("http://schemas.xmlsoap.org/wsdl/", 1, Integer::sum);
		collectNs.merge("http://schemas.xmlsoap.org/wsdl/soap/", 1, Integer::sum);
		nsPrefix = XMLNsCollector.buildNsMapping(collectNs);

		ws = nsPrefix.get("http://schemas.xmlsoap.org/wsdl/");
		wp = nsPrefix.get("http://schemas.xmlsoap.org/wsdl/soap/");
		xs = nsPrefix.get("http://www.w3.org/2001/XMLSchema");
	}

	public byte[] build() {
		try {
			sb.append('<').append(ws).append(":definitions targetNamespace=\"").append(service.ns).write("\"");
			for (Entry<String, String> e : nsPrefix.entrySet())
				sb.append(" xmlns:").append(e.getValue()).append("=\"").append(e.getKey()).write('"');
			sb.append("><").append(ws).write(":types>");
			for (String s : nsPrefix.keySet())
				appendSchema(s);
			sb.append("</").append(ws).append(":types>");

			for (Op o : service.operations)
				appendMessage(o);

			appendPortType();
			appendBinding();

			sb.append('<').append(ws).append(":service name=\"").append(service.name).write("\">");
			sb.append('<').append(ws).append(":port name=\"").append(service.name).append("PortType\" binding=\"").append(name(service.ns, service.name + "Binding"))
					.write("\">");
			sb.append('<').append(wp).append(":address location=\"").append(address).append(service.urls[0]).write("\"/>");
			sb.append("</").append(ws).append(":port></").append(ws).append(":service></").append(ws).write(":definitions>");
			sb.flush();
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
		if ("http://www.w3.org/2001/XMLSchema".equals(ns) || !schemaHasData(service, ns))
			return;
		sb.append('<').append(xs).append(":schema targetNamespace=\"").append(ns).write("\">");

		Set<SchemaData> set = new HashSet<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED && ns.equals(o.ns)) {
				sb.append('<').append(xs).append(":element name=\"").append(o.name).write("\">");
				sb.append('<').append(xs).append(":complexType><").append(xs).write(":sequence>");
				for (Param p : o.params)
					sb.append('<').append(xs).append(":element name=\"").append(p.name).append("\" type=\"").append(name(p.type)).write("\"/>");
				sb.append("</").append(xs).append(":sequence></").append(xs).append(":complexType></").append(xs).write(":element>");

				if (o.result != null) {
					sb.append('<').append(xs).append(":element name=\"").append(o.name).write("Response\">");
					sb.append('<').append(xs).append(":complexType><").append(xs).write(":sequence>");
					sb.append('<').append(xs).append(":element name=\"").append(o.result.name).append("\" type=\"").append(name(o.result.type)).write("\"/>");
					sb.append("</").append(xs).append(":sequence></").append(xs).append(":complexType></").append(xs).write(":element>");
				} else
					sb.append('<').append(xs).append(":element name=\"").append(o.name).write("Response\"/>");
			} else {
				for (Service.Param p : o.params) {
					if (ns.equals(p.ns))
						sb.append('<').append(xs).append(":element name=\"").append(p.name).append("\" type=\"").append(name(p.ns, p.name)).write("\"/>");
				}
				if (o.result != null && ns.equals(o.result.ns))
					sb.append('<').append(xs).append(":element name=\"").append(o.result.name).append("\" type=\"").append(name(o.result.type)).write("\"/>");
			}
			if (o.result != null)
				appendType(o.result.type, ns, set);
			for (Service.Param p : o.params)
				appendType(p.type, ns, set);
		}
		sb.append("</").append(xs).write(":schema>");
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
		if (ns.equals(s.rootNs))
			sb.append('<').append(xs).append(":element name=\"").append(s.rootElement).append("\" type=\"").append(name(type)).write("\"/>");
		if (!ns.equals(s.ns))
			return;

		if (type instanceof XmlEnum) {
//			((XmlEnum)type).clazz // TODO
			sb.append('<').append(xs).append(":simpleType name=\"").append(s.name).append("\"><").append(xs).append(":restriction base=\"").append(xs).write(":string\">");
			for (XmlEnumEntry e : ((XmlEnum) type).entries)
				sb.append('<').append(xs).append(":enumeration value=\"").append(e.value).write("\"/>");
			sb.append("</").append(xs).append(":restriction></").append(xs).write(":simpleType>");
		} else if (type instanceof XmlObject) {
			XmlObject o = (XmlObject) type;
			sb.append('<').append(xs).append(":complexType name=\"").write(s.name);
			if (o.elems.isEmpty() && o.value != null) {
				sb.append("\"><").append(xs).append(":simpleContent><").append(xs).append(":extension base=\"").append(name(o.value.type)).write("\">");
				for (XmlField e : o.attrs) // TODO list ?
					sb.append('<').append(xs).append(":attribute name=\"").append(e.name).append("\" type=\"").append(name(o.value.type)).write("\"/>");
				sb.append("</").append(xs).append(":extension></").append(xs).write(":simpleType>");
			} else {
				if (o.value != null)
					sb.write("\" mixed=\"true");
				sb.append("\"><").append(xs).write(":sequence>");
				for (XmlField e : o.elems) // TODO list
					sb.append('<').append(xs).append(":element name=\"").append(e.name).append("\" type=\"").append(name(e.type)).write("\"/>");
				sb.append("</").append(xs).write(":sequence>");
				for (XmlField e : o.attrs) // TODO list ?
					sb.append('<').append(xs).append(":attribute name=\"").append(e.name).append("\" type=\"").append(name(e.type)).write("\"/>");
			}
			sb.append("</").append(xs).append(":complexType>");
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
		sb.append('<').append(ws).append(":message name=\"").append(o.name).write("\">");
		if (o.paramStyle == ParameterStyle.WRAPPED) {
			sb.append('<').append(ws).append(":part name=\"param\" element=\"").append(name(o.ns, o.name)).write("\"/>");
			sb.append("</").append(ws).append(":message><").append(ws).append(":message name=\"").append(o.name).write("Response\">");
			sb.append('<').append(ws).append(":part name=\"param\" element=\"").append(name(o.ns, o.name + "Response")).write("\"/>");
			sb.append("</").append(ws).write(":message>");
		} else {
			for (Param p : o.params)
				sb.append('<').append(ws).append(":part name=\"").append(p.name).append("\" element=\"").append(name(p.type)).write("\"/>");
			sb.append("</").append(ws).write(":message>");
			if (o.result != null) {
				sb.append('<').append(ws).append(":message name=\"").append(o.result.name).write("\">");
				sb.append('<').append(ws).append(":part name=\"param\" element=\"").append(name(o.result.ns, o.result.name)).write("\"/>");
				sb.append("</").append(ws).write(":message>");
			}
		}
	}

	private void appendPortType() throws IOException {
		sb.append('<').append(ws).append(":portType name=\"").append(service.name).write("PortType\">");
		for (Op o : service.operations) {
			sb.append('<').append(ws).append(":operation name=\"").append(o.name).write("\">");
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				sb.append('<').append(ws).append(":input name=\"").append(o.name).append("\" message=\"").append(name(service.ns, o.name)).write("\"/>");
				sb.append('<').append(ws).append(":output name=\"").append(o.name).append("Response\" message=\"").append(name(service.ns, o.name + "Response")).write("\"/>");
			} else {
				if (o.params.size() > 0)
					sb.append('<').append(ws).append(":input name=\"").append(o.name).append("\" message=\"").append(name(service.ns, o.name)).write("\"/>");
				if (o.result != null)
					sb.append('<').append(ws).append(":output name=\"").append(o.result.name).append("\" message=\"").append(name(service.ns, o.result.name)).write("\"/>");
			}
			sb.append("</").append(ws).write(":operation>");
		}
		sb.append("</").append(ws).write(":portType>");
	}

	private void appendBinding() throws IOException {
		sb.append('<').append(ws).append(":binding name=\"").append(service.name).append("Binding\" type=\"").append(name(service.ns, service.name + "PortType")).write("\">");
		sb.append('<').append(wp).write(":binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>");
		for (Op o : service.operations) {
			sb.append('<').append(ws).append(":operation name=\"").append(o.name).write("\">");
			sb.append('<').append(wp).append(":operation soapAction=\"").append(o.action).write("\"/>");
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				sb.append('<').append(ws).append(":input name=\"").append(o.name).append("\"><").append(wp).append(":body use=\"literal\"/></").append(ws).write(":input>");
				sb.append('<').append(ws).append(":output name=\"").append(o.name).append("Response\"><").append(wp).append(":body use=\"literal\"/></").append(ws).write(":output>");
			} else {
				if (!o.params.isEmpty())
					sb.append('<').append(ws).append(":input name=\"").append(o.name).append("\"><").append(wp).append(":body use=\"literal\"/></").append(ws)
							.write(":input>");
				if (o.result != null)
					sb.append('<').append(ws).append(":output name=\"").append(o.result.name).append("\"><").append(wp).append(":body use=\"literal\"/></").append(ws)
							.write(":output>");
			}
			sb.append("</").append(ws).write(":operation>");
		}
		sb.append("</").append(ws).append(":binding>");
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
