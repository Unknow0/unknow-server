package unknow.server.maven.model;

import unknow.server.maven.model.util.WithName;

public class AnnotationMemberModel implements WithName, AnnotationValue {
	private final String name;
	private final AnnotationValue value;
	private final AnnotationValue defaultValue;

	public AnnotationMemberModel(String name, AnnotationValue value, AnnotationValue defaultValue) {
		this.name = name;
		this.value = value;
		this.defaultValue = defaultValue;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public AnnotationValue[] asArray() {
		return value.asArray();
	}

	@Override
	public TypeModel asClass() {
		return value.asClass();
	}

	@Override
	public String asLiteral() {
		return value.asLiteral();
	}

	@Override
	public AnnotationModel asAnnotation() {
		return value.asAnnotation();
	}

	public AnnotationValue defaultValue() {
		return defaultValue;
	}

	public boolean isSet() {
		return !value.valueEquals(defaultValue);
	}

	@Override
	public boolean valueEquals(AnnotationValue a) {
		return value.valueEquals(a);
	}

}
