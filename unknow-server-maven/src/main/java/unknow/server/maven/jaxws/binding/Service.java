/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.jws.soap.SOAPBinding.ParameterStyle;
import jakarta.jws.soap.SOAPBinding.Style;
import unknow.server.jaxws.UrlMapping;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlLoader;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class Service {
	/** target name */
	public final String name;
	/** target namespace */
	public final String ns;

	public final String[] urls;

	public final Style style;
	public final ParameterStyle paramStyle;

	public String postConstruct;
	public String preDestroy;

	public final List<Operation> operations = new ArrayList<>();

	private Service(String name, String ns, String[] urls, Style style, ParameterStyle paramStyle) {
		this.name = name;
		this.ns = ns;
		this.urls = urls;
		this.style = style;
		this.paramStyle = paramStyle;
	}

	public static Service build(TypeDeclaration<?> serviceClass, ModelLoader loader, XmlLoader typeLoader) {
		TypeModel clazz = loader.get(serviceClass.resolve().getQualifiedName());
		AnnotationModel ws = clazz.annotation(WebService.class).get();
		String name = ws.member("name").map(v -> v.asLiteral()).orElse(serviceClass.resolve().getClassName());
		String ns = ws.member("targetNamespace").map(v -> v.asLiteral()).orElse(serviceClass.resolve().getPackageName());

		String[] url = clazz.annotation(UrlMapping.class).flatMap(a -> a.value()).map(v -> v.asArrayLiteral()).orElse(new String[] { "/" + name });

		Style style = clazz.annotation(SOAPBinding.class).flatMap(a -> a.member("style")).map(s -> Style.valueOf(s.asLiteral())).orElse(Style.DOCUMENT);
		ParameterStyle paramStyle = clazz.annotation(SOAPBinding.class).flatMap(a -> a.member("parameterStyle")).map(s -> ParameterStyle.valueOf(s.asLiteral()))
				.orElse(ParameterStyle.WRAPPED);

		Service service = new Service(name, ns, url, style, paramStyle);

		String inter = ws.member("endpointInterface").map(v -> v.asLiteral()).orElse("");
		if (!inter.isEmpty()) {
			clazz = loader.get(inter);
			if (clazz == null)
				throw new RuntimeException("can't find endpointInterface '" + inter + "'");
		}
		service.collectOp(clazz.asClass(), typeLoader);

		for (MethodDeclaration m : serviceClass.getMethods()) {
			if (m.getAnnotationByClass(PostConstruct.class).isPresent()) {
				if (m.getParameters().size() != 0)
					throw new RuntimeException("PostConstruct method can't have parameters on " + clazz.name());
				if (service.postConstruct != null)
					throw new RuntimeException("only one method can be annoted with @PostConstruct on " + clazz.name());
				service.postConstruct = m.getNameAsString();
			}
			if (m.getAnnotationByClass(PreDestroy.class).isPresent()) {
				if (m.getParameters().size() != 0)
					throw new RuntimeException("@PreDestroy method can't have parameters on " + clazz.name());
				if (service.postConstruct != null)
					throw new RuntimeException("only one method can be annoted with @PreDestroy on " + clazz.name());
				service.preDestroy = m.getNameAsString();
			}
		}
		return service;
	}

	/**
	 * @param cl
	 * @param typeLoader
	 */
	private void collectOp(ClassModel cl, XmlLoader typeLoader) {
		for (MethodModel m : cl.methods()) {
			Optional<AnnotationModel> o = m.annotation(WebMethod.class);
			if (o.isEmpty())
				continue;
			if (o.get().member("exclude").map(v -> v.asBoolean()).orElse(false))
				continue;
			String name = o.get().member("operationName").map(v -> v.asLiteral()).orElse(m.name());
			String action = o.get().member("action").map(v -> v.asLiteral()).orElse("");

			Style style = m.annotation(SOAPBinding.class).flatMap(a -> a.member("style")).map(s -> Style.valueOf(s.asLiteral())).orElse(this.style);
			ParameterStyle paramStyle = m.annotation(SOAPBinding.class).flatMap(a -> a.member("parameterStyle")).map(s -> ParameterStyle.valueOf(s.asLiteral()))
					.orElse(this.paramStyle);

			Param r = null;
			TypeModel type = m.type();
			if (!type.isVoid()) {
				boolean header = m.annotation(WebResult.class).flatMap(a -> a.member("header")).map(v -> v.asBoolean()).orElse(false);
				String rname = m.annotation(WebResult.class).flatMap(a -> a.member("name")).map(v -> v.asLiteral()).orElse("");
				String rns = m.annotation(WebResult.class).flatMap(a -> a.member("targetNamespace")).map(v -> v.asLiteral()).orElse("");

				if (rname.isEmpty())
					rname = style == Style.DOCUMENT && paramStyle == ParameterStyle.BARE ? name + "Response" : "return";
				if (rns.isEmpty() && (style != Style.DOCUMENT || paramStyle != ParameterStyle.WRAPPED || header))
					rns = this.ns;

				r = new Param(rns, rname, typeLoader.add(m.type()), type.name(), header);
			}
//			Operation op = new Operation(m.name(), name, ns, r, action, style, paramStyle);
//			for (ParamModel<?> p : m.parameters()) {
//				XmlType t = typeLoader.add(p.type());
//				boolean header = p.annotation(WebParam.class).flatMap(a -> a.member("header")).map(v -> v.asBoolean()).orElse(false);
//				name = p.annotation(WebParam.class).flatMap(a -> a.member("name")).map(v -> v.asLiteral()).orElse("##default");
//				String ns = p.annotation(WebParam.class).flatMap(a -> a.member("targetNamespace")).map(v -> v.asLiteral()).orElse("##default");
//
//				if ("##default".equals(name))
//					name = paramStyle == ParameterStyle.BARE ? op.name : "arg" + op.params.size();
//				if ("##default".equals(ns))
//					ns = paramStyle == ParameterStyle.WRAPPED && !header ? "" : this.ns;
//
//				op.params.add(new Param(ns, name, t, p.type().name(), header));
//			}
//			operations.add(op);
		}
	}

	public static class Operation {
		public final QName name;
		public final String action;
		public final List<XmlElement> headers;
		public final List<XmlElement> body;
		public final XmlElement result;

		public Operation(QName name, String action, List<XmlElement> headers, List<XmlElement> body, XmlElement result) {
			this.name = name;
			this.result = result;
			this.action = action;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public String toString() {
			return "Operation: " + name;
		}
	}

	public static class Param {
		public final QName name;
		public final XmlType type;
		public final String clazz;
		public final boolean header;

		public Param(String ns, String name, XmlType type, String clazz, boolean header) {
			this.name = new QName(ns, name);
			this.type = type;
			this.clazz = clazz;
			this.header = header;
		}

		@Override
		public String toString() {
			return name + " " + type.type() + " " + header;
		}
	}
}
