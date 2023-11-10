package unknow.server.maven.jaxb.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import unknow.server.jaxb.MarshallerImpl;
import unknow.server.maven.SourceBuilder.AbstractSourceBuilder;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxb.HandlerContext;
import unknow.server.maven.jaxb.model.XmlChoice;
import unknow.server.maven.jaxb.model.XmlCollection;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.jaxb.model.XmlTypeComplex;
import unknow.server.maven.model.TypeModel;

public class HandlerObjectWriter extends AbstractSourceBuilder<HandlerContext> {
	private static final String INSTANCE = "INSTANCE";
	private static final NameExpr LISTENER = new NameExpr("listener");

	@Override
	protected void build() {
		XmlTypeComplex xml = ctx.xml();

		BlockStmt b = cl.addMethod("write", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(XMLStreamException.class))
				.addThrownException(types.getClass(JAXBException.class)).addParameter(types.get(XMLStreamWriter.class), "w").addParameter(types.get(xml.type()), "t")
				.addParameter(types.get(MarshallerImpl.class), "m").addParameter(types.get(Marshaller.Listener.class), "listener").createBody();
		xml.type().asClass().method("beforeMarshal", ctx.type(Marshaller.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "beforeMarshal", Utils.list(LISTENER))));
		b.addStatement(new IfStmt(new BinaryExpr(LISTENER, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
				new ExpressionStmt(new MethodCallExpr(LISTENER, "beforeMarshal", Utils.list(new NameExpr("t")))), null));

		buildAttributes(xml.getAttributes(), b);

		AtomicInteger i = new AtomicInteger(0);
		for (XmlElement e : xml.getElements())
			buildElement(types, e, b, i);

		if (xml.getValue() != null) {
			XmlElement value = xml.getValue();
			b.addStatement(new MethodCallExpr(new NameExpr("w"), "writeCharacters",
					Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(value.xmlType()))), INSTANCE), "toString",
							Utils.list(new MethodCallExpr(new NameExpr("t"), value.getter()))))));
		}

		xml.type().asClass().method("afterMarshal", ctx.type(Marshaller.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "afterMarshal", Utils.list(LISTENER))));
		b.addStatement(new IfStmt(new BinaryExpr(LISTENER, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
				new ExpressionStmt(new MethodCallExpr(LISTENER, "afterMarshal", Utils.list(new NameExpr("t")))), null));
	}

	private void buildAttributes(List<XmlElement> attrs, BlockStmt b) {
		if (attrs.isEmpty())
			return;

		b.addStatement(new VariableDeclarationExpr(types.get(String.class), "s"));
		for (XmlElement e : attrs) {
			Expression v = new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(e.xmlType()))), INSTANCE), "toString",
					Utils.list(new MethodCallExpr(new NameExpr("t"), e.getter())));
			v = new EnclosedExpr(new AssignExpr(new NameExpr("s"), v, AssignExpr.Operator.ASSIGN));
			NodeList<Expression> l = Utils.list();
			if (!e.ns().isEmpty())
				l.add(Utils.text(e.ns()));
			l.add(Utils.text(e.name()));
			l.add(new NameExpr("s"));
			if (e.type().isPrimitive())
				b.addStatement(new MethodCallExpr(new NameExpr("w"), "writeAttribute", l));
			else
				b.addStatement(new IfStmt(new BinaryExpr(v, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new ExpressionStmt(new MethodCallExpr(new NameExpr("w"), "writeAttribute", l)), null));
		}
	}

	private void buildElement(TypeCache types, XmlElement e, BlockStmt b, AtomicInteger i) {
		String n = "o$" + i.getAndIncrement();
		BlockStmt w;

		TypeModel t = e.prop().type();
		XmlType xml = e.xmlType();
		if (xml instanceof XmlCollection)
			xml = ((XmlCollection) xml).component();

		if (t.isArray() || t.isAssignableTo(Collection.class)) {
			TypeModel c = t.isArray() ? t.asArray().type() : t.asClass().ancestor(Collection.class.getName()).parameter(0).type();
			BlockStmt p = new BlockStmt()
					.addStatement(new IfStmt(new BinaryExpr(new NameExpr("e"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS), new ContinueStmt(), null));
			buildElement(types, e, xml, p, new NameExpr("e"));

			if (t.isArray()) {
				p.addStatement(0, Utils.assign(types.get(xml.type()), "e", new ArrayAccessExpr(new NameExpr(n), new NameExpr("i"))));
				w = new BlockStmt().addStatement(new ForStmt(Utils.list(Utils.assign(types.get("int"), "i", new IntegerLiteralExpr("0"))),
						new BinaryExpr(new NameExpr("i"), new FieldAccessExpr(new NameExpr(n), "length"), BinaryExpr.Operator.LESS),
						Utils.list(new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT)), p));
			} else {
				w = new BlockStmt().addStatement(new ForEachStmt(new VariableDeclarationExpr(types.get(c), "e"), new NameExpr(n), p));
			}
		} else
			w = buildElement(types, e, xml, new BlockStmt(), new NameExpr(n));

		b.addStatement(Utils.assign(types.get(e.type()), n, new MethodCallExpr(new NameExpr("t"), e.getter())));
		if (e.type().isPrimitive())
			b.addStatement(w);
		else
			b.addStatement(new IfStmt(new BinaryExpr(new NameExpr(n), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS), w, null));
	}

	private BlockStmt buildElement(TypeCache types, XmlElement e, XmlType xml, BlockStmt p, Expression name) {
		if (!(xml instanceof XmlChoice)) {
			p.addStatement(new MethodCallExpr(new NameExpr("w"), "writeStartElement", Utils.list(Utils.text(e.ns()), Utils.text(e.name()))))
					.addStatement(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(xml))), INSTANCE), "write",
							Utils.list(new NameExpr("w"), name, new NameExpr("m"), LISTENER)))
					.addStatement(new MethodCallExpr(new NameExpr("w"), "writeEndElement", Utils.list()));
			return p;
		}
		XmlChoice c = (XmlChoice) xml;
		Map<TypeModel, List<XmlElement>> map = new HashMap<>();
		for (XmlElement x : c.choice())
			map.computeIfAbsent(x.xmlType().type(), k -> new ArrayList<>()).add(x);
		List<XmlElement> list = new ArrayList<>();
		for (List<XmlElement> v : map.values()) {
			if (v.size() == 1)
				list.add(v.get(0));
		}

		Statement s = new ThrowStmt(new ObjectCreationExpr(null, types.getClass(JAXBException.class), Utils.list(Utils.text("Expected unexpected type"))));

		for (XmlElement x : list)
			s = new IfStmt(new InstanceOfExpr(name, types.getClass(x.xmlType().type())),
					buildElement(types, x, x.xmlType(), new BlockStmt(), new CastExpr(types.get(x.xmlType().type()), name)), s);
		p.addStatement(new IfStmt(new InstanceOfExpr(name, types.getClass(JAXBElement.class)), new ExpressionStmt(
				new MethodCallExpr(new NameExpr("m"), "write", Utils.list(new CastExpr(types.get(JAXBElement.class), name), new NameExpr("w"), new NullLiteralExpr()))), s));
		return p;
	}
}
