package unknow.server.maven.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanProperty {
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

	public TypeModel type() {
		return getter.type();
	}

	public Optional<AnnotationModel> annotation(Class<?> clazz) {
		return annotation(clazz.getName());
	}

	public Optional<AnnotationModel> annotation(String name) {
		List<AnnotationModel> list = annotations(name);
		if (list.isEmpty())
			return Optional.empty();
		if (list.size() > 1)
			throw new IllegalArgumentException("Duplicate annotation " + name + " for property " + this);
		return Optional.of(list.get(0));
	}

	/**
	 * get annotation on field, getter and setter
	 * @param name
	 * @return
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
		Set<String> names = clazz.methods().stream().map(m -> m.name()).filter(m -> m.startsWith("set") || m.startsWith("get"))
				.map(m -> Character.toLowerCase(m.charAt(3)) + m.substring(4)).collect(Collectors.toSet());
		for (FieldModel f : clazz.fields()) {
			if (!f.isStatic())
				names.add(f.name());
		}

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
