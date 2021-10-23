/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import unknow.sax.SaxContext;
import unknow.sax.SaxHandler;
import unknow.sax.SaxParser;
import unknow.server.jaxws.Element;
import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.Envelope.Operation;
import unknow.server.jaxws.OperationWrapper;
import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;

/**
 * @author unknow
 */
public class JaxwsServletBuilder {
	private static final NameExpr CONTEXT = new NameExpr("context");
	private static final NameExpr QNAME = new NameExpr("qname");

	private static final AnnotationExpr SOAPBINDING = new NormalAnnotationExpr().addPair("style", new FieldAccessExpr(QNAME, "DOCUMENT")).addPair("parameterStyle", new FieldAccessExpr(QNAME, "WRAPPED"));

	private static final List<XmlField> OP_ATTRS = Arrays.asList(new XmlField(JaxSaxHandlerBuilder.QNAME_PARAM, "", "setQName", "", ""));
	private static final String OPERATION = Operation.class.getName();

	private final ClassOrInterfaceDeclaration serviceClass;
	private final Map<String, ClassOrInterfaceDeclaration> classes;
	private final JaxMarshallerBuilder mbuilder;

	private final SOAPBinding.Style style;
	private final SOAPBinding.ParameterStyle paramStyle;

	private final Map<String, NameExpr> saxHandlers = new HashMap<>();
	private final List<Op> operations = new ArrayList<>();

	/** target name */
	private final String name;
	/** target namespace */
	private final String ns;

	private ClassOrInterfaceDeclaration servlet;

	private JaxSaxHandlerBuilder header;
	private JaxSaxHandlerBuilder body;

