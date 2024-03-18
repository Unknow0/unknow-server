/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

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
import unknow.server.jaxws.WebServiceUrl;
import unknow.server.maven.jaxb.model.XmlLoader;
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

	public final String portType;
	public final String portName;
	/** target namespace */
	public final String ns;

	public final String[] urls;

	public final ParameterStyle defaultParamStyle;

	public String postConstruct;
	public String preDestroy;

	public final List<Operation> operations = new ArrayList<>();

	private Service(String name, String portType, String portName, String ns, String[] urls, ParameterStyle paramStyle) {
		this.name = name;
		this.portType = portType;
		this.portName = portName;
		this.ns = ns;
		this.urls = urls;
		this.defaultParamStyle = paramStyle;
	}

	public static Service build(ClassModel clazz, String basePath, ModelLoader loader, XmlLoader xmlLoader) {
		AnnotationModel ws = clazz.annotation(WebService.class).orElse(null);
		String name = ws.member("serviceName").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(clazz.simpleName() + "Service");
		String portType = ws.member("name").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(clazz.simpleName());
		String portName = ws.member("portName").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(portType + "Port");
		String ns = ws.member("targetNamespace").filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(clazz.packageName());

		String[] url = clazz.annotation(WebServiceUrl.class).flatMap(a -> a.value()).filter(v -> v.isSet()).map(v -> v.asArrayLiteral()).orElse(new String[] { name });
		for (int i = 0; i < url.length; i++) {
			String u = url[i];
			if (u.startsWith("/"))
				u = u.substring(1);
			url[i] = basePath + u;
		}

		Optional<AnnotationModel> a = clazz.annotation(SOAPBinding.class);
		Style style = a.flatMap(v -> v.member("style")).filter(v -> v.isSet()).map(v -> v.asEnum(Style.class)).orElse(Style.DOCUMENT);
		if (style != Style.DOCUMENT)
			throw new RuntimeException("Only Document style is managed " + clazz);
		Use use = a.flatMap(v -> v.member("use")).filter(v -> v.isSet()).map(v -> v.asEnum(Use.class)).orElse(Use.LITERAL);
		if (use != Use.LITERAL)
			throw new RuntimeException("Only literal use is managed " + clazz);

		ParameterStyle paramStyle = a.flatMap(v -> v.member("parameterStyle")).filter(v -> v.isSet()).map(s -> s.asEnum(ParameterStyle.class)).orElse(ParameterStyle.WRAPPED);

		Service service = new Service(name, portType, portName, ns, url, paramStyle);

		String inter = ws.member("endpointInterface").filter(v -> v.isSet()).map(v -> v.asLiteral()).orElse("");
		if (!inter.isEmpty()) {
			TypeModel typeModel = loader.get(inter);
			if (typeModel == null)
				throw new RuntimeException("can't find endpointInterface '" + inter + "'");
			if (!typeModel.isClass())
				throw new RuntimeException("endpointInterface isn't an class or interface'" + inter + "'");
			clazz = typeModel.asClass();
		}
		service.collectOp(clazz.asClass(), xmlLoader);

		for (MethodModel m : clazz.methods()) {
			if (m.annotation(PostConstruct.class).isPresent()) {
				if (!m.parameters().isEmpty())
					throw new RuntimeException("PostConstruct method can't have parameters on " + clazz.name());
				if (service.postConstruct != null)
					throw new RuntimeException("only one method can be annoted with @PostConstruct on " + clazz.name());
				service.postConstruct = m.name();
			}
			if (m.annotation(PreDestroy.class).isPresent()) {
				if (!m.parameters().isEmpty())
					throw new RuntimeException("@PreDestroy method can't have parameters on " + clazz.name());
				if (service.postConstruct != null)
					throw new RuntimeException("only one method can be annoted with @PreDestroy on " + clazz.name());
				service.preDestroy = m.name();
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
			if (a.flatMap(v -> v.member("exclude")).filter(v -> v.isSet()).map(v -> v.asBoolean()).orElse(false))
				continue;
			String opName = a.flatMap(v -> v.member("operationName")).filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).orElse(m.name());
			String action = a.flatMap(v -> v.member("action")).filter(v -> v.isSet()).map(v -> v.asLiteral()).orElse("");

			a = m.annotation(SOAPBinding.class);
			Style style = a.flatMap(v -> v.member("style")).filter(v -> v.isSet()).map(v -> v.asEnum(Style.class)).orElse(Style.DOCUMENT);
			if (style != Style.DOCUMENT)
				throw new RuntimeException("Only Document style is managed " + m);
			Use use = a.flatMap(v -> v.member("use")).filter(v -> v.isSet()).map(v -> v.asEnum(Use.class)).orElse(Use.LITERAL);
			if (use != Use.LITERAL)
				throw new RuntimeException("Only literal use is managed " + m);
			ParameterStyle paramStyle = a.flatMap(v -> v.member("parameterStyle")).filter(v -> v.isSet()).map(v -> v.asEnum(ParameterStyle.class)).orElse(defaultParamStyle);

			Parameter r = null;
			TypeModel type = m.type();
			if (!type.isVoid()) {
				a = m.annotation(WebResult.class);
				boolean h = a.flatMap(v -> v.member("header")).filter(v -> v.isSet()).map(v -> v.asBoolean()).orElse(false);
				String paramName = a.flatMap(v -> v.member("name")).filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.BARE ? opName + "Response" : "return");
				String paramNs = a.flatMap(v -> v.member("targetNamespace")).filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle != ParameterStyle.WRAPPED || h ? this.ns : "");

				r = new Parameter(new QName(paramNs, paramName), m.type(), xmlLoader.add(m.type()), h);
			}
			List<Parameter> params = new ArrayList<>();
			for (ParamModel<?> p : m.parameters()) {
				a = p.annotation(WebParam.class);
				boolean h = a.flatMap(v -> v.member("header")).map(v -> v.asBoolean()).orElse(false);
				String paramName = a.flatMap(v -> v.member("name")).filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.BARE ? opName : "arg" + p.index());
				String paramNs = a.flatMap(v -> v.member("targetNamespace")).filter(v -> v.isSet()).map(v -> v.asLiteral()).filter(v -> !v.isEmpty())
						.orElse(paramStyle == ParameterStyle.WRAPPED && !h ? "" : this.ns);
				params.add(new Parameter(new QName(paramNs, paramName), p.type(), xmlLoader.add(p.type()), h));
			}
			operations.add(new Operation(m.name(), new QName(ns, opName), paramStyle == ParameterStyle.WRAPPED, action, params, r));
		}
	}
}
