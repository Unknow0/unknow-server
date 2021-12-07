/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

	public WsdlBuilder() {
//   ................................................
//</definitions>");
	}

	public static byte[] build(Service service, String address) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter sb = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		try {
			sb.append("<ws:definitions targetNamespace=\"").append(service.ns).write(
					"\" xmlns:ws=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wp=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><ws:types>");
			appendSchemas(sb, service);
			sb.write("</ws:types>");

			for (Op o : service.operations)
				appendMessage(sb, o);

			appendPortType(sb, service);
			appendBinding(sb, service);

			sb.append("<ws:service name=\"").append(service.name).write("\">");
			sb.append("<ws:port name=\"").append(service.name).append("PortType\" binding=\"").append(service.name).write("Binding\">");
			sb.append("<wp:address location=\"").append(address).append(service.urls[0]).write("\"/>");
			sb.write("</ws:port></ws:service></ws:definitions>");
			sb.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	/**
	 * @param sb
	 * @param service
	 * @throws IOException
	 */
	private static void appendSchemas(Writer sb, Service service) throws IOException {
		Map<String, Integer> ns = collectNs(service, null);
		ns.remove("http://www.w3.org/2001/XMLSchema");
		for (String s : ns.keySet())
			appendSchema(sb, service, s);
	}

	private static Map<String, Integer> collectNs(Service service, String limit) {
		Map<String, Integer> ns = new HashMap<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED && (limit == null || o.ns.equals(limit))) {
				ns.merge(o.ns, 1, Integer::sum);
				for (Service.Param p : o.params)
					ns.merge(p.ns, 1, Integer::sum);
			} else if (limit == null || o.result.ns.equals(limit)) {
				ns.merge(o.result.ns, 1, Integer::sum);
				collectNs(o.result.type, ns, limit);
			}
			for (Service.Param p : o.params) {
				collectNs(p.type, ns, limit);
			}
		}
		return ns;
	}

	/**
	 * @param type
	 * @param ns
	 * @param limit
	 */
	private static void collectNs(XmlType type, Map<String, Integer> ns, String limit) {
		// TODO avoid loop in collection

		SchemaData schema = type.schema();
		ns.merge(schema.ns, 1, Integer::sum);

		if (type instanceof XmlList)
			type = ((XmlList) type).component;
		if (!(type instanceof XmlObject))
			return;

		if (limit != null && !limit.equals(schema.rootNs))
			return;
		if (schema.rootNs != null)
			ns.merge(schema.rootNs, 1, Integer::sum);

		XmlObject o = (XmlObject) type;
		for (XmlField e : o.elems) {
			ns.merge(e.ns, 1, Integer::sum);
			collectNs(e.type, ns, limit);
		}
		for (XmlField e : o.attrs) {
			ns.merge(e.ns, 1, Integer::sum);
			collectNs(e.type, ns, limit);
		}
		if (o.value != null)
			collectNs(o.value.type, ns, limit);
	}

	/**
	 * @param sb
	 * @param service
	 * @param s
	 * @throws IOException
	 */
	private static void appendSchema(Writer sb, Service service, String ns) throws IOException {
		Map<String, Integer> collectNs = collectNs(service, ns);
		Map<String, String> nsPrefix = XMLNsCollector.buildNsMapping(collectNs);
		nsPrefix.put("http://www.w3.org/2001/XMLSchema", "xs");

		sb.append("<xs:schema targetNamespace=\"").append(ns).write("\"");
		for (Entry<String, String> e : nsPrefix.entrySet()) {
			String n = e.getValue();
			sb.write(" xmlns");
			if (!n.isEmpty())
				sb.append(':').write(n);
			sb.append("=\"").append(e.getKey()).write('"');
		}
		sb.write('>');

		Set<SchemaData> set = new HashSet<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED && o.ns.equals(ns)) {
				sb.append("<xs:complexType name=\"").append(o.name).write("\"><xs:sequence>");
				for (Param p : o.params)
					sb.append("<xs:element name=\"").append(p.name).append("\" type=\"").append(getType(p.type, nsPrefix)).write("\"/>");
				sb.append("</xs:sequence></xs:complexType><xs:complexType name=\"").append(o.name + "Response").write("\"><xs:sequence>");
				sb.append("<xs:element name=\"").append(o.result.name).append("\" type=\"").append(getType(o.result.type, nsPrefix)).write("\"/>");
				sb.write("</xs:sequence></xs:complexType>");
			}
			for (Service.Param p : o.params)
				appendType(sb, p.type, nsPrefix, ns, set);
		}
		sb.write("</xs:schema>");
	}

	/**
	 * @param type
	 * @param nsPrefix
	 * @param ns
	 * @throws IOException
	 */
	private static void appendType(Writer sb, XmlType type, Map<String, String> nsPrefix, String ns, Set<SchemaData> processed) throws IOException {
		SchemaData s = type.schema();
		if (processed.contains(s))
			return;
		processed.add(s);
		if (ns.equals(s.rootNs))
			sb.append("<xs:element name=\"").append(s.rootElement).append("\" type=\"").append(getType(type, nsPrefix)).write("\"/>");
		if (!ns.equals(s.ns))
			return;

		if (type instanceof XmlEnum) {
//			((XmlEnum)type).clazz // TODO
			sb.append("<xs:simpleType name=\"").append(s.name).write("\"><xs:restriction base=\"xs:string\">");
			for (XmlEnumEntry e : ((XmlEnum) type).entries)
				sb.append("<xs:enumeration value=\"").append(e.value).write("\"/>");
			sb.write("</xs:restriction></xs:simpleType>");
		} else if (type instanceof XmlObject) {
			XmlObject o = (XmlObject) type;
			sb.append("<xs:complexType name=\"").write(s.name);
			if (o.elems.isEmpty() && o.value != null) {
				sb.append("\"><xs:simpleContent><xs:extension base=\"").append(getType(o.value.type, nsPrefix)).write("\">");
				for (XmlField e : o.attrs) // TODO list ?
					sb.append("<xs:attribute name=\"").append(e.name).append("\" type=\"").append(getType(o.value.type, nsPrefix)).write("\"/>");
				sb.write("</xs:extension></xs:simpleType>");
			} else {
				if (o.value != null)
					sb.write("\" mixed=\"true");
				sb.append("\"><xs:sequence>");
				for (XmlField e : o.elems) // TODO list
					sb.append("<xs:element name=\"").append(e.name).append("\" type=\"").append(getType(e.type, nsPrefix)).write("\"/>");
				sb.write("</xs:sequence>");
				for (XmlField e : o.attrs) // TODO list ?
					sb.append("<xs:attribute name=\"").append(e.name).append("\" type=\"").append(getType(e.type, nsPrefix)).write("\"/>");
			}
			sb.write("</xs:complexType>");
			if (o.value != null)
				appendType(sb, o.value.type, nsPrefix, ns, processed);
			for (XmlField e : o.elems)
				appendType(sb, e.type, nsPrefix, ns, processed);
			for (XmlField e : o.attrs)
				appendType(sb, e.type, nsPrefix, ns, processed);
		}
	}

	/**
	 * @param sb
	 * @param o
	 * @throws IOException
	 */
	private static void appendMessage(Writer sb, Op o) throws IOException {
		sb.append("<ws:message name=\"").append(o.name).write("\">");
		if (o.paramStyle == ParameterStyle.WRAPPED)
			sb.append("<ws:part name=\"param\" element=\"").append(o.name).append("\" xmlns=\"").append(o.ns).write("\"/>");
		else {
			for (Param p : o.params)
				sb.append("<ws:part name=\"").append(p.name).append("\" element=\"").append(p.type.schema().name).append("\" xmlns=\"").append(p.type.schema().ns).write("\"/>");
		}
		sb.append("</ws:message><ws:message name=\"").append(o.name).write("Response\">");
		if (o.paramStyle == ParameterStyle.WRAPPED)
			sb.append("<ws:part name=\"param\" element=\"").append(o.name).append("Response\" xmlns=\"").append(o.ns).write("\"/>");
		else
			sb.append("<ws:part name=\"param\" element=\"").append(o.result.name).append("\" xmlns=\"").append(o.result.ns).write("\"/>");
		sb.write("</ws:message>");
	}

	private static void appendPortType(Writer sb, Service service) throws IOException {
		sb.append("<ws:portType name=\"").append(service.name).write("PortType\">");
		for (Op o : service.operations) {
			sb.append("<ws:operation name=\"").append(o.name).write("\">");
			if (o.paramStyle == ParameterStyle.WRAPPED || o.params.size() > 0)
				sb.append("<ws:input name=\"").append(o.name).append("\" message=\"").append(o.name).write("\"/>");
			if (o.result != null)
				sb.append("<ws:output name=\"").append(o.name).append("Response\" message=\"").append(o.name).write("Response\"/>");
			sb.write("</ws:operation>");
		}
		sb.write("</ws:portType>");
	}

	private static void appendBinding(Writer sb, Service service) throws IOException {
		// XXX type tns: ?
		sb.append("<ws:binding name=\"").append(service.name).append("Binding\" type=\"").append(service.name).write("PortType\">");
		sb.write("<wp:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>");
		for (Op o : service.operations) {
			sb.append("<ws:operation name=\"").append(o.name).write("\">");
			sb.append("<wp:operation soapAction=\"").append(o.action).write("\"/>");
			if (o.paramStyle == ParameterStyle.WRAPPED || o.params.size() > 0)
				sb.append("<ws:input name=\"").append(o.name).write("\"><wp:body use=\"literal\"/></ws:input>");
			if (o.result != null)
				sb.append("<ws:output name=\"").append(o.name).write("Response\"><wp:body use=\"literal\"/></ws:output>");
			sb.write("</ws:operation>");
		}
		sb.write("</ws:binding>");
	}

	private static String getType(XmlType type, Map<String, String> ns) {
		SchemaData d = type.schema();
		return ns.get(d.ns) + ":" + d.name;
	}
}
