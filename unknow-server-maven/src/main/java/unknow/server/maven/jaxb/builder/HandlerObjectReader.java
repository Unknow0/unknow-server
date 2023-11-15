package unknow.server.maven.jaxb.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import unknow.server.jaxb.StrReader;
import unknow.server.jaxb.UnmarshallerImpl;
import unknow.server.jaxb.XmlHandler;
import unknow.server.maven.SourceBuilder.AbstractSourceBuilder;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxb.HandlerContext;
import unknow.server.maven.jaxb.model.XmlChoice;
import unknow.server.maven.jaxb.model.XmlCollection;
import unknow.server.maven.jaxb.model.XmlElement;
import unknow.server.maven.jaxb.model.XmlElements;
import unknow.server.maven.jaxb.model.XmlType;
import unknow.server.maven.jaxb.model.XmlTypeComplex;
import unknow.server.maven.model.TypeModel;

public class HandlerObjectReader extends AbstractSourceBuilder<HandlerContext> {
	private static final String INSTANCE = "INSTANCE";
	private static final String PARENT = "parent";
	private static final NameExpr LISTENER = new NameExpr("listener");

	@Override
	protected void build() {
		XmlTypeComplex xml = ctx.xml();

		Expression create;
		if ("".equals(xml.factory().method))
			create = new ObjectCreationExpr(null, types.getClass(xml.factory().clazz), Utils.list());
		else
			create = new MethodCallExpr(new TypeExpr(types.getClass(xml.factory().clazz)), xml.factory().method);

		BlockStmt b = cl.addMethod("read", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(XMLStreamException.class))
				.addThrownException(types.getClass(JAXBException.class)).addParameter(types.get(XMLStreamReader.class), "r").setType(types.get(xml.type()))
				.addParameter(types.get(Object.class), PARENT).addParameter(types.get(UnmarshallerImpl.class), "listener").createBody()
				.addStatement(Utils.assign(types.get(xml.type()), "o", create));
		xml.type().asClass().method("beforeUnmarshal", ctx.type(Unmarshaller.class.getName()), ctx.type(Object.class.getName()))
				.ifPresent(m -> b.addStatement(new MethodCallExpr(new NameExpr("o"), "beforeUnmarshal", Utils.list(LISTENER, new NameExpr(PARENT)))));
		b.addStatement(new MethodCallExpr(LISTENER, "beforeUnmarshal", Utils.list(new NameExpr("o"), new NameExpr(PARENT))));

		readAttributes(xml, b);

		BlockStmt end = new BlockStmt();
		processCollection(xml.getElements(), b, end);

		xml.type().asClass().method("afterUnmarshal", ctx.type(Unmarshaller.class.getName()), ctx.type(Object.class.getName()))
				.ifPresent(m -> end.addStatement(new MethodCallExpr(new NameExpr("o"), "afterUnmarshal", Utils.list(LISTENER, new NameExpr(PARENT)))));

		IfStmt i = new IfStmt(
				new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "END_ELEMENT"), BinaryExpr.Operator.EQUALS),
				end.addStatement(new MethodCallExpr(LISTENER, "afterUnmarshal", Utils.list(new NameExpr("o"), new NameExpr(PARENT))))
						.addStatement(new ReturnStmt(new NameExpr("o"))),
				null);

		if (xml.getElements().iterator().hasNext()) {
			AtomicInteger c = new AtomicInteger(0);
			Statement j = new ExpressionStmt(new MethodCallExpr(new TypeExpr(types.get(XmlHandler.class)), "skipTag", Utils.list(new NameExpr("r"))));
			for (XmlElement e : xml.getElements())
				j = readElement(e, j, new NameExpr("o"), e.setter(), c);

			i = new IfStmt(
					new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "START_ELEMENT"), BinaryExpr.Operator.EQUALS),
					new BlockStmt().addStatement(Utils.assign(types.get(QName.class), "q", new MethodCallExpr(new NameExpr("r"), "getName"))).addStatement(j), i);
		}

		BlockStmt w = new BlockStmt().addStatement(Utils.assign(types.get("int"), "n", new MethodCallExpr(new NameExpr("r"), "next")));
		if (xml.getValue() != null) {
			XmlElement v = xml.getValue();
			w.addStatement(new IfStmt(
					new BinaryExpr(new NameExpr("n"), new FieldAccessExpr(new TypeExpr(types.get(XMLStreamConstants.class)), "CHARACTERS"), BinaryExpr.Operator.EQUALS),
					new BlockStmt()
							.addStatement(new MethodCallExpr(new NameExpr("o"), v.setter(),
									Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(v.xmlType()))), INSTANCE), "toObject",
											Utils.list(new MethodCallExpr(new TypeExpr(types.get(StrReader.class)), "read", Utils.list(new NameExpr("r"))))))))
							.addStatement(new AssignExpr(new NameExpr("n"), new MethodCallExpr(new NameExpr("r"), "getEventType"), AssignExpr.Operator.ASSIGN)),
					null));
		}
		b.addStatement(new WhileStmt(new MethodCallExpr(new NameExpr("r"), "hasNext"), w.addStatement(i)))
				.addStatement(new ThrowStmt(new ObjectCreationExpr(null, types.getClass(XMLStreamException.class), Utils.list(new StringLiteralExpr("EOF")))));

	}

	private void readAttributes(XmlTypeComplex xml, BlockStmt b) {
		if (xml.getAttributes().isEmpty())
			return;

		Statement i = null;
		int c = 0;
		for (XmlElement e : xml.getAttributes()) {
			i = new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$a" + c++), "equals", Utils.list(new NameExpr("n"))),
					new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(),
							Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(e.xmlType()))), INSTANCE), "toObject",
									Utils.list(new MethodCallExpr(new NameExpr("r"), "getAttributeValue", Utils.list(new NameExpr("i")))))))),
					i);
		}
		b.addStatement(new ForStmt(Utils.list(Utils.assign(types.get("int"), "i", new IntegerLiteralExpr("0"))),
				new BinaryExpr(new NameExpr("i"), new MethodCallExpr(new NameExpr("r"), "getAttributeCount"), BinaryExpr.Operator.LESS),
				Utils.list(new UnaryExpr(new NameExpr("i"), UnaryExpr.Operator.POSTFIX_INCREMENT)),
				new BlockStmt()
						.addStatement(Utils.assign(types.get(QName.class), "n", new MethodCallExpr(new NameExpr("r"), "getAttributeName", Utils.list(new NameExpr("i")))))
						.addStatement(i)));
	}

	private void processCollection(XmlElements elements, BlockStmt b, BlockStmt end) {
		for (XmlElement e : elements) {
			if (!(e.xmlType() instanceof XmlCollection))
				continue;
			Expression v = new NameExpr("l$" + e.name());
			if (e.type().isArray())
				v = new MethodCallExpr(v, "toArray", Utils.list(new NameExpr(ctx.emptyArray(e.type()))));
			end.addStatement(new IfStmt(new BinaryExpr(new NameExpr("l$" + e.name()), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
					new ExpressionStmt(new MethodCallExpr(new NameExpr("o"), e.setter(), Utils.list(v))), null));

			Type t = e.type().isArray() ? types.getClass(List.class, types.get(e.type().asArray().type().asClass())) : types.get(e.type());
			b.addStatement(Utils.assign(t, "l$" + e.name(), new NullLiteralExpr()));
		}
	}

	private Statement readElement(XmlElement e, Statement j, NameExpr n, String setter, AtomicInteger c) {
		XmlType xmlType = e.xmlType();
		BlockStmt s = new BlockStmt();
		if (xmlType instanceof XmlCollection) {
			xmlType = ((XmlCollection) xmlType).component();
			n = new NameExpr("l$" + e.name());
			setter = "add";
			s.addStatement(new IfStmt(new BinaryExpr(n, new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
					new ExpressionStmt(new AssignExpr(n,
							new ObjectCreationExpr(null, types.getClass(e.type().isAssignableTo(Set.class) ? HashSet.class : ArrayList.class, TypeCache.EMPTY), Utils.list()),
							AssignExpr.Operator.ASSIGN)),
					null));
		}

		if (!(xmlType instanceof XmlChoice)) {
			Expression call = new MethodCallExpr(n, setter, Utils.list(new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(xmlType))), INSTANCE), "read",
					Utils.list(new NameExpr("r"), new NameExpr("o"), LISTENER))));
			return new IfStmt(new MethodCallExpr(new NameExpr(e.name() + "$e" + c.getAndIncrement()), "equals", Utils.list(new NameExpr("q"))),
					s.getStatements().isEmpty() ? new ExpressionStmt(call) : s.addStatement(call), j);
		}

		XmlChoice choice = (XmlChoice) xmlType;
		Map<TypeModel, List<XmlElement>> map = new HashMap<>();
		for (XmlElement x : choice.choice())
			map.computeIfAbsent(x.xmlType().type(), k -> new ArrayList<>()).add(x);

		for (XmlElement x : choice.choice()) {
			BlockStmt b = s.clone();
			xmlType = x.xmlType();
			if (xmlType instanceof XmlCollection)
				xmlType = ((XmlCollection) xmlType).component();

			Expression v = new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(ctx.handler(xmlType))), INSTANCE), "read",
					Utils.list(new NameExpr("r"), new NameExpr("o"), LISTENER));

			if (map.get(xmlType.type()).size() > 1)
				v = new ObjectCreationExpr(null, types.getClass(JAXBElement.class, TypeCache.EMPTY),
						Utils.list(new NameExpr("q"), new ClassExpr(types.get(xmlType.type())), new NullLiteralExpr(), v));

			Expression call = new MethodCallExpr(n, setter, Utils.list(v));
			j = new IfStmt(new MethodCallExpr(new NameExpr(x.name() + "$e" + c.getAndIncrement()), "equals", Utils.list(new NameExpr("q"))),
					b.getStatements().isEmpty() ? new ExpressionStmt(call) : b.addStatement(call), j);
		}
		return j;
	}
}
