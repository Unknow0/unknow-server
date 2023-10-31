package unknow.server.maven.model.simple;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.util.WithParent;

public class SimpleParam<T extends WithParent<ClassModel>> extends SimpleWithAnnotation implements ParamModel<T> {
	private final T parent;
	private final String name;
	private final TypeModel type;
	private final int index;

	public SimpleParam(T parent, String name, TypeModel type, int index) {
		this.parent = parent;
		this.name = name;
		this.type = type;
		this.index = index;
	}

	@Override
	public TypeModel type() {
		return type;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public T parent() {
		return parent;
	}

	@Override
	public int index() {
		return index;
	}
}
