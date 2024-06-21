/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.UnknownType;

import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import unknow.server.jaxws.AbstractWs;
import unknow.server.jaxws.Envelope;
import unknow.server.jaxws.OperationWrapper;
import unknow.server.jaxws.WSMethod;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.binding.Operation;
import unknow.server.maven.jaxws.binding.Service;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JaxwsServletBuilder {
	private final ClassModel serviceClass;

	private final Service service;

	private ClassOrInterfaceDeclaration servlet;

	public JaxwsServletBuilder(ClassModel serviceClass, Service service) {
		this.serviceClass = serviceClass;
		this.service = service;
	}

	public void generate(CompilationUnit cu, TypeCache types, String factory, String wsdl) {
		String name = service.name;
		NodeList<Expression> list = Utils.list();
		for (String s : service.urls)
			list.add(Utils.text(s));

		servlet = cu.addClass(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Servlet", Utils.PUBLIC).addExtendedType(types.getClass(AbstractWs.class));
		servlet.addAndGetAnnotation(WebServlet.class).addPair("urlPatterns", new ArrayInitializerExpr(list)).addPair("name", Utils.text(name));

		servlet.addFieldWithInitializer(types.get(long.class), "serialVersionUID", new LongLiteralExpr("1L"), Utils.PSF);

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

				String n = "N$" + w;
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
				servlet.addFieldWithInitializer(types.getClass(QName.class), "P$" + w + "$" + (bi + h),
						new ObjectCreationExpr(null, types.getClass(QName.class), Utils.list(Utils.text(p.name.getNamespaceURI()), Utils.text(p.name.getLocalPart()))),
						Utils.PSF);
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
				clazz.add(o.result.type);
				b.addStatement(
						new AssignExpr(new VariableDeclarationExpr(types.getClass(o.result.type), "ro"), new MethodCallExpr(new NameExpr("WS"), o.m, param), Operator.ASSIGN));
				e = new ObjectCreationExpr(null, types.getClass(JAXBElement.class, TypeCache.EMPTY),
						Utils.list(
								new ObjectCreationExpr(null, types.getClass(QName.class),
										Utils.list(Utils.text(o.result.name.getNamespaceURI()), Utils.text(o.result.name.getLocalPart()))),
								new ClassExpr(types.get(o.result.type)), new NameExpr("ro")));
			}
			NodeList<Expression> header = Utils.list();
			NodeList<Expression> body = Utils.list();
			if (o.wrapped) {
				// TODO out param
				Expression c = new ClassExpr(types.get(e == null ? Object.class : JAXBElement.class));
				if (e == null)
					e = new NullLiteralExpr();
				e = new ObjectCreationExpr(null, types.getClass(JAXBElement.class, TypeCache.EMPTY), Utils.list(new ObjectCreationExpr(null, types.getClass(QName.class),
						Utils.list(Utils.text(o.name.getNamespaceURI()), Utils.text(o.name.getLocalPart() + "Response"))), c, e));
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
			w++;
		}

		servlet.addConstructor(Modifier.Keyword.PUBLIC).getBody().addStatement(new MethodCallExpr(null, "super", Utils.list(new StringLiteralExpr(wsdl))));

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

		Statement s = new IfStmt(wrapped,
				new ReturnStmt(new MethodCallExpr(null, "read",
						Utils.list(new NameExpr("r"), new NameExpr("u"),
								new ObjectCreationExpr(null, types.getClass(OperationWrapper.class), Utils.list(new NameExpr("n")))))),
				new ThrowStmt(new ObjectCreationExpr(null, types.getClass(IOException.class), Utils.list(Utils.text("Invalid request")))));
		w = 0;
		for (Operation o : service.operations) {
			int i = 0;
			for (unknow.server.maven.jaxws.binding.Parameter p : o.params)
				s = new IfStmt(new MethodCallExpr(new NameExpr("P$" + w + "$" + i++), "equals", Utils.list(new NameExpr("n"))), new ReturnStmt(
						new MethodCallExpr(new MethodCallExpr(new NameExpr("u"), "unmarshal", Utils.list(new NameExpr("r"), new ClassExpr(types.get(p.type)))), "getValue")),
						s);
			w++;
		}

		servlet.addMethod("read", Utils.PROTECT).addMarkerAnnotation(Override.class).setType(types.get(Object.class))
				.addParameter(new Parameter(types.get(XMLStreamReader.class), "r")).addParameter(new Parameter(types.get(Unmarshaller.class), "u"))
				.addThrownException(types.getClass(XMLStreamException.class)).addThrownException(types.getClass(JAXBException.class))
				.addThrownException(types.getClass(IOException.class)).getBody().get()
				.addStatement(Utils.assign(types.get(QName.class), "n", new MethodCallExpr(new NameExpr("r"), "getName"))).addStatement(s);
	}
}
