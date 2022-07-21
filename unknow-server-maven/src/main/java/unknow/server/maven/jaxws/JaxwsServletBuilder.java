/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
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
import unknow.server.jaxws.OperationWrapper;
import unknow.server.jaxws.WSMethod;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.binding.Service;
import unknow.server.maven.jaxws.binding.XmlEnum;
import unknow.server.maven.jaxws.binding.XmlObject;
import unknow.server.maven.jaxws.binding.XmlType;
import unknow.server.maven.jaxws.binding.XmlTypeLoader;
import unknow.server.maven.jaxws.binding.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public class JaxwsServletBuilder {
	private static final NameExpr CONTEXT = new NameExpr("context");
	private static final NameExpr QNAME = new NameExpr("qname");

	private static final Modifier.Keyword[] PSF = { Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL };
	private static final Modifier.Keyword[] PF = { Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL };

	private final ClassOrInterfaceDeclaration serviceClass;
	private final JaxMarshallerBuilder mbuilder;

	private final Map<String, NameExpr> saxHandlers = new HashMap<>();

	private final Service service;

	private ClassOrInterfaceDeclaration servlet;

	private JaxSaxHandlerBuilder header;
	private JaxSaxHandlerBuilder body;

	public JaxwsServletBuilder(ClassOrInterfaceDeclaration serviceClass, ModelLoader loader, JaxMarshallerBuilder mbuilder, XmlTypeLoader xmlLoader) {
		this.serviceClass = serviceClass;
		this.mbuilder = mbuilder;
		// collect operations
		this.service = Service.build(serviceClass, loader, xmlLoader);
	}

	public void generate(CompilationUnit cu, TypeCache types, String baseUrl) {
		String name = service.name;
		NodeList<Expression> list = Utils.list();
		for (String s : service.urls)
			list.add(new StringLiteralExpr(s));

		servlet = cu.addClass(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Servlet", PF).addExtendedType(types.get(HttpServlet.class));
		servlet.addAndGetAnnotation(WebServlet.class).addPair("urlPatterns", new ArrayInitializerExpr(list)).addPair("name", new StringLiteralExpr(name));

		servlet.addFieldWithInitializer(types.get(long.class), "serialVersionUID", new LongLiteralExpr("1"), PSF);

		servlet.addFieldWithInitializer(types.get(Logger.class), "log",
				new MethodCallExpr(new TypeExpr(types.get(LoggerFactory.class)), "getLogger", Utils.list(new ClassExpr(types.get(servlet)))), PSF);

		servlet.addFieldWithInitializer(types.get(serviceClass), "WS", new ObjectCreationExpr(null, types.get(serviceClass), Utils.list()), PSF);
		// TODO life cycle @PostConstruct, @PreDestroy

		Collections.sort(service.operations, (o1, o2) -> o1.sig().compareTo(o2.sig()));
		servlet.addFieldWithInitializer(types.get(String[].class), "OP_SIG", Utils.array(types.get(String.class), service.operations.size()), PSF);
		servlet.addFieldWithInitializer(types.array(WSMethod.class), "OP_CALL", Utils.array(types.get(WSMethod.class), service.operations.size()), PSF);
		BlockStmt init = servlet.addStaticInitializer();
		int oi = 0;
		for (Service.Op o : service.operations) {
			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_SIG"), new IntegerLiteralExpr("" + oi)), new StringLiteralExpr(o.sig()), Operator.ASSIGN));

			BlockStmt b = new BlockStmt().addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(Envelope.class), "r"),
					new ObjectCreationExpr(null, types.get(Envelope.class), Utils.list()), Operator.ASSIGN));
			if (o.paramStyle == ParameterStyle.WRAPPED)
				b.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(OperationWrapper.class), "o"),
						new CastExpr(types.get(OperationWrapper.class), new MethodCallExpr(new NameExpr("e"), "getBody", Utils.list(new IntegerLiteralExpr("0")))), Operator.ASSIGN));

			NodeList<Expression> param = Utils.list();

			int h = 0;
			int i = 0;
			for (Service.Param p : o.params) {
				Expression v;
				if (p.header)
					v = new MethodCallExpr(new NameExpr("e"), "getHeader", Utils.list(new IntegerLiteralExpr(Integer.toString(h++))));
				else if (o.paramStyle == ParameterStyle.WRAPPED)
					v = new MethodCallExpr(new NameExpr("o"), "get", Utils.list(new IntegerLiteralExpr(Integer.toString(i++))));
				else
					v = new MethodCallExpr(new NameExpr("e"), "getBody", Utils.list(new IntegerLiteralExpr(Integer.toString(i++))));

				param.add(new CastExpr(types.get(p.clazz), v));
			}

			Expression e = null;
			if (o.result == null)
				b.addStatement(new MethodCallExpr(new NameExpr("WS"), o.m, param));
			else {
				b.addStatement(
						new AssignExpr(new VariableDeclarationExpr(types.get(Object.class), "ro"), new MethodCallExpr(new NameExpr("WS"), o.m, param), Operator.ASSIGN));
//			TODO if(o.result.header)
				e = new ObjectCreationExpr(null, types.get(Element.class),
						Utils.list(new StringLiteralExpr(o.result.ns), new StringLiteralExpr(o.result.name), new NameExpr("ro")));
			}
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				NodeList<Expression> p = Utils.list(new StringLiteralExpr(o.ns), new StringLiteralExpr(o.name + "Response"));
				if (e != null)
					p.add(e);
				// TODO out param
				e = new ObjectCreationExpr(null, types.get(OperationWrapper.class), p);
			} else {
				// TODO out param
			}
			if (e != null)
				b.addStatement(new MethodCallExpr(new NameExpr("r"), "addBody", Utils.list(e)));
			b.addStatement(new ReturnStmt(new NameExpr("r")));

			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new IntegerLiteralExpr("" + oi)),
					new LambdaExpr(Utils.list(new Parameter(new UnknownType(), "e")), b), Operator.ASSIGN));
			oi++;
		}

		generateHandlers(types);

		byte[] wsdl = new WsdlBuilder(service, baseUrl).build();
		servlet.addFieldWithInitializer(types.get(byte[].class), "WSDL", Utils.byteArray(wsdl), PSF);
		servlet.addMethod("doGet", PF).addMarkerAnnotation(Override.class).addThrownException(types.get(IOException.class))
				.addParameter(types.get(HttpServletRequest.class), "req").addParameter(types.get(HttpServletResponse.class), "res").getBody().get()
				.addStatement(new IfStmt(new BinaryExpr(new MethodCallExpr(new NameExpr("req"), "getParameter", Utils.list(new StringLiteralExpr("wsdl"))),
						new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ReturnStmt(), null))
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentType", Utils.list(new StringLiteralExpr("text/xml"))))
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentLength", Utils.list(new IntegerLiteralExpr(Integer.toString(wsdl.length)))))
				.addStatement(new MethodCallExpr(new MethodCallExpr(new NameExpr("res"), "getOutputStream"), "write", Utils.list(new NameExpr("WSDL"))));

		servlet.addMethod("doPost",
				PF).addMarkerAnnotation(Override.class).addParameter(types.get(HttpServletRequest.class), "req").addParameter(types.get(HttpServletResponse.class), "res")
				.getBody().get().addStatement(new TryStmt(
						new BlockStmt()
								.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(Envelope.class), "e"),
										new MethodCallExpr(new TypeExpr(types.get(SaxParser.class)), "parse",
												Utils.list(new ThisExpr(),
														new ObjectCreationExpr(null, types.get(InputSource.class),
																Utils.list(new MethodCallExpr(new NameExpr("req"), "getInputStream"))))),
										Operator.ASSIGN))
								.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(int.class), "i"),
										new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "binarySearch",
												Utils.list(new NameExpr("OP_SIG"), new MethodCallExpr(new NameExpr("e"), "sig"))),
										Operator.ASSIGN))
								.addStatement(new IfStmt(new BinaryExpr(new NameExpr("i"), new IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS),
										new BlockStmt()
												.addStatement(new MethodCallExpr(null, "fault", Utils.list(new NameExpr("res"), new StringLiteralExpr("unknown request"))))
												.addStatement(new ReturnStmt()),
										null))
								// TODO if i<0 return soap fault
								.addStatement(
										new MethodCallExpr(new NameExpr("Marshallers"), "marshall",
												Utils.list(
														new MethodCallExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new NameExpr("i")), "call",
																Utils.list(new NameExpr("e"))),
														new MethodCallExpr(new NameExpr("res"), "getWriter")))),
						Utils.list(new CatchClause(new Parameter(types.get(Exception.class), "e"), new BlockStmt()
								.addStatement(new MethodCallExpr(null, "fault", Utils.list(new NameExpr("res"), new MethodCallExpr(new NameExpr("e"), "getMessage"))))
								.addStatement(
										new MethodCallExpr(new NameExpr("log"), "warn", Utils.list(new StringLiteralExpr("failed to service request"), new NameExpr("e")))))),
						null));
		generateFault(types);

	}

	private void generateFault(TypeCache types) {
		LineComment lineComment = new LineComment("OK");
		lineComment.setRange(Range.range(0, 0, 0, 0));
		BlockStmt ok = new BlockStmt();
		ok.addOrphanComment(lineComment);

		servlet.addMethod("fault", PSF).addParameter(types.get(HttpServletResponse.class), "res").addParameter(types.get(String.class), "err").getBody().get()
				.addStatement(new TryStmt(new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("res"), "setStatus", Utils.list(new IntegerLiteralExpr("500"))))
						.addStatement(new MethodCallExpr(new MethodCallExpr(
								new MethodCallExpr(new MethodCallExpr(new NameExpr("res"), "getWriter"), "append", Utils.list(new StringLiteralExpr(
										"<e:Envelope xmlns:e=\\\"http://schemas.xmlsoap.org/soap/envelope/\\\"><e:Body><e:Fault><faultcode>Server</faultcode><faultstring>"))),
								"append", Utils.list(new NameExpr("err"))), "write", Utils.list(new StringLiteralExpr("</faultstring></e:Fault></e:Body></e:Envelope>")))),
						Utils.list(new CatchClause(new Parameter(types.get(Exception.class), "ignore"), ok)), null));
	}

	private void generateHandlers(TypeCache types) {
		header = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());
		body = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());

		for (Service.Op o : service.operations) {
			System.out.println("building " + o);

			List<XmlField> childs = new ArrayList<>();

			if (o.result != null) {
				if (o.result.type instanceof XmlObject)
					mbuilder.add((XmlObject) o.result.type);
				else if (o.result.type instanceof XmlEnum)
					mbuilder.add((XmlEnum) o.result.type);
			}

			for (Service.Param p : o.params) {
				if (p.header)
					header.addElem(new XmlField(p.type, "", "addHeader", p.name, p.ns));
				else if (o.paramStyle == ParameterStyle.WRAPPED)
					childs.add(new XmlField(p.type, "", "add", p.name, p.ns));
				else
					body.addElem(new XmlField(p.type, "", "addBody", p.name, p.ns));
			}
			if (o.paramStyle == ParameterStyle.WRAPPED)
				body.addElem(new XmlField(new XmlOperation(o.name, childs), "", "addBody", o.name, service.ns));
		}

		ClassOrInterfaceType t = types.get(SaxHandler.class, types.get(SaxContext.class));
		servlet.addImplementedType(t);
		servlet.addMethod("startElement", PF).addParameter(types.get(String.class), "qname").addParameter(types.get(String.class), "name")
				.addParameter(types.get(SaxContext.class), "context").addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
				.setBody(new BlockStmt().addStatement(new IfStmt(
						new MethodCallExpr(new StringLiteralExpr("{http://schemas.xmlsoap.org/soap/envelope/}Header"), "equals", Utils.list(QNAME)),
						new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", Utils.list(new NameExpr("HEADER")))),
						new IfStmt(new MethodCallExpr(new StringLiteralExpr("{http://schemas.xmlsoap.org/soap/envelope/}Body"), "equals", Utils.list(QNAME)),
								new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", Utils.list(new NameExpr("BODY")))),
								new IfStmt(new MethodCallExpr(new StringLiteralExpr("{http://schemas.xmlsoap.org/soap/envelope/}Envelope"), "equals", Utils.list(QNAME)),
										new ExpressionStmt(
												new MethodCallExpr(CONTEXT, "push", Utils.list(new ObjectCreationExpr(null, types.get(Envelope.class), Utils.list())))),
										new ThrowStmt(new ObjectCreationExpr(null, types.get(SAXException.class),
												Utils.list(new BinaryExpr(new StringLiteralExpr("Invalid tag "), QNAME, BinaryExpr.Operator.PLUS)))))))));
		servlet.addFieldWithInitializer(t, "HEADER", new ObjectCreationExpr(null, t, null, Utils.list(), header.build()), PSF);
		servlet.addFieldWithInitializer(t, "BODY", new ObjectCreationExpr(null, t, null, Utils.list(), body.build()), PSF);
	}

	/**
	 * @param type
	 * @param types
	 */
	private NameExpr generateHandler(XmlType type, TypeCache types) {
		if (type instanceof XmlEnum) {
			XmlEnum e = (XmlEnum) type;
			NodeList<Expression> values = Utils.list();
//			Statement s = 
			for (XmlEnumEntry c : e.entries)
				values.add(new StringLiteralExpr(c.value));
			servlet.addFieldWithInitializer(types.array(String.class), e.convertMethod, Utils.array(types.get(String.class), values), PSF);
			servlet.addMethod(e.convertMethod, PSF).addParameter(types.get(String.class), "s").setType(types.get(e.clazz))
					.addThrownException(types.get(SAXException.class))
					.getBody().get()
					.addStatement(Utils.assign(types.get(int.class), "i", new MethodCallExpr(
							new TypeExpr(types.get(Arrays.class)), "binarySearch", Utils.list(new NameExpr(e.convertMethod), new NameExpr("s")))))
					.addStatement(new IfStmt(
							new BinaryExpr(new NameExpr("i"), new IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS),
							new ThrowStmt(new ObjectCreationExpr(null, types.get(SAXException.class), Utils.list(new BinaryExpr(new StringLiteralExpr("Invalid enum constant: "), new NameExpr("s"), BinaryExpr.Operator.PLUS)))),
							null))
					.addStatement(new ReturnStmt(new ArrayAccessExpr(new MethodCallExpr(new TypeExpr(types.get(e.clazz)), "values"), new NameExpr("i"))));
		}
		String k = type.isSimple() ? "" : type.binaryName();
		NameExpr nameExpr = saxHandlers.get(k);
		if (nameExpr != null)
			return nameExpr;
		String name = "$" + saxHandlers.size() + "$";
		saxHandlers.put(k, nameExpr = new NameExpr(name));

		ClassOrInterfaceType t = types.get(SaxHandler.class, types.get(SaxContext.class));

		servlet.addFieldWithInitializer(t, name, new ObjectCreationExpr(null, t, null, Utils.list(), JaxSaxHandlerBuilder.build(types, type, n -> generateHandler(n, types))),
				PSF);
		return nameExpr;
	}

	private static class XmlOperation extends XmlObject {

		final String name;

		public XmlOperation(String name, List<XmlField> elems) {
			super(OperationWrapper.class.getCanonicalName(), new Factory(".qname", null), Collections.emptyList(), elems, null, null);
			this.name = name;
		}

		@Override
		public String binaryName() {
			return "op:" + name;
		}
	}

}
