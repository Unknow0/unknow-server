/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class PrimitiveModel implements TypeModel {
	public static final PrimitiveModel BOOLEAN = new PrimitiveModel("boolean");
	public static final PrimitiveModel BYTE = new PrimitiveModel("byte");
	public static final PrimitiveModel CHAR = new PrimitiveModel("char");
	public static final PrimitiveModel SHORT = new PrimitiveModel("short");
	public static final PrimitiveModel INT = new PrimitiveModel("int");
	public static final PrimitiveModel LONG = new PrimitiveModel("long");
	public static final PrimitiveModel FLOAT = new PrimitiveModel("float");
	public static final PrimitiveModel DOUBLE = new PrimitiveModel("double");

	private final String name;

	private PrimitiveModel(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
