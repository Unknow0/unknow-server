/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import unknow.server.maven.jaxws.model.Service;
import unknow.server.maven.jaxws.model.Service.Op;
import unknow.server.maven.jaxws.model.Service.Param;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;

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
		Set<String> schemaNs = new HashSet<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED)
				schemaNs.add(o.ns);
			for (Service.Param p : o.params)
				collectNs(p.type, schemaNs);
		}

		for (String s : schemaNs)
			buildSchema(sb, service, s);
	}

	/**
	 * @param sb
	 * @param service
	 * @param s
	 * @throws IOException
	 */
	private static void buildSchema(Writer sb, Service service, String ns) throws IOException {
		sb.append("<xs:schema xmlns=\"").append(ns).write("\">");
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED && o.ns.equals(ns))
				appendOperation(sb, o, null);
			for (Service.Param p : o.params) {
				if (!p.ns.equals(ns))
					continue;
				// TODO
			}
		}
		sb.write("</xs:schema>");
	}

	/**
	 * @param sb
	 * @param o
	 * @throws IOException
	 */
	private static void appendOperation(Writer sb, Op o, Map<String, String> ns) throws IOException {
		sb.append("<xs:complexType name=\"").append(o.name).write("\"><xs:sequence>");
		for (Param p : o.params) {
			sb.append("<xs:element name=\"").append(p.name).append("\" type=\"").append(getType(p.type, ns)).write("\"");

			sb.write("/>");
		}
	}

	private static String getType(XmlType type, Map<String, String> ns) {
		if (type == XmlType.XmlBigDecimal || type == XmlType.XmlFloat || type == XmlType.XmlDouble)
			return "xs:decimal";
		if (type == XmlType.XmlBigInteger)
			return "xs:integer";
		if (type == XmlType.XmlBoolean)
			return "xs:boolean";
		if (type == XmlType.XmlByte)
			return "xs:byte";
		if (type == XmlType.XmlShort)
			return "xs:short";
		if (type == XmlType.XmlInt)
			return "xs:int";
		if (type == XmlType.XmlLong)
			return "xs:long";
		if (type == XmlType.XmlString)
			return "xs:string";
		// TODO enum
		XmlObject o = (XmlObject) type;
		throw new RuntimeException("TODO");
	}

	private static void collectNs(XmlType type, Set<String> schemaNs) {
		if (type.isSimple())
			return;
		XmlObject o = (XmlObject) type;
		for (XmlField e : o.attrs) {
			if (!e.ns.isEmpty())
				schemaNs.add(e.ns);
		}
		for (XmlField e : o.elems) {
			schemaNs.add(e.ns);
			collectNs(e.type, schemaNs);
		}
	}
}