	public JaxwsServletBuilder(ClassOrInterfaceDeclaration serviceClass, Map<String, ClassOrInterfaceDeclaration> classes, JaxMarshallerBuilder mbuilder) {
		this.serviceClass = serviceClass;
		this.classes = classes;
		this.mbuilder = mbuilder;
		// collect operations
		AnnotationExpr a = serviceClass.getAnnotationByClass(WebService.class).get();
		this.name = a.findFirst(MemberValuePair.class, m -> "name".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(serviceClass.resolve().getClassName());
		this.ns = a.findFirst(MemberValuePair.class, m -> "targetNamespace".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(serviceClass.resolve().getPackageName());

		Optional<AnnotationExpr> o = serviceClass.getAnnotationByClass(SOAPBinding.class);
		style = SOAPBinding.Style.valueOf(o.orElse(SOAPBINDING).findFirst(MemberValuePair.class, m -> "style".equals(m.getNameAsString())).map(m -> styleName(m)).get());
		paramStyle = SOAPBinding.ParameterStyle.valueOf(o.orElse(SOAPBINDING).findFirst(MemberValuePair.class, m -> "parameterStyle".equals(m.getNameAsString())).map(m -> styleName(m)).get());
	}

	private static String styleName(MemberValuePair m) {
		Expression e = m.getValue();
		if (e.isNameExpr())
			return e.asNameExpr().getNameAsString();
		return e.asFieldAccessExpr().getNameAsString();
	}

	/**
	 * @param cl
	 */
	private void collectOperation(ClassOrInterfaceDeclaration cl) {
		for (MethodDeclaration m : cl.getMethods()) {
			Optional<AnnotationExpr> o = m.getAnnotationByClass(WebMethod.class);
			if (o.isEmpty())
				continue;
			MemberValuePair orElse = o.get().findFirst(MemberValuePair.class, v -> "exclude".equals(v.getNameAsString())).orElse(null);
			if (orElse != null && orElse.getValue().asBooleanLiteralExpr().getValue())
				continue;
			String name = o.get().findFirst(MemberValuePair.class, v -> "operationName".equals(v.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().getValue()).orElse(m.getNameAsString());

			Style style = this.style;
			ParameterStyle paramStyle = this.paramStyle;
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
				rns = ns;

			Param r = new Param(rns, rname, XmlType.get(m.getType(), classes), m.resolve().getQualifiedName(), header);
			Op op = new Op(m.getNameAsString(), name, ns, r, style, paramStyle);
			for (Parameter p : m.getParameters()) {
				String ns = "##default";
				name = "##default";
				XmlType type = XmlType.get(p.getType(), classes);
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

	public void generate(CompilationUnit cu, TypeCache types) {
		// TODO get url pattern ?
		servlet = cu.addClass(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Servlet", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addExtendedType(types.get(HttpServlet.class));
		servlet.addAndGetAnnotation(WebServlet.class).addPair("urlPatterns", new StringLiteralExpr("/" + name)).addPair("name", new StringLiteralExpr(name));

		servlet.addFieldWithInitializer(types.get(long.class), "serialVersionUID", new LongLiteralExpr("1"), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

		servlet.addFieldWithInitializer(types.get(Logger.class), "log", new MethodCallExpr(
				new TypeExpr(types.get(LoggerFactory.class)),
				"getLogger",
				new NodeList<>(new ClassExpr(types.get(servlet)))), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

		servlet.addFieldWithInitializer(types.get(serviceClass), "WS", new ObjectCreationExpr(null, types.get(serviceClass), new NodeList<>()), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		// TODO life cycle @PostConstruct, @PreDestroy

		AnnotationExpr a = serviceClass.getAnnotationByClass(WebService.class).get();
		String inter = a.findFirst(MemberValuePair.class, m -> "endpointInterface".equals(m.getNameAsString())).map(m -> m.getValue().asStringLiteralExpr().asString()).orElse(null);
		if (inter != null) {
			ClassOrInterfaceDeclaration i = classes.get(inter);
			if (i != null)
				collectOperation(i);
			else // TODO load from classpath
				throw new RuntimeException("can't find in source");
		} else
			collectOperation(serviceClass);

		Collections.sort(operations, (o1, o2) -> o1.sig().compareTo(o2.sig()));
		servlet.addFieldWithInitializer(types.get(String[].class), "OP_SIG", new ArrayCreationExpr(types.get(String.class), new NodeList<>(new ArrayCreationLevel(operations.size())), null), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		servlet.addFieldWithInitializer(types.array(Function.class, types.get(Envelope.class), types.get(Envelope.class)), "OP_CALL", new ArrayCreationExpr(types.get(Function.class), new NodeList<>(new ArrayCreationLevel(operations.size())), null), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL).addSingleMemberAnnotation(SuppressWarnings.class, new StringLiteralExpr("unchecked"));
		BlockStmt init = servlet.addStaticInitializer();
		int oi = 0;
		for (Op o : operations) {
			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_SIG"), new IntegerLiteralExpr("" + oi)), new StringLiteralExpr(o.sig()), Operator.ASSIGN));

			BlockStmt b = new BlockStmt()
					.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(Envelope.class), "r"), new ObjectCreationExpr(null, types.get(Envelope.class), new NodeList<>()), Operator.ASSIGN));
			if (o.paramStyle == ParameterStyle.WRAPPED)
				b.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(Operation.class), "o"), new CastExpr(types.get(Operation.class), new MethodCallExpr(new NameExpr("e"), "getBody", new NodeList<>(new IntegerLiteralExpr("0")))), Operator.ASSIGN));

			NodeList<Expression> param = new NodeList<>();

			int h = 0;
			int i = 0;
			for (Param p : o.params) {
				Expression v;
				if (p.header)
					v = new MethodCallExpr(new NameExpr("e"), "getHeader", new NodeList<>(new IntegerLiteralExpr(Integer.toString(h++))));
				else if (o.paramStyle == ParameterStyle.WRAPPED)
					v = new MethodCallExpr(new NameExpr("o"), "get", new NodeList<>(new IntegerLiteralExpr(Integer.toString(i++))));
				else
					v = new MethodCallExpr(new NameExpr("e"), "getBody", new NodeList<>(new IntegerLiteralExpr(Integer.toString(i++))));

				param.add(new CastExpr(types.get(p.clazz), v));
			}
			if (o.result == null)
				b.addStatement(new MethodCallExpr(new NameExpr("WS"), o.m, param));
			else
				b.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(Object.class), "ro"), new MethodCallExpr(new NameExpr("WS"), o.m, param), Operator.ASSIGN));
//			TODO if(o.result.header)
			Expression e = new ObjectCreationExpr(null, types.get(Element.class), new NodeList<>(new StringLiteralExpr(o.result.ns), new StringLiteralExpr(o.result.name), new NameExpr("ro")));
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				NodeList<Expression> p = new NodeList<>(new StringLiteralExpr(o.name), new StringLiteralExpr(o.ns), e);
				// TODO out param
				e = new ObjectCreationExpr(null, types.get(OperationWrapper.class), p);
			} else {
			}
			b.addStatement(new MethodCallExpr(new NameExpr("r"), "addBody", new NodeList<>(e)));
			b.addStatement(new ReturnStmt(new NameExpr("r")));

			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new IntegerLiteralExpr("" + oi)),
					new LambdaExpr(new NodeList<>(new Parameter(new UnknownType(), "e")), b),
					Operator.ASSIGN));
			oi++;
		}

		generateHandlers(types);

		servlet.addMethod("doGet", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addMarkerAnnotation(Override.class)
				.addParameter(types.get(HttpServletRequest.class), "req")
				.addParameter(types.get(HttpServletResponse.class), "res")
				.setBody(new BlockStmt());
		// TODO wsdl

		servlet.addMethod("doPost", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).addMarkerAnnotation(Override.class)
				.addParameter(types.get(HttpServletRequest.class), "req")
				.addParameter(types.get(HttpServletResponse.class), "res")
				.setBody(new BlockStmt()
						.addStatement(new TryStmt(new BlockStmt()
								.addStatement(
										new AssignExpr(
												new VariableDeclarationExpr(types.get(Envelope.class), "e"),
												new MethodCallExpr(new TypeExpr(types.get(SaxParser.class)), "parse", new NodeList<>(
														new ThisExpr(),
														new ObjectCreationExpr(null, types.get(InputSource.class), new NodeList<>(new MethodCallExpr(new NameExpr("req"), "getInputStream"))))),
												Operator.ASSIGN))
								.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(System.class)), "out"), "println", new NodeList<>(new NameExpr("e"))))
								.addStatement(new AssignExpr(
										new VariableDeclarationExpr(types.get(int.class), "i"),
										new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "binarySearch", new NodeList<>(new NameExpr("OP_SIG"), new MethodCallExpr(new NameExpr("e"), "sig"))),
										Operator.ASSIGN))
								// TODO if i<0 return soap fault
								.addStatement(new MethodCallExpr(new NameExpr("Marshallers"), "marshall", new NodeList<>(
										new MethodCallExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new NameExpr("i")), "apply", new NodeList<>(new NameExpr("e"))),
										new MethodCallExpr(new NameExpr("res"), "getWriter")))),
								new NodeList<>(
										new CatchClause(new Parameter(types.get(Exception.class), "e"), new BlockStmt()
												.addStatement(new MethodCallExpr(new NameExpr("res"), "setStatus", new NodeList<>(new IntegerLiteralExpr("500"))))
												.addStatement(new TryStmt(
														new BlockStmt().addStatement(
																new MethodCallExpr(
																		new MethodCallExpr(
																				new MethodCallExpr(
																						new MethodCallExpr(new NameExpr("res"), "getWriter"),
																						"append",
																						new NodeList<>(new StringLiteralExpr("<s:Envelope  xmlns:s=\\\"http://schemas.xmlsoap.org/soap/envelope/\\\"><s:Body><s:Fault><faultcode>Client</faultcode><faultstring>"))),
																				"append", new NodeList<>(new MethodCallExpr(new NameExpr("e"), "getMessage"))),
																		"append", new NodeList<>(new StringLiteralExpr("</faultstring></s:Fault><s:Body></s:Envelope>")))),
														new NodeList<>(new CatchClause(new Parameter(types.get(Exception.class), "ignore"), new BlockStmt())), null))
												.addStatement(new MethodCallExpr(new NameExpr("log"), "warn", new NodeList<>(new StringLiteralExpr("failed to service request"), new NameExpr("e")))))),
								null)));
	}

	private void generateHandlers(TypeCache types) {
		header = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());
		body = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());

		for (Op o : operations) {
			System.out.println("building " + o);

			List<XmlField> childs = new ArrayList<>();

			for (Param p : o.params) {
				if (p.type instanceof XmlObject)
					mbuilder.add((XmlObject) p.type);
				if (p.header)
					header.addElem(new XmlField(p.type, "", "addHeader", p.name, p.ns));
				else if (o.paramStyle == ParameterStyle.WRAPPED)
					childs.add(new XmlField(p.type, "", "add", p.name, p.ns));
				else
					body.addElem(new XmlField(p.type, "", "addBody", p.name, p.ns));
			}
			if (o.paramStyle == ParameterStyle.WRAPPED)
				body.addElem(new XmlField(new XmlObject(OPERATION, null, OP_ATTRS, childs, null), "", "addBody", o.name, ns));
		}

		ClassOrInterfaceType t = types.get(SaxHandler.class, types.get(SaxContext.class));
		servlet.addImplementedType(t);
		servlet.addMethod("startElement", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL)
				.addParameter(types.get(String.class), "qname").addParameter(types.get(String.class), "name").addParameter(types.get(SaxContext.class), "context")
				.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
				.setBody(new BlockStmt()
						.addStatement(
								new IfStmt(
										new MethodCallExpr(new StringLiteralExpr("{http://www.w3.org/2001/12/soap-envelope}Header"), "equals", new NodeList<>(QNAME)),
										new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", new NodeList<>(new NameExpr("HEADER")))),
										new IfStmt(
												new MethodCallExpr(new StringLiteralExpr("{http://www.w3.org/2001/12/soap-envelope}Body"), "equals", new NodeList<>(QNAME)),
												new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", new NodeList<>(new NameExpr("BODY")))),
												new IfStmt(
														new MethodCallExpr(new StringLiteralExpr("{http://www.w3.org/2001/12/soap-envelope}Envelope"), "equals", new NodeList<>(QNAME)),
														new ExpressionStmt(new MethodCallExpr(CONTEXT, "push", new NodeList<>(new ObjectCreationExpr(null, types.get(Envelope.class), new NodeList<>())))),
														new ThrowStmt(new ObjectCreationExpr(null, types.get(SAXException.class), new NodeList<>(new BinaryExpr(new StringLiteralExpr("Invalid tag "), QNAME, BinaryExpr.Operator.PLUS)))))))));
