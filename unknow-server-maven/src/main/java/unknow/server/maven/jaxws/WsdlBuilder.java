/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import unknow.server.jaxws.XMLNsCollector;
import unknow.server.jaxws.XMLOutput;
import unknow.server.maven.jaxws.binding.Service;
import unknow.server.maven.jaxws.binding.Service.Op;
import unknow.server.maven.jaxws.binding.Service.Param;
import unknow.server.maven.jaxws.binding.XmlElem;
import unknow.server.maven.jaxws.binding.XmlEnum;
import unknow.server.maven.jaxws.binding.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.binding.XmlObject;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
import unknow.server.maven.jaxws.binding.XmlType;

/**
 * @author unknow
 */
public class WsdlBuilder {
	private static final String XS = "http://www.w3.org/2001/XMLSchema";
	private static final String WS = "http://schemas.xmlsoap.org/wsdl/";
	private static final String WP = "http://schemas.xmlsoap.org/wsdl/soap/";

	private final Service service;
	private final String address;
	private final ByteArrayOutputStream out;
	private final XMLOutput x;
	private final Map<String, String> nsPrefix;

	private final Map<String, Collection<XmlElem<?>>> elems;
	private final Map<String, Collection<XmlType<?>>> types;

	public WsdlBuilder(Service service, String address) {
		this.types = new HashMap<>();
		this.elems = new HashMap<>();
		collectElems(service, t -> {
			Collection<XmlType<?>> c = types.get(t.ns());
			if (c == null)
				types.put(t.ns(), c = new ArrayList<>());
			c.add(t);
		}, e -> {
			Collection<XmlElem<?>> c = elems.get(e.ns());
			if (c == null)
				elems.put(e.ns(), c = new ArrayList<>());
			c.add(e);
		});
		Map<String, Integer> collectNs = new HashMap<>();
		for (Entry<String, Collection<XmlElem<?>>> e : elems.entrySet())
			collectNs.merge(e.getKey(), e.getValue().size(), Integer::sum);
		for (Entry<String, Collection<XmlType<?>>> e : types.entrySet())
			collectNs.merge(e.getKey(), e.getValue().size(), Integer::sum);
		collectNs.merge(XS, 1, Integer::sum);
		collectNs.merge(WS, 1, Integer::sum);
		collectNs.merge(WP, 1, Integer::sum);
		this.nsPrefix = XMLNsCollector.buildNsMapping(collectNs);

		this.service = service;
		this.address = address;
		this.out = new ByteArrayOutputStream();
		this.x = new XMLOutput(new OutputStreamWriter(out, StandardCharsets.UTF_8), nsPrefix);
	}

