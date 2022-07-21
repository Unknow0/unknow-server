/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;

import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class XmlObject implements XmlType {
	public final String clazz;
	public final Factory factory;
	public final List<XmlField> attrs;
	public final List<XmlField> elems;
	public final XmlElem value;

	private final SchemaData schema;

	public XmlObject(String clazz, Factory factory, List<XmlField> attrs, List<XmlField> elems, XmlElem value, SchemaData schema) {
		this.clazz = clazz;
		this.factory = factory;
		this.attrs = attrs;
		this.elems = elems;
		this.value = value;
		this.schema = schema;
	}

	public String factoryClazz() {
		return factory == null ? null : factory.clazz;
	}

	public String factoryMethod() {
		return factory == null ? null : factory.method;
	}

	@Override
	public Expression convert(TypeCache types, Expression v) {
		return new CastExpr(types.get(clazz), v);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public String clazz() {
		return clazz;
	}

	@Override
	public SchemaData schema() {
		return schema;
	}

	public static class Factory {
		public final String clazz;
		public final String method;

		/**
		 * create new Factory
		 * 
		 * @param clazz
		 * @param method
		 */
		public Factory(String clazz, String method) {
			this.clazz = clazz;
			this.method = method;
		}
	}

	public static class XmlElem {
		protected final XmlType type;
		public final String getter;
		public final String setter;

		public XmlElem(XmlType type, String getter, String setter) {
			this.type = type;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type(), getter, setter);
		}

		public XmlType type() {
			if (type instanceof XmlTypeWrapper)
				return ((XmlTypeWrapper) type).delegate;
			return type;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof XmlElem))
				return false;
			XmlElem other = (XmlElem) obj;
			return Objects.equals(type(), other.type()) && Objects.equals(getter, other.getter) && Objects.equals(setter, other.setter);
		}

		@Override
		public String toString() {
			return "XmlElem [type=" + type() + ", getter=" + getter + ", setter=" + setter + "]";
		}
	}

	public static class XmlField extends XmlElem {
		public final String name;
		public final String ns;

		public XmlField(XmlType type, String getter, String setter, String name, String ns) {
			super(type, getter, setter);
			this.name = name;
			this.ns = ns;
		}

		public String qname() {
			return (ns == null || ns.isEmpty() ? "" : '{' + ns + '}') + name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof XmlField))
				return false;
			XmlField other = (XmlField) obj;
			return Objects.equals(type, other.type) && Objects.equals(getter, other.getter) && Objects.equals(setter, other.setter) && Objects.equals(name, other.name)
					&& Objects.equals(ns, other.ns);
		}

		@Override
		public String toString() {
			return "XmlField [type=" + type + ", name=" + name + ", ns=" + ns + ", getter=" + getter + ", setter=" + setter + "]";
		}
	}

}
