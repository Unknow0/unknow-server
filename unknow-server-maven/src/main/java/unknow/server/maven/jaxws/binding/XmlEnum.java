/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import jakarta.xml.bind.annotation.XmlEnumValue;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.EnumModel.EnumConstant;

/**
 * @author unknow
 */
public class XmlEnum extends XmlType<EnumModel> {
	public final List<XmlEnumEntry> entries;
	public final String convertMethod;

	public XmlEnum(EnumModel type, String ns, String name, String convertMethod) {
		super(type, ns, name);
		this.convertMethod = convertMethod;
		entries = new ArrayList<>();
		for (EnumConstant e : type.entries())
			entries.add(new XmlEnumEntry(e.name(), e.annotation(XmlEnumValue.class).flatMap(a -> a.value()).orElse(e.name())));
	}

	@Override
	public Expression fromString(TypeCache types, Expression v) {
		return new MethodCallExpr(null, convertMethod, Utils.list(v));
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(null, convertMethod, Utils.list(v));
	}

	public static final class XmlEnumEntry {
		public final String name;
		public final String value;

		public XmlEnumEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}