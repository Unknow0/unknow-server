/**
 * 
 */
package unknow.server.maven.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.body.TypeDeclaration;

import unknow.server.maven.model.impl.AstClass;
import unknow.server.maven.model.impl.AstEnum;
import unknow.server.maven.model.impl.JvmClass;
import unknow.server.maven.model.impl.VoidModel;

/**
 * @author unknow
 */
public class ModelLoader {
	private static final Collection<TypeModel> BUILTIN = Arrays.asList(VoidModel.SELF, PrimitiveModel.BOOLEAN, PrimitiveModel.BYTE, PrimitiveModel.CHAR, PrimitiveModel.SHORT, PrimitiveModel.INT, PrimitiveModel.LONG, PrimitiveModel.FLOAT, PrimitiveModel.DOUBLE);

	private final Map<String, TypeDeclaration<?>> classes;
	private final Map<String, TypeModel> models;

	public ModelLoader(Map<String, TypeDeclaration<?>> classes) {
		this.classes = classes;
		this.models = new HashMap<>();
		for (TypeModel t : BUILTIN)
			this.models.put(t.name(), t);
	}

	public TypeModel get(String cl) {
		TypeModel m = models.get(cl);
		if (m != null)
			return m;

		// TODO manage generic
		if (cl.endsWith("[]"))
			m = new ArrayModel(this, cl);
		else {
			TypeDeclaration<?> t = classes.get(cl);
			if (t != null) {
				if (t.isEnumDeclaration())
					m=new AstEnum(t.asEnumDeclaration());
				else if (t.isClassOrInterfaceDeclaration())
					m = new AstClass(this, t.asClassOrInterfaceDeclaration());
			} else {
				try {
					Class<?> c = Class.forName(cl);
					if (c.isEnum())
						;
					else
						m = new JvmClass(this, c);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
		models.put(cl, m);
		return m;
	}
}
