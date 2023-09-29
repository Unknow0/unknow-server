/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;

import unknow.server.maven.model.AnnotationMemberModel;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.AnnotationValue;
import unknow.server.maven.model.AnnotationValue.AnnotationValueAnnotation;
import unknow.server.maven.model.AnnotationValue.AnnotationValueArray;
import unknow.server.maven.model.AnnotationValue.AnnotationValueClass;
import unknow.server.maven.model.AnnotationValue.AnnotationValueEnum;
import unknow.server.maven.model.AnnotationValue.AnnotationValueLiteral;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstAnnotation implements AnnotationModel {
	private final String name;
	private final AnnotationExpr a;
	private final Collection<AnnotationMemberModel> members;

	/**
	 * create new AstAnnotation
	 * 
	 * @param loader
	 * @param a
	 */
	public AstAnnotation(ModelLoader loader, AnnotationExpr a) {
		this.name = a.resolve().getQualifiedName();
		this.a = a;

		Map<String, AnnotationMemberModel> found = new HashMap<>();

		ClassModel cl = loader.get(name).asClass();

		if (a.isSingleMemberAnnotationExpr())
			found.put("value", new AnnotationMemberModel("value", value(loader, a.asSingleMemberAnnotationExpr().getMemberValue()),
					cl.method("value").map(v -> v.defaultValue()).orElse(AnnotationValue.NULL)));
		else if (a.isNormalAnnotationExpr()) {
			for (MemberValuePair m : a.asNormalAnnotationExpr().getPairs()) {
				found.put(m.getNameAsString(), new AnnotationMemberModel(m.getNameAsString(), value(loader, m.getValue()),
						cl.method("value").map(v -> v.defaultValue()).orElse(AnnotationValue.NULL)));
			}
		}

		for (MethodModel m : cl.methods()) {
			if (found.containsKey(m.name()))
				continue;
			found.put(m.name(), new AnnotationMemberModel(m.name(), m.defaultValue(), m.defaultValue()));
		}

		this.members = new ArrayList<>(found.values());
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return a.toString();
	}

	@Override
	public Collection<AnnotationMemberModel> members() {
		return members;
	}

	/**
	 * @param loader
	 * @param v
	 * @return expression as AnnotationValue
	 */
	public static AnnotationValue value(ModelLoader loader, Expression v) {
		if (v.isNullLiteralExpr())
			return AnnotationValue.NULL;
		if (v.isStringLiteralExpr())
			return new AnnotationValueLiteral(v.asStringLiteralExpr().asString());
		if (v.isBooleanLiteralExpr())
			return new AnnotationValueLiteral(Boolean.toString(v.asBooleanLiteralExpr().getValue()));
		if (v.isIntegerLiteralExpr())
			return new AnnotationValueLiteral(v.asIntegerLiteralExpr().getValue());
		if (v.isLongLiteralExpr())
			return new AnnotationValueLiteral(v.asLongLiteralExpr().getValue());
		if (v.isCharLiteralExpr())
			return new AnnotationValueLiteral(v.asCharLiteralExpr().getValue());
		if (v.isDoubleLiteralExpr())
			return new AnnotationValueLiteral(v.asDoubleLiteralExpr().getValue());

		if (v.isFieldAccessExpr())
			return value(loader, v.asFieldAccessExpr().resolve());
		if (v.isNameExpr())
			return value(loader, v.asNameExpr().resolve());
		if (v.isClassExpr())
			return new AnnotationValueClass(loader.get(v.asClassExpr().getType().resolve().describe()));
		if (v.isArrayInitializerExpr()) {
			NodeList<Expression> values = v.asArrayInitializerExpr().getValues();
			AnnotationValue[] a = new AnnotationValue[values.size()];
			int i = 0;
			for (Expression e : values)
				a[i++] = value(loader, e);
			return new AnnotationValueArray(a);
		}
		if (v.isAnnotationExpr())
			return new AnnotationValueAnnotation(new AstAnnotation(loader, v.asAnnotationExpr()));

		throw new RuntimeException("unsuported value: " + v.getClass());
	}

	private static AnnotationValue value(ModelLoader loader, ResolvedValueDeclaration r) {
		if (r.isEnumConstant()) {
			ResolvedEnumConstantDeclaration e = r.asEnumConstant();
			TypeModel t = loader.get(e.getType().describe());
			return new AnnotationValueEnum(t.asEnum().entry(e.getName()).orElseThrow());
		}
		// TODO
//		if (r.isField()) {
//			ResolvedFieldDeclaration f = r.asField();
//			TypeModel t = loader.get(f.getType().describe());
//			if (t.isEnum())
//				return new AnnotationValueEnum(t.asEnum().entry(f.getName()).orElseThrow());
//			FieldModel field = asClass.field(f.getName());
//
//			throw new RuntimeException("Field access in annotation not supported");
//		}
		throw new RuntimeException("Annotation value '" + r + "' not supported");
	}
}
