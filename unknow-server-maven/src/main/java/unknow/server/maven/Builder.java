/**
 * 
 */
package unknow.server.maven;

import java.nio.charset.StandardCharsets;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

/**
 * @author unknow
 */
public abstract class Builder {
	public abstract void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types);

	@SuppressWarnings("rawtypes")
	static final NodeList EMPTYLIST = new NodeList<>();

	@SuppressWarnings("unchecked")
	public static <T extends Node> NodeList<T> emptyList() {
		return EMPTYLIST;
	}

	@SafeVarargs
	public static <T extends Node> NodeList<T> list(T... t) {
		return new NodeList<>(t);
	}

	public static Expression byteArray(byte[] b) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < b.length; i++)
			nodeList.add(new IntegerLiteralExpr(Byte.toString(b[i])));

		return array(PrimitiveType.byteType(), nodeList);
	}

	public static ArrayCreationExpr array(Type type, NodeList<Expression> init) {
		return new ArrayCreationExpr(type, list(new ArrayCreationLevel()), new ArrayInitializerExpr(init));
	}

	public static AssignExpr assign(Type t, String n, Expression value) {
		return new AssignExpr(new VariableDeclarationExpr(t, n), value, Operator.ASSIGN);
	}
}
