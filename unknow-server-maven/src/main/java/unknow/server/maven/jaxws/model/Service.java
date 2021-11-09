/**
 * 
 */
package unknow.server.maven.jaxws.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import unknow.server.jaxws.UrlPattern;

/**
 * @author unknow
 */
public class Service {
	private static final AnnotationExpr SOAPBINDING = new NormalAnnotationExpr().addPair("style", new NameExpr("DOCUMENT")).addPair("parameterStyle", new NameExpr("WRAPPED"));

	/** target name */
	public final String name;
	/** target namespace */
	public final String ns;

	public final String[] urls;

	public final Style style;
	public final ParameterStyle paramStyle;

	public final List<Op> operations = new ArrayList<>();

	private Service(String name, String ns, String[] urls, Style style, ParameterStyle paramStyle) {
		this.name = name;
		this.ns = ns;
		this.urls = urls;
		this.style = style;
		this.paramStyle = paramStyle;
	}

	public static Service build(ClassOrInterfaceDeclaration serviceClass, XmlTypeLoader typeLoader) {
		AnnotationExpr a = serviceClass.getAnnotationByClass(WebService.class).get();
		String name = a.findFirst(MemberValuePair.class, m -> "name".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(serviceClass.resolve().getClassName());
		String ns = a.findFirst(MemberValuePair.class, m -> "targetNamespace".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(serviceClass.resolve().getPackageName());

		Optional<AnnotationExpr> o = serviceClass.getAnnotationByClass(UrlPattern.class);
		String[] url = { name };
		if (o.isPresent()) {
			a = o.get();
			Expression e;
			if (a.isSingleMemberAnnotationExpr())
				e = a.asSingleMemberAnnotationExpr().getMemberValue();
			else
				e = a.findFirst(MemberValuePair.class).get().getValue();
			if (e.isStringLiteralExpr())
				url[0] = e.asStringLiteralExpr().asString();
			else {
				NodeList<Expression> values = e.asArrayInitializerExpr().getValues();
				url = new String[values.size()];
				for (int i = 0; i < url.length; i++)
					url[i] = values.get(i).asStringLiteralExpr().asString();
			}
		}

		o = serviceClass.getAnnotationByClass(SOAPBinding.class);
		Style style = SOAPBinding.Style.valueOf(o.orElse(SOAPBINDING).findFirst(MemberValuePair.class, m -> "style".equals(m.getNameAsString())).map(m -> styleName(m)).get());
		ParameterStyle paramStyle = SOAPBinding.ParameterStyle.valueOf(o.orElse(SOAPBINDING).findFirst(MemberValuePair.class, m -> "parameterStyle".equals(m.getNameAsString())).map(m -> styleName(m)).get());
		Service service = new Service(name, ns, url, style, paramStyle);

		String inter = a.findFirst(MemberValuePair.class, m -> "endpointInterface".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(null);
		if (inter != null) {
			serviceClass = typeLoader.classes.get(inter);
			if (serviceClass != null)
				service.collectOp(service, serviceClass, typeLoader);
			else // TODO load from classpath
				throw new RuntimeException("can't find in source");
		} else
			service.collectOp(service, serviceClass, typeLoader);
		return service;
	}

	/**
	 * @param cl
	 * @param typeLoader
	 */
	private void collectOp(Service service, ClassOrInterfaceDeclaration cl, XmlTypeLoader typeLoader) {
		for (MethodDeclaration m : cl.getMethods()) {
			Optional<AnnotationExpr> o = m.getAnnotationByClass(WebMethod.class);
			if (!o.isPresent())
				continue;
			MemberValuePair orElse = o.get().findFirst(MemberValuePair.class, v -> "exclude".equals(v.getNameAsString())).orElse(null);
			if (orElse != null && orElse.getValue().asBooleanLiteralExpr().getValue())
				continue;
			String name = o.get().findFirst(MemberValuePair.class, v -> "operationName".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse(m.getNameAsString());

			Style style = service.style;
			ParameterStyle paramStyle = service.paramStyle;
			o = m.getAnnotationByClass(SOAPBinding.class);
			if (o.isPresent()) {
				style = o.get().findFirst(MemberValuePair.class, v -> "style".equals(v.getNameAsString())).map(v -> Style.valueOf(styleName(v))).orElse(style);
				paramStyle = o.get().findFirst(MemberValuePair.class, v -> "parameterStyle".equals(v.getNameAsString())).map(v -> ParameterStyle.valueOf(styleName(v))).orElse(paramStyle);
			}

			o = m.getAnnotationByClass(WebResult.class);
			String rns = "";
			String rname = "";
			boolean header = false;
			if (o.isPresent()) {
				header = o.get().findFirst(MemberValuePair.class, v -> "header".equals(v.getNameAsString())).map(v -> v.getValue().asBooleanLiteralExpr().getValue()).orElse(false);
				rname = o.get().findFirst(MemberValuePair.class, v -> "name".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse("");
				rns = o.get().findFirst(MemberValuePair.class, v -> "targetNamespace".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse("");
			}

			if (rname.isEmpty())
				rname = style == Style.DOCUMENT && paramStyle == ParameterStyle.BARE ? name + "Response" : "return";
			if (rns.isEmpty() && (style != Style.DOCUMENT || paramStyle != ParameterStyle.WRAPPED || header))
				rns = service.ns;

			Param r = new Param(rns, rname, typeLoader.get(m.getType()), m.resolve().getQualifiedName(), header);
			Op op = new Op(m.getNameAsString(), name, ns, r, style, paramStyle);
			for (Parameter p : m.getParameters()) {
				String ns = "##default";
				name = "##default";
				XmlType type = typeLoader.get(p.getType());
				header = false;
				Optional<AnnotationExpr> oa = p.getAnnotationByClass(WebParam.class);
				if (oa.isPresent()) {
					header = oa.get().findFirst(MemberValuePair.class, v -> "header".equals(v.getNameAsString())).map(v -> v.getValue().asBooleanLiteralExpr().getValue()).orElse(false);
					name = oa.get().findFirst(MemberValuePair.class, v -> "name".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse("##default");
					ns = oa.get().findFirst(MemberValuePair.class, v -> "targetNamespace".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse("##default");
				}

				if ("##default".equals(name))
					name = paramStyle == ParameterStyle.BARE ? op.name : "arg" + op.params.size();
				if ("##default".equals(ns))
					ns = paramStyle == ParameterStyle.WRAPPED && !header ? "" : this.ns;

				op.params.add(new Param(ns, name, type, p.getType().resolve().describe(), header));
			}
			operations.add(op);
		}
	}

	private static String styleName(MemberValuePair m) {
		Expression e = m.getValue();
		if (e.isNameExpr())
			return e.asNameExpr().getNameAsString();
		return e.asFieldAccessExpr().getNameAsString();
	}

	public static class Op {
		public final String m;
		public final String name;
		public final String ns;
		public final Style style;
		public final ParameterStyle paramStyle;
		public final List<Param> params;
		public final Param result;

		public Op(String m, String name, String ns, Param result, Style style, ParameterStyle paramStyle) {
			this.m = m;
			this.name = name;
			this.ns = ns;
			this.params = new ArrayList<>();
			this.result = result;
			this.style = style;
			this.paramStyle = paramStyle;
		}

		public String sig() {
			if (paramStyle == ParameterStyle.WRAPPED)
				return (ns.isEmpty() ? "" : '{' + ns + '}') + name;
			StringBuilder sb = new StringBuilder();
			for (Param p : params) {
				if (p.header)
					sb.append(p.type.binaryName());
			}
			sb.append('#');
			for (Param p : params) {
				if (!p.header)
					sb.append(p.type.binaryName());
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			return "Operation: " + name + " " + params;
		}
	}

	public static class Param {
		public final String ns;
		public final String name;
		public final XmlType type;
		public final String clazz;
		public final boolean header;

		public Param(String ns, String name, XmlType type, String clazz, boolean header) {
			this.ns = ns;
			this.name = name;
			this.type = type;
			this.clazz = clazz;
			this.header = header;
		}

		@Override
		public String toString() {
			return (ns == null ? "" : '{' + ns + '}') + name + " " + type + " " + header;
		}
	}
}
