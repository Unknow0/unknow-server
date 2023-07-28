/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import unknow.server.maven.model.util.AncestrorIterable;
import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface ClassModel extends TypeModel, WithMod {

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
		ClassModel c = this;
		do {
			Optional<MethodModel> o = methods().stream().filter(m -> {
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
		String n = name();
		for (PrimitiveModel t : PrimitiveModel.PRIMITIVES) {
			if (n.equals(t.boxed()))
				return true;
		}
		return false;
	}
}