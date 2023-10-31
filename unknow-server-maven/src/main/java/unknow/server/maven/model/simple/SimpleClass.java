package unknow.server.maven.model.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.PackageModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.TypeParamModel;
import unknow.server.maven.model.jvm.JvmMod;
import unknow.server.maven.model.jvm.JvmModelLoader;

public class SimpleClass extends SimpleWithAnnotation implements ClassModel, JvmMod {
	private final String name;
	private final int mod;
	private final List<FieldModel> fields = new ArrayList<>();
	private final List<MethodModel> methods = new ArrayList<>();
	private SimplePackage parent;

	public SimpleClass(String name, int mod) {
		this.name = name;
		this.mod = mod;
		int i = name.lastIndexOf(".");
		this.parent = new SimplePackage(i < 0 ? "" : name.substring(i + 1));
	}

	public final SimpleMethod withMethod(String name, int mod, TypeModel type) {
		SimpleMethod m = new SimpleMethod(this, name, mod, type);
		methods.add(m);
		return m;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public PackageModel parent() {
		return parent;
	}

	@Override
	public int mod() {
		return mod;
	}

	@Override
	public ClassModel superType() {
		return JvmModelLoader.GLOBAL.get("java.lang.Object").asClass();
	}

	@Override
	public List<ClassModel> interfaces() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ConstructorModel> constructors() {
		return Collections.emptyList();
	}

	@Override
	public Collection<FieldModel> fields() {
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		return methods;
	}

	@Override
	public List<TypeParamModel> parameters() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return name;
	}
}
