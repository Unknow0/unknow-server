package unknow.server.maven.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithType;

public class BeanProperty implements WithName, WithAnnotation, WithType {
	private static final Logger logger = LoggerFactory.getLogger(BeanProperty.class);

	private final String name;
	private final FieldModel field;
	private final MethodModel getter;
	private final MethodModel setter;

	public BeanProperty(String name, FieldModel field, MethodModel getter, MethodModel setter) {
		this.name = name;
		this.field = field;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public String name() {
		return name;
	}

	public FieldModel field() {
		return field;
	}

	public MethodModel getter() {
		return getter;
	}

	public MethodModel setter() {
		return setter;
	}

	@Override
	public TypeModel type() {
		return getter.type();
	}

	@Override
	public Optional<AnnotationModel> annotation(Class<?> clazz) {
		return annotation(clazz.getName());
	}

	@Override
	public Optional<AnnotationModel> annotation(String name) {
		List<AnnotationModel> list = annotations(name);
		if (list.isEmpty())
			return Optional.empty();
		if (list.size() > 1)
			throw new IllegalArgumentException("Duplicate annotation " + name + " for property " + this);
		return Optional.of(list.get(0));
	}

	/**
	 * get all annotation on the field or the getter or the setter (the first one that as annotation)
	 * 
	 * @return annotations of the property
	 */
	@Override
	public Collection<AnnotationModel> annotations() {
		Collection<AnnotationModel> annotations = Collections.emptyList();
		if (field != null)
			annotations = field.annotations();
		if (annotations.isEmpty())
			annotations = getter.annotations();
		if (annotations.isEmpty())
			annotations = setter.annotations();
		return annotations;
	}

	/**
	 * get annotation on field, getter and setter
	 * 
	 * @param name field name
	 * @return the annotations
	 */
	public List<AnnotationModel> annotations(String name) {
		List<AnnotationModel> list = new ArrayList<>(3);
		if (field != null)
			field.annotation(name).ifPresent(list::add);
		getter.annotation(name).ifPresent(list::add);
		setter.annotation(name).ifPresent(list::add);
		return list;
	}

	public static Collection<BeanProperty> properties(ClassModel clazz) {
		Set<String> names = new HashSet<>();

		for (MethodModel m : clazz.methods()) {
			if (m.isAbstract() || m.isStatic())
				continue;
			String n = m.name();
			if (n.startsWith("is") && m.parameters().isEmpty())
				names.add(Character.toLowerCase(n.charAt(2)) + n.substring(3));
			else if (n.startsWith("get") && m.parameters().isEmpty() || n.startsWith("set") && m.parameters().size() == 1)
				names.add(Character.toLowerCase(n.charAt(3)) + n.substring(4));
		}
		clazz.fields().stream().filter(f -> !f.isStatic()).forEach(f -> f.name());

		List<BeanProperty> list = new ArrayList<>();
		for (String n : names) {
			BeanProperty p = property(clazz, n);
			if (p != null)
				list.add(p);
		}
		return list;
	}

	@Override
	public String toString() {
		return getter.parent().name() + "." + name;
	}

	public static BeanProperty property(ClassModel clazz, String name) {
		String n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		MethodModel getter = clazz.method("get" + n).orElse(null);
		if (getter == null)
			getter = clazz.method(name).orElse(null);
		if (getter == null)
			getter = clazz.method("is" + n).filter(v -> v.type().isAssignableFrom(PrimitiveModel.BOOLEAN)).orElse(null);
		if (getter == null) {
			logger.info("Getter not found for property {} in {}", name, clazz);
			return null;
		}

		MethodModel setter = clazz.method("set" + n, getter.type()).orElse(null);
		if (setter == null)
			setter = clazz.method(name, getter.type()).orElse(null);
		if (setter == null) {
			logger.info("Setter not found matching {}", getter);
			return null;
		}
		FieldModel field = clazz.field(name);
		if (field != null && !getter.type().isAssignableFrom(field.type())) {
			logger.warn("Field {} don't match {}", field, getter);
			return null;
		}
		return new BeanProperty(name, field, getter, setter);
	}
}
