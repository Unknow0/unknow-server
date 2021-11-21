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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import unknow.server.jaxws.XMLNsCollector;
import unknow.server.maven.jaxws.model.SchemaData;
import unknow.server.maven.jaxws.model.Service;
import unknow.server.maven.jaxws.model.Service.Op;
import unknow.server.maven.jaxws.model.Service.Param;
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

	public static byte[] build(Service service) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter sb = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		try {
			sb.append("<ws:definitions name=\"").append(service.name).write("\" xmlns:ws=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">");
			sb.write("<ws:types>");
			buildSchemas(sb, service);
			sb.write("</ws:types>");

			sb.write("</ws:definitions>");
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
	private static void buildSchemas(Writer sb, Service service) throws IOException {
		Map<String, Integer> ns = collectNs(service, null);

		for (String s : ns.keySet())
			buildSchema(sb, service, s);
	}

	private static Map<String, Integer> collectNs(Service service, String limit) {
		Map<String, Integer> ns = new HashMap<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				ns.merge(o.ns, 1, Integer::sum);
				if (limit != null && !limit.equals(o.ns))
					continue;
			}
			for (Service.Param p : o.params) {
				if (limit != null && !limit.equals(p.ns))
					continue;
				collectNs(p, ns, limit);
			}
		}
		return ns;
	}

	private static void collectNs(Service.Param p, Map<String, Integer> ns, String limit) {
		ns.merge(p.ns, 1, Integer::sum);
		collectNs(p.type, ns, limit);
	}

	/**
	 * @param type
	 * @param ns
	 * @param limit
	 */
	private static void collectNs(XmlType type, Map<String, Integer> ns, String limit) {
		SchemaData schema = type.schema();
		ns.merge(schema.ns, 1, Integer::sum);

		if (type instanceof XmlList)
			type = ((XmlList) type).component;
		if (!(type instanceof XmlObject))
			return;

		if (limit != null && !limit.equals(schema.rootElement))
			return;

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
	private static void buildSchema(Writer sb, Service service, String ns) throws IOException {
		Map<String, Integer> collectNs = collectNs(service, ns);
		Map<String, String> nsPrefix = XMLNsCollector.buildNsMapping(collectNs);
		nsPrefix.put("http://www.w3.org/2001/XMLSchema", "xs");

		sb.write("<xs:schema");
		for (Entry<String, String> e : nsPrefix.entrySet()) {
			String n = e.getValue();
			sb.write(" xmlns");
			if (!n.isEmpty())
				sb.append(':').write(n);
			sb.append("=\"").append(e.getKey()).write('"');
		}
		sb.write('>');
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED && o.ns.equals(ns)) {
				sb.append("<xs:complexType name=\"").append(o.name).write("\"><xs:sequence>");
				for (Param p : o.params)
					sb.append("<xs:element name=\"").append(p.name).append("\" type=\"").append(getType(p.type, nsPrefix)).write("\"/>");
				sb.write("</wx:sequence></xs:complexType>");
			}
			for (Service.Param p : o.params)
				appendType(sb, p.type, nsPrefix, ns);
		}
		sb.write("</xs:schema>");

	}

	/**
	 * @param type
	 * @param nsPrefix
	 * @param ns
	 * @throws IOException
	 */
	private static void appendType(Writer sb, XmlType type, Map<String, String> nsPrefix, String ns) throws IOException {
		SchemaData s = type.schema();
		if (ns.equals(s.rootNs)) {
			sb.append("<xs:element name=\"").append(s.rootElement).write("\" type=\"");
			String n = nsPrefix.get(s.rootNs);
			if (!n.isEmpty())
				sb.append(n).write(':');
			sb.append(s.rootElement).write("\"/>");
		}
		if (!ns.equals(s.ns))
			return;
		
	}

	private static String getType(XmlType type, Map<String, String> ns) {
		SchemaData d = type.schema();
		return ns.get(d.ns) + ":" + d.name;
	}
}
