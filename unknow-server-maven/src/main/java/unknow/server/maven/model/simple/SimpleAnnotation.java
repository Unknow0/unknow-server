package unknow.server.maven.model.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import unknow.server.maven.model.AnnotationMemberModel;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.AnnotationValue;
import unknow.server.maven.model.AnnotationValue.AnnotationValueClass;
import unknow.server.maven.model.AnnotationValue.AnnotationValueLiteral;
import unknow.server.maven.model.TypeModel;

public class SimpleAnnotation implements AnnotationModel {
	private final String name;
	private final List<AnnotationMemberModel> members;

	public SimpleAnnotation(String name) {
		this.name = name;
		this.members = new ArrayList<>(0);
	}

	public SimpleAnnotation withLiteral(String name, String value) {
		return withLiteral(name, value, null);
	}

	public SimpleAnnotation withLiteral(String name, String value, String defValue) {
		AnnotationValue def = defValue == null ? AnnotationValue.NULL : new AnnotationValueLiteral(defValue);
		members.add(new AnnotationMemberModel(name, new AnnotationValueLiteral(value), def));
		return this;
	}

	public SimpleAnnotation withClass(String name, TypeModel type) {
		return withClass(name, type, null);
	}

	public SimpleAnnotation withClass(String name, TypeModel type, TypeModel defValue) {
		AnnotationValue def = defValue == null ? AnnotationValue.NULL : new AnnotationValueClass(defValue);
		members.add(new AnnotationMemberModel(name, new AnnotationValueClass(type), def));
		return this;
	}

	public SimpleAnnotationArray withArray(String name) {
		SimpleAnnotationArray a = new SimpleAnnotationArray();
		members.add(new AnnotationMemberModel(name, a, AnnotationValue.NULL));
		return a;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Collection<AnnotationMemberModel> members() {
		return members;
	}
}
