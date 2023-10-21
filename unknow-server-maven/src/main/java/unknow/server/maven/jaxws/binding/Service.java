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
import jakarta.jws.soap.SOAPBinding.Use;
import unknow.server.jaxws.UrlMapping;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model_xml.XmlLoader;

/**
 * @author unknow
 */
public class Service {
	/** target name */
	public final String name;

	public final String portType;
	public final String portName;
	/** target namespace */
	public final String ns;

	public final String[] urls;

	public final ParameterStyle paramStyle;

	public String postConstruct;
	public String preDestroy;

	public final List<Operation> operations = new ArrayList<>();

	private Service(String name, String portType, String portName, String ns, String[] urls, ParameterStyle paramStyle) {
		this.name = name;
		this.portType = portType;
		this.portName = portName;
		this.ns = ns;
		this.urls = urls;
		this.paramStyle = paramStyle;
	}

	public static Service build(TypeDeclaration<?> serviceClass, String basePath, ModelLoader loader, XmlLoader xmlLoader) {
		TypeModel clazz = loader.get(serviceClass.resolve().getQualifiedName());
		AnnotationModel ws = clazz.annotation(WebService.class).get();
		String name = ws.member("serviceName").map(v -> v.asLiteral()).orElse(serviceClass.getNameAsString() + "Service");
		String portType = ws.member("name").map(v -> v.asLiteral()).orElse(serviceClass.resolve().getClassName());
		String portName = ws.member("portName").map(v -> v.asLiteral()).orElse(portType + "Port");
		String ns = ws.member("targetNamespace").map(v -> v.asLiteral()).orElse(serviceClass.resolve().getPackageName());

		String[] url = clazz.annotation(UrlMapping.class).flatMap(a -> a.value()).map(v -> v.asArrayLiteral()).orElse(new String[] { basePath + name });

		Optional<AnnotationModel> a = clazz.annotation(SOAPBinding.class);
		Style style = a.flatMap(v -> v.member("style")).map(v -> v.asEnum(Style.class)).orElse(Style.DOCUMENT);
		if (style != Style.DOCUMENT)
			throw new RuntimeException("Only Document style is managed " + clazz);
		Use use = a.flatMap(v -> v.member("use")).map(v -> v.asEnum(Use.class)).orElse(Use.LITERAL);
		if (use != Use.LITERAL)
			throw new RuntimeException("Only literal use is managed " + clazz);

		ParameterStyle paramStyle = a.flatMap(v -> v.member("parameterStyle")).map(s -> s.asEnum(ParameterStyle.class)).orElse(ParameterStyle.WRAPPED);

		Service service = new Service(name, portType, portName, ns, url, paramStyle);

		String inter = ws.member("endpointInterface").map(v -> v.asLiteral()).orElse("");
		if (!inter.isEmpty()) {
			clazz = loader.get(inter);
			if (clazz == null)
				throw new RuntimeException("can't find endpointInterface '" + inter + "'");
		}
		service.collectOp(clazz.asClass(), xmlLoader);

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
	 * @param xmlLoader
	 */
	private void collectOp(ClassModel cl, XmlLoader xmlLoader) {
		for (MethodModel m : cl.methods()) {
			Optional<AnnotationModel> a = m.annotation(WebMethod.class);
			if (a.flatMap(v -> v.member("exclude")).map(v -> v.asBoolean()).orElse(false))
				continue;
			String opName = a.flatMap(v -> v.member("operationName")).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(m.name());
			String action = a.flatMap(v -> v.member("action")).map(v -> v.asLiteral()).orElse("");

			a = m.annotation(SOAPBinding.class);
			Style style = a.flatMap(v -> v.member("style")).map(v -> v.asEnum(Style.class)).orElse(Style.DOCUMENT);
			if (style != Style.DOCUMENT)
				throw new RuntimeException("Only Document style is managed " + m);
			Use use = a.flatMap(v -> v.member("use")).map(v -> v.asEnum(Use.class)).orElse(Use.LITERAL);
			if (use != Use.LITERAL)
				throw new RuntimeException("Only literal use is managed " + m);
			ParameterStyle paramStyle = a.flatMap(v -> v.member("parameterStyle")).map(v -> v.asEnum(ParameterStyle.class)).orElse(this.paramStyle);

			Parameter r = null;
			TypeModel type = m.type();
			if (!type.isVoid()) {
				a = m.annotation(WebResult.class);
				boolean h = a.flatMap(v -> v.member("header")).map(v -> v.asBoolean()).orElse(false);
				String name = a.flatMap(v -> v.member("name")).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.BARE ? opName + "Response" : "return");
				String ns = a.flatMap(v -> v.member("targetNamespace")).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle != ParameterStyle.WRAPPED || h ? this.ns : "");

				r = new Parameter(new QName(ns, name), m.type(), xmlLoader.add(m.type()), h);
			}
			List<Parameter> params = new ArrayList<>();
			for (ParamModel<?> p : m.parameters()) {
				a = p.annotation(WebParam.class);
				boolean h = a.flatMap(v -> v.member("header")).map(v -> v.asBoolean()).orElse(false);
				String name = a.flatMap(v -> v.member("name")).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.BARE ? opName : "arg" + p.index());
				String ns = a.flatMap(v -> v.member("targetNamespace")).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.WRAPPED && !h ? "" : this.ns);
				params.add(new Parameter(new QName(ns, name), p.type(), xmlLoader.add(p.type()), h));
			}
			operations.add(new Operation(m.name(), new QName(ns, opName), paramStyle == ParameterStyle.WRAPPED, action, params, r));
		}
	}
}