//		servlet.addMethod("endElement", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL)
//				.addParameter(types.get(String.class), "qname").addParameter(types.get(String.class), "name").addParameter(types.get(SaxContext.class), "context")
//				.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
//				.setBody(new BlockStmt()
//						.addStatement(new IfStmt(
//								new UnaryExpr(new MethodCallExpr(new StringLiteralExpr("{http://www.w3.org/2001/12/soap-envelope}Envelope"), "equals", new NodeList<>(QNAME)), UnaryExpr.Operator.LOGICAL_COMPLEMENT),
//								new ExpressionStmt(new MethodCallExpr(new NameExpr("context"), "previous")),
//								null)));

		servlet.addFieldWithInitializer(t, "HEADER",
				new ObjectCreationExpr(null, t, null, new NodeList<>(), header.build()),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		servlet.addFieldWithInitializer(t, "BODY",
				new ObjectCreationExpr(null, t, null, new NodeList<>(), body.build()),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
	}

	/**
	 * @param type
	 * @param types
	 */
	private NameExpr generateHandler(XmlType type, TypeCache types) {
		String k = type.isSimple() ? "" : type.binaryName();
		NameExpr nameExpr = saxHandlers.get(k);
		if (nameExpr != null && !OPERATION.equals(type))
			return nameExpr;
		String name = "$" + saxHandlers.size() + "$";
		saxHandlers.put(k, nameExpr = new NameExpr(name));

		ClassOrInterfaceType t = types.get(SaxHandler.class, types.get(SaxContext.class));

		servlet.addFieldWithInitializer(t, name,
				new ObjectCreationExpr(null, t, null, new NodeList<>(), JaxSaxHandlerBuilder.build(types, type, n -> generateHandler(n, types))),
				Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		return nameExpr;
	}

	public static class Op {
		final String m;
		final String name;
		final String ns;
		final SOAPBinding.Style style;
		final SOAPBinding.ParameterStyle paramStyle;
		final List<Param> params;
		final Param result;

		public Op(String m, String name, String ns, Param result, SOAPBinding.Style style, SOAPBinding.ParameterStyle paramStyle) {
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
		final String ns;
		final String name;
		final XmlType type;
		final String clazz;
		final boolean header;

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
