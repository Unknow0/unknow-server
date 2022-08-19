/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.ArrayList;
import java.util.List;

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
import unknow.server.jaxws.UrlPattern;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
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

	public final List<Op> operations = new ArrayList<>();

	private Service(String name, String ns, String[] urls, Style style, ParameterStyle paramStyle) {
		this.name = name;
		this.ns = ns;
		this.urls = urls;
		this.style = style;
		this.paramStyle = paramStyle;
	}

	public static Service build(TypeDeclaration<?> serviceClass, ModelLoader loader, XmlTypeLoader typeLoader) {
		TypeModel clazz = loader.get(serviceClass.resolve().getQualifiedName());
		AnnotationModel ws = clazz.annotation(WebService.class);
		String name = ws.value("name").orElse(serviceClass.resolve().getClassName());
		String ns = ws.value("targetNamespace").orElse(serviceClass.resolve().getPackageName());

		AnnotationModel a = clazz.annotation(UrlPattern.class);
		String[] url = { "/" + name };
		if (a != null)
			url = a.values("value").orElse(url);

		Style style = Style.DOCUMENT;
		ParameterStyle paramStyle = ParameterStyle.WRAPPED;
		a = clazz.annotation(SOAPBinding.class);
		if (a != null) {
			style = a.value("style").map(s -> SOAPBinding.Style.valueOf(s)).orElse(SOAPBinding.Style.DOCUMENT);
			paramStyle = a.value("parameterStyle").map(s -> SOAPBinding.ParameterStyle.valueOf(s)).orElse(SOAPBinding.ParameterStyle.WRAPPED);
		}
		Service service = new Service(name, ns, url, style, paramStyle);

		String inter = ws.value("endpointInterface").orElse(null);
		if (inter != null) {
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
	private void collectOp(ClassModel cl, XmlTypeLoader typeLoader) {
		for (MethodModel m : cl.methods()) {
			AnnotationModel o = m.annotation(WebMethod.class);
			if (o == null)
				continue;
			if (o.value("exclude").map(Boolean::parseBoolean).orElse(false))
				continue;
			String name = o.value("operationName").orElse(m.name());
			String action = o.value("action").orElse("");

			Style style = this.style;
			ParameterStyle paramStyle = this.paramStyle;
			o = m.annotation(SOAPBinding.class);
			if (o != null) {
				style = o.value("style").map(s -> SOAPBinding.Style.valueOf(s)).orElse(style);
				paramStyle = o.value("parameterStyle").map(s -> SOAPBinding.ParameterStyle.valueOf(s)).orElse(paramStyle);
			}

			Param r = null;
			TypeModel type = m.type();
			if (!type.isVoid()) {
				o = m.annotation(WebResult.class);
				String rns = "";
				String rname = "";
				boolean header = false;
				if (o != null) {
					header = o.value("header").map(Boolean::parseBoolean).orElse(false);
					rname = o.value("name").orElse("");
					rns = o.value("targetNamespace").orElse("");
				}

				if (rname.isEmpty())
					rname = style == Style.DOCUMENT && paramStyle == ParameterStyle.BARE ? name + "Response" : "return";
				if (rns.isEmpty() && (style != Style.DOCUMENT || paramStyle != ParameterStyle.WRAPPED || header))
					rns = this.ns;

				r = new Param(rns, rname, typeLoader.get(m.type()), type.name(), header);
			}
			Op op = new Op(m.name(), name, ns, r, action, style, paramStyle);
			for (ParamModel p : m.parameters()) {
				String ns = "##default";
				name = "##default";
				XmlType<?> t = typeLoader.get(p.type());
				boolean header = false;
				AnnotationModel oa = p.annotation(WebParam.class);
				if (oa != null) {
					header = oa.value("header").map(Boolean::parseBoolean).orElse(false);
					name = oa.value("name").orElse("##default");
					ns = oa.value("targetNamespace").orElse("##default");
				}

				if ("##default".equals(name))
					name = paramStyle == ParameterStyle.BARE ? op.name : "arg" + op.params.size();
				if ("##default".equals(ns))
					ns = paramStyle == ParameterStyle.WRAPPED && !header ? "" : this.ns;

				op.params.add(new Param(ns, name, t, p.type().name(), header));
			}
			operations.add(op);
		}
	}

	public static class Op {
		public final String m;
		public final String name;
		public final String ns;
		public final String action;
		public final Style style;
		public final ParameterStyle paramStyle;
		public final List<Param> params;
		public final Param result;

		public Op(String m, String name, String ns, Param result, String action, Style style, ParameterStyle paramStyle) {
			this.m = m;
			this.name = name;
			this.ns = ns;
			this.params = new ArrayList<>();
			this.result = result;
			this.action = action;
			this.style = style;
			this.paramStyle = paramStyle;
		}

		public String sig() {
			if (paramStyle == ParameterStyle.WRAPPED)
				return (ns.isEmpty() ? "" : '{' + ns + '}') + name;
			StringBuilder sb = new StringBuilder();
			for (Param p : params) {
				if (p.header)
					sb.append(p.type().javaType().name()).append(';');
			}
			sb.append('#');
			for (Param p : params) {
				if (!p.header)
					sb.append(p.type().javaType().name()).append(';');
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			return "Operation: " + name + " " + params;
		}
	}

	public static class Param extends XmlField<XmlType<?>> {
		public final String clazz;
		public final boolean header;

		public Param(String ns, String name, XmlType<?> type, String clazz, boolean header) {
			super(type, ns, name, "", "");
			this.clazz = clazz;
			this.header = header;
		}

		@Override
		public String toString() {
			return qname() + " " + type() + " " + header;
		}
	}
}
