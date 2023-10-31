package unknow.server.maven.model.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.util.WithAnnotation;

public class SimpleWithAnnotation implements WithAnnotation {
	private final List<AnnotationModel> annotations = new ArrayList<>(0);

	public SimpleAnnotation withAnnotation(Class<?> clazz) {
		return withAnnotation(clazz.getName());
	}

	public SimpleAnnotation withAnnotation(String name) {
		SimpleAnnotation a = new SimpleAnnotation(name);
		annotations.add(a);
		return a;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return annotations;
	}
}
