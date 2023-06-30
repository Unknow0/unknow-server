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

import jakarta.jws.soap.SOAPBinding.ParameterStyle;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import unknow.server.maven.jaxws.binding.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.binding.XmlObject;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
import unknow.server.maven.jaxws.binding.XmlType;
import unknow.server.maven.jaxws.binding.XmlTypeLoader;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.jvm.JvmClass;

/**
 * @author unknow
 */
public class JaxwsServletBuilder {
	private static final Logger logger = LoggerFactory.getLogger(JaxwsServletBuilder.class);

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
			list.add(Utils.text(s));

		servlet = cu.addClass(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Servlet", PF).addExtendedType(types.getClass(HttpServlet.class));
		servlet.addAndGetAnnotation(WebServlet.class).addPair("urlPatterns", new ArrayInitializerExpr(list)).addPair("name", Utils.text(name));

		servlet.addFieldWithInitializer(types.getClass(long.class), "serialVersionUID", new LongLiteralExpr("1"), PSF);

		servlet.addFieldWithInitializer(types.getClass(Logger.class), "log",
				new MethodCallExpr(new TypeExpr(types.getClass(LoggerFactory.class)), "getLogger", Utils.list(new ClassExpr(types.get(servlet)))), PSF);

		servlet.addFieldWithInitializer(types.get(serviceClass), "WS", new ObjectCreationExpr(null, types.get(serviceClass), Utils.list()), PSF);

		Collections.sort(service.operations, (o1, o2) -> o1.sig().compareTo(o2.sig()));
		servlet.addFieldWithInitializer(types.get(String[].class), "OP_SIG", Utils.array(types.getClass(String.class), service.operations.size()), PSF);
		servlet.addFieldWithInitializer(types.array(WSMethod.class), "OP_CALL", Utils.array(types.getClass(WSMethod.class), service.operations.size()), PSF);
		BlockStmt init = servlet.addStaticInitializer();
		int oi = 0;
		for (Service.Op o : service.operations) {
			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_SIG"), new IntegerLiteralExpr("" + oi)), Utils.text(o.sig()), Operator.ASSIGN));

			BlockStmt b = new BlockStmt().addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(Envelope.class), "r"),
					new ObjectCreationExpr(null, types.getClass(Envelope.class), Utils.list()), Operator.ASSIGN));
			if (o.paramStyle == ParameterStyle.WRAPPED)
				b.addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(OperationWrapper.class), "o"),
						new CastExpr(types.getClass(OperationWrapper.class), new MethodCallExpr(new NameExpr("e"), "getBody", Utils.list(new IntegerLiteralExpr("0")))),
						Operator.ASSIGN));

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
						new AssignExpr(new VariableDeclarationExpr(types.getClass(Object.class), "ro"), new MethodCallExpr(new NameExpr("WS"), o.m, param), Operator.ASSIGN));
				e = new ObjectCreationExpr(null, types.getClass(Element.class), Utils.list(Utils.text(o.result.ns()), Utils.text(o.result.name()), new NameExpr("ro")));
			}
			if (o.paramStyle == ParameterStyle.WRAPPED) {
				NodeList<Expression> p = Utils.list(Utils.text(o.ns), Utils.text(o.name + "Response"));
				if (e != null)
					p.add(e);
				// TODO out param
				e = new ObjectCreationExpr(null, types.getClass(OperationWrapper.class), p);
			} else {
				// TODO out param
			}
			if (e != null)
				b.addStatement(new MethodCallExpr(new NameExpr("r"), o.result != null && o.result.header ? "addHeader" : "addBody", Utils.list(e)));
			b.addStatement(new ReturnStmt(new NameExpr("r")));

			init.addStatement(new AssignExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new IntegerLiteralExpr("" + oi)),
					new LambdaExpr(Utils.list(new Parameter(new UnknownType(), "e")), b), Operator.ASSIGN));
			oi++;
		}

		generateHandlers(types);

		if (service.postConstruct != null)
			servlet.addMethod("init", PF).addMarkerAnnotation(Override.class).getBody().get().addStatement(new MethodCallExpr(new NameExpr("WS"), service.postConstruct));
		if (service.preDestroy != null)
			servlet.addMethod("destroy", PF).addMarkerAnnotation(Override.class).getBody().get().addStatement(new MethodCallExpr(new NameExpr("WS"), service.preDestroy));

		byte[] wsdl = new WsdlBuilder(service, baseUrl).build();
		servlet.addFieldWithInitializer(types.get(byte[].class), "WSDL", Utils.byteArray(wsdl), PSF);
		servlet.addMethod("doGet", PF).addMarkerAnnotation(Override.class).addThrownException(types.getClass(IOException.class))
				.addParameter(types.getClass(HttpServletRequest.class), "req").addParameter(types.getClass(HttpServletResponse.class), "res").getBody().get()
				.addStatement(new IfStmt(new BinaryExpr(new MethodCallExpr(new NameExpr("req"), "getParameter", Utils.list(Utils.text("wsdl"))), new NullLiteralExpr(),
						BinaryExpr.Operator.EQUALS), new ReturnStmt(), null))
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentType", Utils.list(Utils.text("text/xml"))))
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentLength", Utils.list(new IntegerLiteralExpr(Integer.toString(wsdl.length)))))
				.addStatement(new MethodCallExpr(new MethodCallExpr(new NameExpr("res"), "getOutputStream"), "write", Utils.list(new NameExpr("WSDL"))));

		servlet.addMethod("doPost", PF).addMarkerAnnotation(
				Override.class).addParameter(types.getClass(HttpServletRequest.class),
						"req")
				.addParameter(types.getClass(HttpServletResponse.class), "res").getBody().get()
				.addStatement(new TryStmt(
						new BlockStmt()
								.addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(Envelope.class), "e"),
										new MethodCallExpr(new TypeExpr(types.getClass(SaxParser.class)), "parse",
												Utils.list(new ThisExpr(),
														new ObjectCreationExpr(null, types.getClass(InputSource.class),
																Utils.list(new MethodCallExpr(new NameExpr("req"), "getInputStream"))))),
										Operator.ASSIGN))
								.addStatement(
										new AssignExpr(new VariableDeclarationExpr(types.getClass(int.class), "i"),
												new MethodCallExpr(new TypeExpr(types.getClass(Arrays.class)), "binarySearch",
														Utils.list(new NameExpr("OP_SIG"), new MethodCallExpr(new NameExpr("e"), "sig"))),
												Operator.ASSIGN))
								.addStatement(
										new IfStmt(new BinaryExpr(new NameExpr("i"), new IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS),
												new BlockStmt().addStatement(new MethodCallExpr(null, "fault", Utils.list(new NameExpr("res"), Utils.text("unknown request"))))
														.addStatement(new ReturnStmt()),
												null))
								.addStatement(
										new MethodCallExpr(new NameExpr("Marshallers"), "marshall",
												Utils.list(
														new MethodCallExpr(new ArrayAccessExpr(new NameExpr("OP_CALL"), new NameExpr("i")), "call",
																Utils.list(new NameExpr("e"))),
														new MethodCallExpr(new NameExpr("res"), "getWriter")))),
						Utils.list(new CatchClause(new Parameter(types.getClass(Exception.class), "e"), new BlockStmt()
								.addStatement(new MethodCallExpr(null, "fault", Utils.list(new NameExpr("res"), new MethodCallExpr(new NameExpr("e"), "getMessage"))))
								.addStatement(new MethodCallExpr(new NameExpr("log"), "warn", Utils.list(Utils.text("failed to service request"), new NameExpr("e")))))),
						null));
		generateFault(types);

	}

	private void generateFault(TypeCache types) {
		LineComment lineComment = new LineComment("OK");
		lineComment.setRange(Range.range(0, 0, 0, 0));
		BlockStmt ok = new BlockStmt();
		ok.addOrphanComment(lineComment);

		servlet.addMethod("fault", PSF).addParameter(types.getClass(HttpServletResponse.class), "res").addParameter(types.getClass(String.class), "err").getBody().get()
				.addStatement(new TryStmt(
						new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("res"), "setStatus", Utils.list(new IntegerLiteralExpr("500")))).addStatement(
								new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new NameExpr("res"), "getWriter"), "append", Utils.list(Utils.text(
										"<e:Envelope xmlns:e=\\\"http://schemas.xmlsoap.org/soap/envelope/\\\"><e:Body><e:Fault><faultcode>Server</faultcode><faultstring>"))),
										"append", Utils.list(new NameExpr("err"))), "write", Utils.list(Utils.text("</faultstring></e:Fault></e:Body></e:Envelope>")))),
						Utils.list(new CatchClause(new Parameter(types.getClass(Exception.class), "ignore"), ok)), null));
	}

	private void generateHandlers(TypeCache types) {
		header = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());
		body = new JaxSaxHandlerBuilder(types, t -> generateHandler(t, types), Envelope.class.getName());

		for (Service.Op o : service.operations) {
			logger.info("building {}", o);

			List<XmlField<?>> childs = new ArrayList<>();

			if (o.result != null) {
				if (o.result.type() instanceof XmlObject)
					mbuilder.add((XmlObject) o.result.type());
				else if (o.result.type() instanceof XmlEnum)
					mbuilder.add((XmlEnum) o.result.type());
			}

			for (Service.Param p : o.params) {
				if (p.header)
					header.addElem(new XmlField<>(p.type(), p.ns(), p.name(), "", "addHeader"));
				else if (o.paramStyle == ParameterStyle.WRAPPED)
					childs.add(new XmlField<>(p.type(), p.ns(), p.name(), "", "add"));
				else
					body.addElem(new XmlField<>(p.type(), p.ns(), p.name(), "", "addBody"));
			}
			if (o.paramStyle == ParameterStyle.WRAPPED)
				body.addElem(new XmlField<>(new XmlOperation(o.ns, o.name, childs), service.ns, o.name, "", "addBody"));
		}

		ClassOrInterfaceType t = types.getClass(SaxHandler.class, types.getClass(SaxContext.class));
		servlet.addImplementedType(t);
		servlet.addMethod("startElement", PF).addParameter(types.getClass(String.class), "qname").addParameter(types.getClass(String.class), "name")
				.addParameter(types.getClass(SaxContext.class), "context").addMarkerAnnotation(Override.class).addThrownException(types.getClass(SAXException.class))
				.setBody(new BlockStmt().addStatement(new IfStmt(
						new MethodCallExpr(Utils.text("{http://schemas.xmlsoap.org/soap/envelope/}Header"), "equals", Utils.list(QNAME)),
						new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", Utils.list(new NameExpr("HEADER")))),
						new IfStmt(new MethodCallExpr(Utils.text("{http://schemas.xmlsoap.org/soap/envelope/}Body"), "equals", Utils.list(QNAME)),
								new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", Utils.list(new NameExpr("BODY")))),
								new IfStmt(new MethodCallExpr(Utils.text("{http://schemas.xmlsoap.org/soap/envelope/}Envelope"), "equals", Utils.list(QNAME)),
										new ExpressionStmt(
												new MethodCallExpr(CONTEXT, "push", Utils.list(new ObjectCreationExpr(null, types.getClass(Envelope.class), Utils.list())))),
										new ThrowStmt(new ObjectCreationExpr(null, types.getClass(SAXException.class),
												Utils.list(new BinaryExpr(Utils.text("Invalid tag "), QNAME, BinaryExpr.Operator.PLUS)))))))));
		servlet.addFieldWithInitializer(t, "HEADER", new ObjectCreationExpr(null, t, null, Utils.list(), header.build()), PSF);
		servlet.addFieldWithInitializer(t, "BODY", new ObjectCreationExpr(null, t, null, Utils.list(), body.build()), PSF);
	}

	/**
	 * @param type
	 * @param types
	 */
	private NameExpr generateHandler(XmlType<?> type, TypeCache types) {
		logger.info("Building {}", type);
		if (type instanceof XmlEnum) {
			XmlEnum e = (XmlEnum) type;
			NodeList<Expression> values = Utils.list();
//			Statement s = 
			for (XmlEnumEntry c : e.entries)
				values.add(Utils.text(c.value));
			servlet.addFieldWithInitializer(types.array(String.class), e.convertMethod, Utils.array(types.getClass(String.class), values), PSF);
			servlet.addMethod(e.convertMethod, PSF).addParameter(types.getClass(String.class), "s").setType(types.get(e.javaType().name()))
					.addThrownException(types.getClass(SAXException.class)).getBody().get()
					.addStatement(Utils.assign(types.getClass(int.class), "i",
							new MethodCallExpr(new TypeExpr(types.getClass(Arrays.class)), "binarySearch", Utils.list(new NameExpr(e.convertMethod), new NameExpr("s")))))
					.addStatement(new IfStmt(new BinaryExpr(new NameExpr("i"), new IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS),
							new ThrowStmt(new ObjectCreationExpr(null, types.getClass(SAXException.class),
									Utils.list(new BinaryExpr(Utils.text("Invalid enum constant: "), new NameExpr("s"), BinaryExpr.Operator.PLUS)))),
							null))
					.addStatement(new ReturnStmt(new ArrayAccessExpr(new MethodCallExpr(new TypeExpr(types.get(e.javaType().name())), "values"), new NameExpr("i"))));
		}
		String k = type.isSimple() ? "" : type.javaType().name();
		if (type instanceof XmlOperation)
			k = type.name();
		NameExpr nameExpr = saxHandlers.get(k);
		if (nameExpr != null)
			return nameExpr;
		String name = "$" + saxHandlers.size() + "$";
		saxHandlers.put(k, nameExpr = new NameExpr(name));

		ClassOrInterfaceType t = types.getClass(SaxHandler.class, types.getClass(SaxContext.class));

		servlet.addFieldWithInitializer(t, name, new ObjectCreationExpr(null, t, null, Utils.list(), JaxSaxHandlerBuilder.build(types, type, n -> generateHandler(n, types))),
				PSF);
		return nameExpr;
	}

	private static class XmlOperation extends XmlObject {

		public XmlOperation(String ns, String name, List<XmlField<?>> elems) {
			super(new JvmClass(null, OperationWrapper.class, new TypeModel[0]), ns, name, new Factory(".qname", null), null);
			this.elems = elems;
			this.attrs = Collections.emptyList();
		}

		@Override
		public String name() {
			return "op:" + qname();
		}
	}

}
