/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.function.Function;

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
import unknow.server.maven.Utils;
import unknow.server.maven.jaxws.binding.SchemaData;
import unknow.server.maven.jaxws.binding.XmlObject;
import unknow.server.maven.jaxws.binding.XmlType;
import unknow.server.maven.jaxws.binding.XmlObject.XmlElem;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;

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
		public String clazz() {
			return "";
		}

		@Override
		public SchemaData schema() {
			return null;
		};
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
		this.start = new ThrowStmt(new ObjectCreationExpr(null, types.get(SAXException.class), Utils.list(
				new BinaryExpr(new StringLiteralExpr("Invalid tag "), QNAME, BinaryExpr.Operator.PLUS))));
		this.last = new ExpressionStmt(new MethodCallExpr(CONTEXT, "previous"));
	}

	public void setObjectCreation(String factory, String method) {
		Expression create = new ObjectCreationExpr(null, types.get(clazz), Utils.list());
		if (".qname".equals(factory))
			create = new ObjectCreationExpr(null, types.get(clazz), Utils.list(QNAME));
		else if (factory != null)
			create = new MethodCallExpr(new TypeExpr(types.get(factory)), method);
		attrs.addStatement(new AssignExpr(new VariableDeclarationExpr(types.get(clazz), "o"), create, AssignExpr.Operator.ASSIGN))
				.addStatement(new MethodCallExpr(CONTEXT, "push", Utils.list(new NameExpr("o"))));
	}

	public void setQName(String field) {
		attrs.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("o"), field), QNAME, AssignExpr.Operator.ASSIGN));
	}

	public void addAttr(XmlField a) {
		Expression var = new NameExpr("a");
		if (firstAttr) {
			firstAttr = false;
			var = new VariableDeclarationExpr(types.get(String.class), "a");
		}
		attrs
				.addStatement(new AssignExpr(
						var,
						new MethodCallExpr(ATTRS, "getValue", Utils.list(new StringLiteralExpr(a.ns), new StringLiteralExpr(a.name))),
						AssignExpr.Operator.ASSIGN))
				.addStatement(new IfStmt(
						new BinaryExpr(new NameExpr("a"), new NullLiteralExpr(), Operator.NOT_EQUALS),
						new ExpressionStmt(
								new MethodCallExpr(new NameExpr("o"), a.setter, Utils.list(a.type().convert(types, new NameExpr("a"))))),
						null));
	}

	public void setValue(XmlElem value) {
		last = new BlockStmt()
				.addStatement(new MethodCallExpr(
						new EnclosedExpr(new CastExpr(types.get(clazz), new MethodCallExpr(CONTEXT, "peek"))),
						value.setter,
						Utils.list(value.type().convert(types, new MethodCallExpr(CONTEXT, "textContent")))))
				.addStatement(new MethodCallExpr(CONTEXT, "previous"));
	}

	public void addElem(XmlField a) {
		start = new IfStmt(
				new MethodCallExpr(new StringLiteralExpr(a.qname()), "equals", Utils.list(QNAME)),
				new ExpressionStmt(new MethodCallExpr(CONTEXT, "next", Utils.list(handlers.apply(a.type())))),
				start);

		end = new IfStmt(
				new MethodCallExpr(new StringLiteralExpr(a.qname()), "equals", Utils.list(QNAME)),
				new BlockStmt()
						.addStatement(Utils.assign(types.get(a.type().clazz()), "v", a.type().convert(types, new MethodCallExpr(CONTEXT, "pop"))))
						.addStatement(new MethodCallExpr(
								new EnclosedExpr(new CastExpr(types.get(clazz), new MethodCallExpr(CONTEXT, "peek"))),
								a.setter,
								Utils.list(new NameExpr("v")))),
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

		NodeList<BodyDeclaration<?>> list = Utils.list();
		if (!attrs.getStatements().isEmpty())
			list.add(new MethodDeclaration(
					Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
					"attributes",
					new VoidType(),
					Utils.list(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(Attributes.class), "attrs"), new Parameter(types.get(SaxContext.class), "context")))
							.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
							.setBody(attrs));
		list.add(new MethodDeclaration(
				Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
				"startElement",
				new VoidType(),
				Utils.list(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
						.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
						.setBody(new BlockStmt(Utils.list(start))));
		list.add(new MethodDeclaration(
				Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
				"endElement",
				new VoidType(),
				Utils.list(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
						.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
						.setBody(new BlockStmt(Utils.list(last))));
		return list;
	}

	public static NodeList<BodyDeclaration<?>> build(TypeCache types, XmlType xml, Function<XmlType, NameExpr> handlers) {
		if (xml.isSimple()) {
			return Utils.list(
					new MethodDeclaration(
							Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL),
							"endElement",
							new VoidType(),
							Utils.list(new Parameter(types.get(String.class), "qname"), new Parameter(types.get(String.class), "name"), new Parameter(types.get(SaxContext.class), "context")))
									.addMarkerAnnotation(Override.class).addThrownException(types.get(SAXException.class))
									.setBody(new BlockStmt()
											.addStatement(new MethodCallExpr(CONTEXT, "push", Utils.list(new MethodCallExpr(CONTEXT, "textContent"))))
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