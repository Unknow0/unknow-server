/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;

import unknow.server.maven.model.AnnotationModel;

/**
 * @author unknow
 */
public class AstAnnotation implements AnnotationModel {
	private static final String[] EMPTY = {};
	private static final Function<Expression, String> TOSTRING = v -> {
		if (v.isStringLiteralExpr())
			return v.asStringLiteralExpr().asString();
		if (v.isNameExpr())
			return v.asNameExpr().getNameAsString();
		if (v.isFieldAccessExpr())
			return v.asFieldAccessExpr().getNameAsString();
		if (v.isBooleanLiteralExpr())
			return Boolean.toString(v.asBooleanLiteralExpr().getValue());
		if (v.isIntegerLiteralExpr())
			return v.asIntegerLiteralExpr().getValue();
		if (v.isClassExpr())
			return v.asClassExpr().getType().resolve().describe();
		throw new RuntimeException("unsuported value: " + v.getClass());
	};
	private static final Function<Expression, String[]> TOARRAY = v -> {
		if (v.isArrayInitializerExpr())
			return v.asArrayInitializerExpr().getValues().stream().map(TOSTRING).filter(s -> s != null).collect(Collectors.toList()).toArray(EMPTY);
		String s = TOSTRING.apply(v);
		if (s != null)
			return new String[] { s };
		return null;
	};

	private final String name;
	private final AnnotationExpr a;

	public AstAnnotation(AnnotationExpr a) {
		this.name = a.resolve().getQualifiedName();
		this.a = a;
	}

	@Override
	public String name() {
		return name;
	}

	private <T> Optional<T> value(String name, Function<Expression, T> f) {
		if (a.isSingleMemberAnnotationExpr())
			return name.equals("value") ? Optional.of(f.apply(a.asSingleMemberAnnotationExpr().getMemberValue())) : Optional.empty();
		return a.findFirst(MemberValuePair.class, m -> name.equals(m.getNameAsString())).map(m -> f.apply(m.getValue()));
	}

	@Override
	public Optional<String> value(String name) {
		return value(name, TOSTRING);
	}

	@Override
	public Optional<String[]> values(String name) {
		return value(name, TOARRAY);
	}
}
