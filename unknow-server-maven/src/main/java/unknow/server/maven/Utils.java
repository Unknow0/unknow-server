/**
 * 
 */
package unknow.server.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import unknow.server.http.utils.ArrayMap;

/**
 * @author unknow
 */
public class Utils {
	@SuppressWarnings("rawtypes")
	private static final NodeList EMPTYLIST = new NodeList<>();

	private Utils() {
	}

	/**
	 * empty node list
	 * 
	 * @param <T> type
	 * @return empty node list
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Node> NodeList<T> emptyList() {
		return EMPTYLIST;
	}

	/**
	 * create a node list
	 * 
	 * @param <T> type of element
	 * @param t   the values
	 * @return the node list
	 */
	@SafeVarargs
	public static <T extends Node> NodeList<T> list(T... t) {
		return new NodeList<>(t);
	}

	/**
	 * new byte[]{values}
	 * 
	 * @param b the values
	 * @return the byte array creation
	 */
	public static Expression byteArray(byte[] b) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < b.length; i++)
			nodeList.add(new IntegerLiteralExpr(Byte.toString(b[i])));

		return array(PrimitiveType.byteType(), nodeList);
	}

	/**
	 * @param values
	 * @return new string[] {values}
	 */
	public static ArrayCreationExpr stringArray(String[] values) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < values.length; i++)
			nodeList.add(new StringLiteralExpr(values[i]));

		return array(new ClassOrInterfaceType(null, "String"), nodeList);
	}

	public static ArrayCreationExpr array(Type type, int... level) {
		NodeList<ArrayCreationLevel> l = new NodeList<>();
		for (int i = 0; i < level.length; i++)
			l.add(new ArrayCreationLevel(level[i]));
		return new ArrayCreationExpr(type, l, null);
	}

	/**
	 * create a new array
	 * 
	 * @param type the element types
	 * @param init the values
	 * @return the array creation
	 */
	public static ArrayCreationExpr array(Type type, NodeList<Expression> init) {
		return new ArrayCreationExpr(type, list(new ArrayCreationLevel()), new ArrayInitializerExpr(init));
	}

	/**
	 * T n = value
	 * 
	 * @param t     the variable type
	 * @param n     the variable name
	 * @param value the value
	 * @return the assignement
	 */
	public static AssignExpr assign(Type t, String n, Expression value) {
		return new AssignExpr(new VariableDeclarationExpr(t, n), value, Operator.ASSIGN);
	}

	public static AssignExpr create(ClassOrInterfaceType t, String n, NodeList<Expression> arg) {
		return assign(t, n, new ObjectCreationExpr(null, t, arg));
	}

	/**
	 * create a new ArrayMap
	 * 
	 * @param map   the map content
	 * @param types the typeCahe
	 * @return the array map creation
	 */
	public static ObjectCreationExpr mapString(Map<String, String> map, TypeCache types) {
		List<String> list = new ArrayList<>(map.keySet());
		Collections.sort(list);
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		for (String key : list) {
			k.add(new StringLiteralExpr(key));
			v.add(new StringLiteralExpr(map.get(key)));
		}
		return new ObjectCreationExpr(null, types.get(ArrayMap.class, TypeCache.EMPTY), list(array(types.get(String.class), k), array(types.get(String.class), v)));
	}
}