	public byte[] build() {
		try {
			x.startElement("definitions", WS);
			x.attribute("targetNamespace", "", service.ns);
			x.startElement("types", WS);

			for (String ns : nsPrefix.keySet()) {
				if (XS.equals(ns) || WS.equals(ns) || WP.equals(ns))
					continue;
				x.startElement("schema", XS);
				x.attribute("targetNamespace", "", ns);
				x.attribute("elementFormDefault", "", "qualified");

				for (XmlType<?> t : types.getOrDefault(ns, Collections.emptyList()))
					appendType(t);
				for (XmlElem<?> e : elems.getOrDefault(ns, Collections.emptyList()))
					appendElem(e, ns);

				x.endElement("schema", XS);
			}
			x.endElement("types", WS);

			for (Op o : service.operations)
				appendMessage(o);

			appendPortType();
			appendBinding();

			x.startElement("service", WS);
			x.attribute("name", "", service.name);

			x.startElement("port", WS);
			x.attribute("name", "", service.name + "PortType");
			x.attribute("binding", "", name(service.ns, service.name + "Binding"));

			x.startElement("address", WP);
			x.attribute("location", "", address + service.urls[0]);
			x.endElement("address", WP);

			x.endElement("port", WS);
			x.endElement("service", WS);
			x.endElement("definitions", WS);
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
//	private void appendSchema(String ns) throws IOException {
//		if (XS.equals(ns) || WS.equals(ns) || WP.equals(ns))
//			return;
//		x.startElement("schema", XS);
//		x.attribute("targetNamespace", "", ns);
//		x.attribute("elementFormDefault", "", "qualified");
//
//		Set<XmlType<?>> set = new HashSet<>();
//		for (Service.Op o : service.operations) {
//			if (o.result != null)
//				appendType(o.result.type(), ns, set);
//			for (Service.Param p : o.params)
//				appendType(p.type(), ns, set);
//
//			if (o.paramStyle == ParameterStyle.WRAPPED) {
//				if (!ns.equals(o.ns))
//					continue;
//				x.startElement("element", XS);
//				x.attribute("name", "", o.name);
//				x.startElement("complexType", XS);
//				x.startElement("sequence", XS);
//				for (Param p : o.params) {
//					x.startElement("element", XS);
//					x.attribute("name", "", p.name());
//					x.attribute("type", "", name(p.type()));
//					x.endElement("element", XS);
//				}
//				x.endElement("sequence", XS);
//				x.endElement("complexType", XS);
//				x.endElement("element", XS);
//
//				x.startElement("element", XS);
//				x.attribute("name", "", o.name + "Response");
//				x.startElement("complexType", XS);
//				x.startElement("sequence", XS);
//				if (o.result != null) {
//					x.startElement("element", ns);
//					x.attribute("name", "", o.result.name());
//					x.attribute("type", "", name(o.result.type()));
//					x.endElement("element", XS);
//				}
//				// TODO out param
//				x.endElement("sequence", XS);
//				x.endElement("complexType", XS);
//				x.endElement("element", XS);
//			} else {
//				for (Service.Param p : o.params) {
//					if (!ns.equals(p.ns))
//						continue;
//					x.startElement("element", XS);
//					x.attribute("name", "", p.name());
//					x.attribute("type", "", name(p.ns, p.name()));
//					x.endElement("element", XS);
//				}
//				if (o.result != null && ns.equals(o.result.ns)) {
//					x.startElement("element", XS);
//					x.attribute("name", "", o.result.name());
//					x.attribute("type", "", name(o.result.type()));
//					x.endElement("element", XS);
//				}
//			}
//		}
//		x.endElement("schema", XS);
//	}

	/**
	 * @param type
	 * @param nsPrefix
	 * @param ns
	 * @throws IOException
	 */
//	private void appendType(XmlType<?> type, String ns, Set<XmlType<?>> processed) throws IOException {
//		if (processed.contains(type))
//			return;
//		processed.add(type);
//		if (type instanceof XmlObject)
//			appendType((XmlObject) type, ns, processed);
//		if (!ns.equals(type.ns()))
//			return;
//
//		if (type instanceof XmlEnum) {
//			x.startElement("simpleType", XS);
//			x.attribute("name", "", type.name());
//			x.startElement("restriction", XS);
//			x.attribute("base", "", name(XS, "string"));
//			for (XmlEnumEntry e : ((XmlEnum) type).entries) {
//				x.startElement("enumeration", XS);
//				x.attribute("value", "", e.value);
//				x.endElement("enumeration", XS);
//			}
//			x.endElement("restriction", XS);
//			x.endElement("simpleType", XS);
//		} else if (type instanceof XmlObject) {
//		}
//	}

//	private void appendType(XmlObject o, String ns, Set<XmlType<?>> processed) throws IOException {
//		String tns = o.ns();
//		if (o.value() != null)
//			appendType(o.value().type(), ns, processed);
//		for (XmlField<?> e : o.elems())
//			appendType(e.type(), ns, processed);
//		for (XmlField<?> e : o.attrs())
//			appendType(e.type(), ns, processed);
//
//		if (!ns.equals(tns))
//			return;
//		x.startElement("complexType", XS);
//		x.attribute("name", "", o.name());
//		if (o.elems().isEmpty() && o.value() != null) {
//			x.startElement("simpleContent", XS);
//			x.startElement("extension", XS);
//			x.attribute("base", "", name(o.value().type()));
//			for (XmlField<?> e : o.attrs()) { // TODO list ?
//				x.startElement("attribute", XS);
//				x.attribute("name", "", e.name());
//				x.attribute("type", "", name(e.type()));
//				x.endElement("attribute", XS);
//			}
//			x.endElement("extension", XS);
//			x.endElement("simpleType", XS);
//		} else {
//			x.startElement("sequence", XS);
//			for (XmlField<?> e : o.elems()) { // TODO list
//				appendElem(e, tns);
//			}
//			x.endElement("sequence", XS);
//			for (XmlField<?> e : o.attrs()) {// TODO list ?
//				x.startElement("attribute", XS);
//				x.attribute("name", "", e.name());
//				x.attribute("type", "", name(e.type()));
//				x.endElement("attribute", XS);
//			}
//		}
//		x.endElement("complexType", XS);
//
//	}

	private void appendType(XmlType<?> t) throws IOException {
		if (t instanceof XmlEnum) {
			x.startElement("simpleType", XS);
			x.attribute("name", "", t.name());
			x.startElement("restriction", XS);
			x.attribute("base", "", name(XS, "string"));
			for (XmlEnumEntry e : ((XmlEnum) t).entries) {
				x.startElement("enumeration", XS);
				x.attribute("value", "", e.value);
				x.endElement("enumeration", XS);
			}
			x.endElement("restriction", XS);
			x.endElement("simpleType", XS);
			return;
		}
		if (!(t instanceof XmlObject))
			return;

		XmlObject o = (XmlObject) t;

		x.startElement("complexType", XS);
		x.attribute("name", "", o.name());
		if (o.elems().isEmpty() && o.value() != null) {
			x.startElement("simpleContent", XS);
			x.startElement("extension", XS);
			x.attribute("base", "", name(o.value().type()));
			for (XmlField<?> e : o.attrs()) { // TODO list ?
				x.startElement("attribute", XS);
				x.attribute("name", "", e.name());
				x.attribute("type", "", name(e.type()));
				x.endElement("attribute", XS);
			}
			x.endElement("extension", XS);
			x.endElement("simpleType", XS);
		} else {
			x.startElement("sequence", XS);
			for (XmlField<?> e : o.elems()) // TODO list
				appendElem(e, o.ns());
			x.endElement("sequence", XS);
			for (XmlField<?> e : o.attrs()) {// TODO list ?
				x.startElement("attribute", XS);
				x.attribute("name", "", e.name());
				x.attribute("type", "", name(e.type()));
				x.endElement("attribute", XS);
			}
		}
		x.endElement("complexType", XS);
	}

	private void appendElem(XmlElem<?> e, String ns) throws IOException {
		x.startElement("element", XS);
		if (e.ns().equals(ns)) {
			x.attribute("name", "", e.name());
			x.attribute("type", "", name(e.type()));
		} else
			x.attribute("ref", "", name(e.ns(), e.name()));
		x.endElement("element", XS);
	}

	/**
	 * @param sb
	 * @param o
	 * @throws IOException
	 */
	private void appendMessage(Op o) throws IOException {
		if (o.paramStyle == ParameterStyle.WRAPPED || o.params.size() > 0) {
			x.startElement("message", WS);
			x.attribute("name", "", o.name);
			x.startElement("part", WS);
			x.attribute("name", "", "param");
			x.attribute("element", "", name(o.ns, o.name));
			x.endElement("part", WS);
			x.endElement("message", WS);
			x.startElement("message", WS);
			x.attribute("name", "", o.name + "Response");
			x.startElement("part", WS);
			x.attribute("name", "", "param");
			x.attribute("element", "", name(o.ns, o.name + "Response"));
			x.endElement("part", WS);
			x.endElement("message", WS);
		} else {
			for (Param p : o.params) {
				x.startElement("part", WS);
				x.attribute("name", "", p.name());
				x.attribute("element", "", name(p.type()));
				x.endElement("part", WS);
			}
			x.endElement("message", WS);
			if (o.result != null) {
				x.startElement("message", WS);
				x.attribute("name", "", o.name + "Response");
				x.startElement("part", WS);
				x.attribute("name", "", "param");
				x.attribute("element", "", name(o.result.ns(), o.result.name()));
				x.endElement("part", WS);
				x.endElement("message", WS);
			}
		}
	}

	private void appendPortType() throws IOException {
		x.startElement("portType", WS);
		x.attribute("name", "", service.name + "PortType");
		for (Op o : service.operations) {
			x.startElement("operation", WS);
			x.attribute("name", "", o.name);
			if (o.paramStyle == ParameterStyle.WRAPPED || !o.params.isEmpty()) {
				x.startElement("input", WS);
				x.attribute("name", "", o.name);
				x.attribute("message", "", name(service.ns, o.name));
				x.endElement("input", WS);
			}
			if (o.paramStyle == ParameterStyle.WRAPPED || o.result != null) {
				x.startElement("output", WS);
				x.attribute("name", "", o.name + "Response");
				x.attribute("message", "", name(service.ns, o.name) + "Response");
				x.endElement("output", WS);
			}
			x.endElement("operation", WS);
		}
		x.endElement("portType", WS);
	}

	private void appendBinding() throws IOException {
		x.startElement("binding", WS);
		x.attribute("name", "", service.name + "Binding");
		x.attribute("type", "", name(service.ns, service.name + "PortType"));

		x.startElement("binding", WP);
		x.attribute("style", "", "document");
		x.attribute("transport", "", "http://schemas.xmlsoap.org/soap/http");
		x.endElement("binding", WP);

		for (Op o : service.operations) {
			x.startElement("operation", WS);
			x.attribute("name", "", o.name);
			x.startElement("operation", WP);
			x.attribute("soapAction", "", o.action);
			x.endElement("operation", WP);
			if (o.paramStyle == ParameterStyle.WRAPPED || !o.params.isEmpty()) {
				x.startElement("input", WS);
				x.attribute("name", "", o.name);
				x.startElement("body", WP);
				x.attribute("use", "", "literal");
				x.endElement("body", WP);
				x.endElement("input", WS);
			}
			if (o.paramStyle == ParameterStyle.WRAPPED || o.result != null) {
				x.startElement("output", WS);
				x.attribute("name", "", o.name + "Response");
				x.startElement("body", WP);
				x.attribute("use", "", "literal");
				x.endElement("body", WP);
				x.endElement("output", WS);
			}
			x.endElement("operation", WS);
		}
		x.endElement("binding", WS);
	}

	private String name(String ns, String name) {
		String n = nsPrefix.get(ns);
		if (n == null || n.isEmpty())
			return name;
		return n + ':' + name;
	}

	private String name(XmlType<?> type) {
		return name(type.ns(), type.name());
	}

	private static void collectElems(Service service, Consumer<XmlType<?>> types, Consumer<XmlElem<?>> elems) {
		Set<XmlType<?>> collected = new HashSet<>();
		for (Service.Op o : service.operations) {
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				XmlElem<?> e = new XmlElem<>(new XmlOp(o.ns, o.name, o.params), o.ns, o.name);
				collectElems(e, types, elems, collected);

				List<Param> r = o.result == null ? Collections.emptyList() : Arrays.asList(o.result);
				e = new XmlElem<>(new XmlOp(o.ns, o.name + "Response", r), o.ns, o.name + "Response");
				collectElems(e, types, elems, collected);
			} else {
				if (o.result != null)
					collectElems(o.result, types, elems, collected);
				for (Service.Param p : o.params)
					collectElems(p, types, elems, collected);
			}
		}
	}

	private static void collectElems(XmlElem<?> e, Consumer<XmlType<?>> types, Consumer<XmlElem<?>> elems, Set<XmlType<?>> collected) {
		elems.accept(e);
		collectElems(e.type(), types, elems, collected);
	}

	private static void collectElems(XmlType<?> type, Consumer<XmlType<?>> types, Consumer<XmlElem<?>> elems, Set<XmlType<?>> collected) {
		if (!collected.add(type))
			return;
		types.accept(type);
		if (!(type instanceof XmlObject))
			return;
		XmlObject o = (XmlObject) type;
		for (XmlField<?> e : o.elems()) {
			collectElems(e.type(), types, elems, collected);
			if (!e.ns().equals(o.ns()))
				elems.accept(e);
		}
	}

	private static final class XmlOp extends XmlObject {
		@SuppressWarnings("unchecked")
		public XmlOp(String ns, String name, List<? extends XmlField<?>> elems) {
			super(null, ns, name, null, null);
			this.elems = (List<XmlField<?>>) elems;
			this.attrs = Collections.emptyList();
		}
	}
}
