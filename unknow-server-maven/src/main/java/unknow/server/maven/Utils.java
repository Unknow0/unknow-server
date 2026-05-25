/**
 * 
 */
package unknow.server.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import unknow.maven.codegen.CodeGenUtils;
import unknow.maven.codegen.TypeFactory;
import unknow.server.util.data.ArrayMap;
import unknow.server.util.data.ArraySet;

/**
 * @author unknow
 */
public class Utils {
	private Utils() {
	}

	/**
	 * create a new ArrayMap
	 * 
	 * @param map the map content
	 * @param types the typeCahe
	 * @return the array map creation
	 */
	public static ObjectCreationExpr mapInteger(Map<String, Integer> map, TypeFactory types) {
		List<String> list = new ArrayList<>(map.keySet());
		Collections.sort(list);
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		for (String key : list) {
			k.add(CodeGenUtils.text(key));
			v.add(new IntegerLiteralExpr(map.get(key).toString()));
		}
		return new ObjectCreationExpr(null, types.getClass(ArrayMap.class, TypeFactory.EMPTY),
				CodeGenUtils.list(CodeGenUtils.array(types.getClass(String.class), k), CodeGenUtils.array(types.getClass(Integer.class), v)));
	}

	/**
	 * create a new ArrayMap
	 * 
	 * @param map the map content
	 * @param types the typeCahe
	 * @return the array map creation
	 */
	public static ObjectCreationExpr mapString(Map<String, String> map, TypeFactory types) {
		List<String> list = new ArrayList<>(map.keySet());
		Collections.sort(list);
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		for (String key : list) {
			k.add(CodeGenUtils.text(key));
			v.add(CodeGenUtils.text(map.get(key)));
		}
		return new ObjectCreationExpr(null, types.getClass(ArrayMap.class, TypeFactory.EMPTY),
				CodeGenUtils.list(CodeGenUtils.array(types.getClass(String.class), k), CodeGenUtils.array(types.getClass(String.class), v)));
	}

	public static ObjectCreationExpr arraySet(Collection<String> list, TypeFactory types) {
		NodeList<Expression> n = new NodeList<>();
		for (String p : list)
			n.add(CodeGenUtils.text(p));
		return new ObjectCreationExpr(null, types.getClass(ArraySet.class, TypeFactory.EMPTY), CodeGenUtils.list(CodeGenUtils.array(types.getClass(String.class), n)));
	}
}
