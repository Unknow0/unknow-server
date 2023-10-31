/**
 * 
 */
package unknow.server.maven.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.maven.model.util.AncestrorIterable;
import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface ClassModel extends TypeModel, WithMod {
	static final Logger logger = LoggerFactory.getLogger(ClassModel.class);

	/**
	 * @return super type
	 */
	ClassModel superType();

	/**
	 * @return implemented interface
	 */
	List<ClassModel> interfaces();

	/**
	 * @return the declared constructors
	 */
	Collection<ConstructorModel> constructors();

	/**
	 * @param params the constructor params
	 * @return a constructors
	 */
	default Optional<ConstructorModel> constructors(TypeModel... params) {
		return constructors().stream().filter(m -> {
			if (m.parameters().size() != params.length)
				return false;
			int i = 0;
			for (ParamModel<ConstructorModel> p : m.parameters()) {
				if (!p.type().equals(params[i++]))
					return false;
			}
			return true;
		}).findFirst();
	}

	/**
	 * @return all the declared field
	 */
	Collection<FieldModel> fields();

	/**
	 * @param name field to get
	 * @return the declared field or null if not found
	 */
	default FieldModel field(String name) {
		for (FieldModel f : fields()) {
			if (name.equals(f.name()))
				return f;
		}
		return null;
	}

	/**
	 * @return declared methods
	 */
	Collection<MethodModel> methods();

	/**
	 * @param name   the method name
	 * @param params the method params
	 * @return the declared method
	 */
	default Optional<MethodModel> method(String name, TypeModel... params) {
		return methods().stream().filter(m -> {
			if (!name.equals(m.name()))
				return false;
			if (m.parameters().size() != params.length)
				return false;
			int i = 0;
			for (ParamModel<MethodModel> p : m.parameters()) {
				if (!p.type().equals(params[i++]))
					return false;
			}
			return true;
		}).findFirst();
	}

	/**
	 * @param name   the method name
	 * @param params the method params
	 * @return the method
	 */
	default Optional<MethodModel> findMethod(String name, TypeModel... params) {

		Predicate<MethodModel> f = m -> {
			if (!name.equals(m.name()))
				return false;
			if (m.parameters().size() != params.length)
				return false;
			int i = 0;
			for (ParamModel<MethodModel> p : m.parameters()) {
				if (!p.type().equals(params[i++]))
					return false;
			}
			return true;
		};
		ClassModel c = this;
		do {
			Optional<MethodModel> o = methods().stream().filter(f).findFirst();
			if (o.isPresent())
				return o;
			c = c.superType();
		} while (c != null);
		return Optional.empty();
	}

	/**
	 * @return generic param
	 */
	List<TypeParamModel> parameters();

	/**
	 * @param i index of the parameter to get
	 * @return get the i'th parameter
	 */
	default TypeParamModel parameter(int i) {
		return parameters().get(i);
	}

	/**
	 * @see unknow.server.maven.model.util.AncestrorIterator
	 * @return ancestors
	 */
	default Iterable<ClassModel> ancestor() {
		return new AncestrorIterable(this);
	}

	/**
	 * @param t ancestor to get
	 * @return the ancestor
	 */
	default ClassModel ancestor(TypeModel t) {
		if (!t.isClass())
			return null;
		return ancestor(t.name());
	}

	/**
	 * @param cl ancestor to get
	 * @return the ancestor
	 */
	default ClassModel ancestor(String cl) {
		for (ClassModel p : ancestor()) {
			if (cl.equals(p.name()))
				return p;
		}
		return null;
	}

	@Override
	default boolean isAssignableFrom(TypeModel t) {
		if (!t.isClass())
			return false;

		for (ClassModel p : t.asClass().ancestor()) {
			if (p.name().equals(name()))
				return true;
		}
		return false;
	}

	@Override
	default boolean isAssignableTo(String cl) {
		for (ClassModel p : ancestor()) {
			if (cl.equals(p.name()))
				return true;
		}
		return false;
	}

	/**
	 * @return the full name with the parameters
	 */
	@Override
	String toString();

	/**
	 * @return true this class is a boxed type for a primitive (Integer, Character, Double, ect..)
	 */
	default boolean isBoxedPrimitive() {
		return unboxed() != null;
	}

	/**
	 * @return the unboxed type or null if it's not a boxed type (Integer, Character, Double, ect..)
	 */
	default PrimitiveModel unboxed() {
		for (PrimitiveModel t : PrimitiveModel.PRIMITIVES) {
			if (this.equals(t.boxed()))
				return t;
		}
		return null;
	}

	/**
	 * @return the list of BeanProperty on this class
	 */
	default Collection<BeanProperty> properties() {
		Set<String> names = methods().stream().map(m -> m.name()).filter(m -> m.startsWith("set") || m.startsWith("get"))
				.map(m -> Character.toLowerCase(m.charAt(3)) + m.substring(4)).collect(Collectors.toSet());
		for (FieldModel f : fields()) {
			if (!f.isStatic())
				names.add(f.name());
		}

		List<BeanProperty> list = new ArrayList<>();
		for (String n : names) {
			BeanProperty p = property(n);
			if (p != null)
				list.add(p);
		}
		return list;
	}

	/**
	 * get a BeanProperty on this class
	 * @param name property name
	 * @return the BeanProperty or null if not found
	 */
	default BeanProperty property(String name) {
		String n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		MethodModel getter = method("get" + n).orElse(null);
		if (getter == null)
			getter = method(name).orElse(null);
		if (getter == null) {
			logger.info("Getter not found for property {} in {}", name, this);
			return null;
		}

		MethodModel setter = method("set" + n, getter.type()).orElse(null);
		if (setter == null)
			setter = method(name, getter.type()).orElse(null);
		if (setter == null) {
			logger.info("Setter not found matching {}", getter);
			return null;
		}
		FieldModel field = field(name);
		if (field != null && !getter.type().isAssignableFrom(field.type())) {
			logger.warn("Field {} don't match {}", field, getter);
			return null;
		}
		return new BeanProperty(name, field, getter, setter);
	}
}