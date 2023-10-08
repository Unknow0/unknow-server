/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
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
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.UnknownType;

import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import unknow.server.jaxws.AbstractWs;
import unknow.server.jaxws.Element;
import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.OperationWrapper;
import unknow.server.jaxws.WSMethod;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.binding.Operation;
import unknow.server.maven.jaxws.binding.Service;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model_xml.XmlLoader;

/**
 * @author unknow
 */
public class JaxwsServletBuilder {
	private static final Logger logger = LoggerFactory.getLogger(JaxwsServletBuilder.class);

	private final ClassOrInterfaceDeclaration serviceClass;

	private final Service service;

	private ClassOrInterfaceDeclaration servlet;

	public JaxwsServletBuilder(ClassOrInterfaceDeclaration serviceClass, ModelLoader loader, XmlLoader xmlLoader) {
		this.serviceClass = serviceClass;
		this.service = Service.build(serviceClass, loader, xmlLoader);
	}

	public void generate(CompilationUnit cu, TypeCache types, String baseUrl, String factory) {
		String name = service.name;
		NodeList<Expression> list = Utils.list();
		for (String s : service.urls)
			list.add(Utils.text(baseUrl + s));

		servlet = cu.addClass(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Servlet", Utils.PUBLIC).addExtendedType(types.getClass(AbstractWs.class));
		servlet.addAndGetAnnotation(WebServlet.class).addPair("urlPatterns", new ArrayInitializerExpr(list)).addPair("name", Utils.text(name));

		servlet.addFieldWithInitializer(types.getClass(long.class), "serialVersionUID", new LongLiteralExpr("1"), Utils.PSF);

		servlet.addFieldWithInitializer(types.getClass(Logger.class), "logger",
				new MethodCallExpr(new TypeExpr(types.getClass(LoggerFactory.class)), "getLogger", Utils.list(new ClassExpr(types.getClass(servlet)))), Utils.PSF);

		servlet.addFieldWithInitializer(types.getClass(serviceClass), "WS", new ObjectCreationExpr(null, types.getClass(serviceClass), Utils.list()), Utils.PSF);

		Collections.sort(service.operations, (o1, o2) -> o1.sig().compareTo(o2.sig()));

		Set<TypeModel> clazz = new HashSet<>();
		NodeList<SwitchEntry> entries = new NodeList<>();
		Expression wrapped = null;
		int w = 0;
		for (Operation o : service.operations) {
			BlockStmt b = new BlockStmt();
			if (o.wrapped) {
				b.addStatement(new AssignExpr(new VariableDeclarationExpr(types.getClass(OperationWrapper.class), "o"),
						new CastExpr(types.getClass(OperationWrapper.class), new MethodCallExpr(new NameExpr("e"), "getBody", Utils.list(new IntegerLiteralExpr("0")))),
						Operator.ASSIGN));

				String n = "N$" + w++;
				servlet.addFieldWithInitializer(types.getClass(QName.class), n,
						new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(Utils.text(o.name.getNamespaceURI()), Utils.text(o.name.getLocalPart()))),
						Utils.PSF);
				Expression e = new MethodCallExpr(new NameExpr(n), "equals", Utils.list(new NameExpr("n")));
				if (wrapped != null)
					wrapped = new BinaryExpr(wrapped, e, BinaryExpr.Operator.OR);
				else
					wrapped = e;
			}

			NodeList<Expression> param = Utils.list();

			int h = 0;
			int bi = 0;
			for (unknow.server.maven.jaxws.binding.Parameter p : o.params) {
				clazz.add(p.type);
				Expression v;
				if (p.header)
					v = new MethodCallExpr(new NameExpr("e"), "getHeader", Utils.list(new IntegerLiteralExpr(Integer.toString(h++))));
				else if (o.wrapped)
					v = new MethodCallExpr(new NameExpr("o"), "get", Utils.list(new IntegerLiteralExpr(Integer.toString(bi++))));
				else
					v = new MethodCallExpr(new NameExpr("e"), "getBody", Utils.list(new IntegerLiteralExpr(Integer.toString(bi++))));
				param.add(new CastExpr(types.get(p.type), v));
			}

			Expression e = null;
			if (o.result == null)
				b.addStatement(new MethodCallExpr(new NameExpr("WS"), o.m, param));
			else {
				b.addStatement(
						new AssignExpr(new VariableDeclarationExpr(types.getClass(Object.class), "ro"), new MethodCallExpr(new NameExpr("WS"), o.m, param), Operator.ASSIGN));
				e = new ObjectCreationExpr(null, types.getClass(Element.class),
						Utils.list(Utils.text(o.result.name.getNamespaceURI()), Utils.text(o.result.name.getLocalPart()), new NameExpr("ro")));
			}
			NodeList<Expression> header = Utils.list();
			NodeList<Expression> body = Utils.list();
			if (o.wrapped) {
				// TODO out param
				e = new ObjectCreationExpr(null, types.getClass(Element.class),
						Utils.list(Utils.text(o.name.getNamespaceURI()), Utils.text(o.name.getLocalPart() + "Response"), e == null ? new NullLiteralExpr() : e));
			} else {
				// TODO out param
			}
			if (e != null)
				(o.result != null && o.result.header ? header : body).add(e);
			b.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.getClass(Envelope.class), Utils.list(
					new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList", header), new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList", body)))));

			String n = "CALL$" + entries.size();
			entries.add(new SwitchEntry().setLabels(Utils.list(new StringLiteralExpr(o.sig()))).addStatement(new ReturnStmt(new NameExpr(n))));
			servlet.addFieldWithInitializer(types.get(WSMethod.class), n, new LambdaExpr(Utils.list(new Parameter(new UnknownType(), "e")), b), Utils.PSF);
		}

		servlet.addConstructor(Modifier.Keyword.PUBLIC).getBody().addStatement(new MethodCallExpr(null, "super", Utils.list(new NullLiteralExpr()))); // TODO generated wsdl

		NodeList<Expression> l = clazz.stream().map(v -> new ClassExpr(types.get(v))).collect(Collectors.toCollection(() -> new NodeList<>()));
		servlet.addMethod("getCtx", Utils.PROTECT).addMarkerAnnotation(Override.class).setType(types.get(JAXBContext.class)).getBody().get().addStatement(new TryStmt(
				new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(new ObjectCreationExpr(null, types.getClass(factory), Utils.list()), "createContext",
						Utils.list(Utils.array(types.get(Class.class), l), new NullLiteralExpr())))),
				Utils.list(new CatchClause(new Parameter(types.get(JAXBException.class), "e"),
						new BlockStmt().addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(RuntimeException.class), Utils.list(new NameExpr("e"))))))),
				null));

		entries.add(new SwitchEntry().addStatement(new ReturnStmt(new NullLiteralExpr())));
		servlet.addMethod("getCall", Utils.PROTECT).addMarkerAnnotation(Override.class).setType(types.get(WSMethod.class))
				.addParameter(new Parameter(types.get(String.class), "sig")).getBody().get().addStatement(new SwitchStmt(new NameExpr("sig"), entries));

		servlet.addMethod("isWrappedOp", Utils.PROTECT).addMarkerAnnotation(Override.class).setType(types.get(boolean.class))
				.addParameter(new Parameter(types.get(QName.class), "n")).getBody().get().addStatement(new ReturnStmt(wrapped));
	}
}
