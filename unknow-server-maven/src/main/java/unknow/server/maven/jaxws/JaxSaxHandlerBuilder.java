/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.function.Function;

import javax.swing.tree.VariableHeightLayoutCache;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.VoidType;

import unknow.sax.SaxContext;
import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxws.model.XmlObject;
import unknow.server.maven.jaxws.model.XmlObject.XmlElem;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType;

public class JaxSaxHandlerBuilder {
	private static final NameExpr CONTEXT = new NameExpr("context");
	private static final NameExpr QNAME = new NameExpr("qname");
	private static final NameExpr ATTRS = new NameExpr("attrs");

	public static final XmlType QNAME_PARAM = new XmlType() {
		@Override
		public Expression convert(TypeCache types, Expression v) {
			return null;
		}

		@Override
		public String binaryName() {
			return null;
		}
	};

	private final TypeCache types;
	private final Function<XmlType, NameExpr> handlers;
	private final String clazz;

	private boolean firstAttr = true;
	private final BlockStmt attrs;
	private Statement start;
	private IfStmt end;
	private Statement last;

	public JaxSaxHandlerBuilder(TypeCache types, Function<XmlType, NameExpr> handlers, String clazz) {
		this.types = types;
		this.handlers = handlers;
		this.clazz = clazz;

		this.firstAttr = true;
		this.attrs = new BlockStmt();
		this.start = new ThrowStmt(new ObjectCreationExpr(null, types.get(SAXException.class), new NodeList<>(
				new BinaryExpr(new StringLiteralExpr("Invalid tag "), QNAME, BinaryExpr.Operator.PLUS))));
		this.last = new ExpressionStmt(new MethodCallExpr(CONTEXT, "previous"));
	}

	public void setObjectCreation(String factory, String method) {
		Expression create = new ObjectCreationExpr(null, types.get(clazz), new NodeList<>());
		if (factory != null)
			create = new MethodCallExpr(new TypeExpr(types.get(factory)), method);
		attrs.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(clazz), "o"), create, AssignExpr.Operator.ASSIGN))
				.addStatement(new MethodCallExpr(CONTEXT, "push", new NodeList<>(new NameExpr("o"))));
	}

	public void setQName(String field) {
		attrs.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("o"), field), QNAME, AssignExpr.Operator.ASSIGN));
	}

	public void addAttr(XmlField a) {
		if (a.type == QNAME_PARAM) {
			attrs.addStatement(new MethodCallExpr(new NameExpr("o"), a.setter, new NodeList<>(QNAME)));
			return;
		}
		Expression var = new NameExpr("a");
		if (firstAttr) {
			firstAttr = false;
			var = new VariableDeclarationExpr(types.get(String.class), "a");
		}
		attrs
				.addStatement(new AssignExpr(
						var,
						new MethodCallExpr(ATTRS, "getValue", new NodeList<>(new StringLiteralExpr(a.ns), new StringLiteralExpr(a.name))),
						AssignExpr.Operator.ASSIGN))
				.addStatement(new IfStmt(
						new BinaryExpr(new NameExpr("a"), new NullLiteralExpr(), Operator.NOT_EQUALS),
						new ExpressionStmt(
								new MethodCallExpr(new NameExpr("o"), a.setter, new NodeList<>(a.type.convert(types, new NameExpr("a"))))),
						null));
	}

	public void setValue(XmlElem value) {
		last = new BlockStmt()
				.addStatement(new MethodCallExpr(
						new EnclosedExpr(new CastExpr(types.get(clazz), new MethodCallExpr(CONTEXT, "pop"))),
						value.setter,
						new NodeList<>(value.type.convert(types, new MethodCallExpr(CONTEXT, "textContent")))))
				.addStatement(new MethodCallExpr(CONTEXT, "previous"));
	}

	public void addElem(XmlField a) {
		start = new IfStmt(
				new MethodCallExpr(new StringLiteralExpr(a.qname()), "equals", new NodeList<>(QNAME)),
				new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", new NodeList<>(handlers.apply(a.type)))),
				start);

		end = new IfStmt(
				new MethodCallExpr(new StringLiteralExpr(a.qname()), "equals", new NodeList<>(QNAME)),
				new BlockStmt()
						.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(a.type.isSimple() ? String.class : Object.class), "v"), new MethodCallExpr(CONTEXT, "pop"), AssignExpr.Operator.ASSIGN))
						.addStatement(new MethodCallExpr(
								new EnclosedExpr(new CastExpr(types.get(clazz), new MethodCallExpr(CONTEXT, "peek"))),
								a.setter,
								new NodeList<>(a.type.convert(types, new NameExpr("v"))))),
				end);
	}

	public NodeList<BodyDeclaration<?>> build() {
		if (end != null) {
			IfStmt l = end;
			while (l.getElseStmt().isPresent() && l.getElseStmt().get() instanceof IfStmt)
				l = (IfStmt) l.getElseStmt().get();
			l.setElseStmt(last);
			last = end;
		}

		NodeList<BodyDeclaration<?>> list = new NodeList<>();
		if (!attrs.getStatements().isEmpty())
			list.add(new MethodDeclaration(
					Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
					"attributes",
					new VoidType(),
					new NodeList<>(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(Attributes.class), "attrs"), new Parameter(types.get(SaxContext.class), "context")))
							.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
							.setBody(attrs));
		list.add(new MethodDeclaration(
				Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
				"startElement",
				new VoidType(),
				new NodeList<>(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
						.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
						.setBody(new BlockStmt(new NodeList<>(start))));
		list.add(new MethodDeclaration(
				Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
				"endElement",
				new VoidType(),
				new NodeList<>(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
						.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
						.setBody(new BlockStmt(new NodeList<>(last))));
		return list;
	}

	public static NodeList<BodyDeclaration<?>> build(TypeCache types, XmlType xml, Function<XmlType, NameExpr> handlers) {
		if (xml.isSimple()) {
			return new NodeList<>(
					new MethodDeclaration(
							Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
							"endElement",
							new VoidType(),
							new NodeList<>(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
									.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
									.setBody(new BlockStmt()
											.addStatement(new MethodCallExpr(CONTEXT, "push", new NodeList<>(new MethodCallExpr(CONTEXT, "textContent"))))
											.addStatement(new MethodCallExpr(CONTEXT, "previous"))));
		}
		XmlObject o = (XmlObject) xml;
		JaxSaxHandlerBuilder b = new JaxSaxHandlerBuilder(types, handlers, o.clazz);
		b.setObjectCreation(o.factoryClazz(), o.factoryMethod());

		for (XmlField a : o.attrs)
			b.addAttr(a);

		if (o.value != null)
			b.setValue(o.value);

		for (XmlField a : o.elems)
			b.addElem(a);
		return b.build();
	}
}