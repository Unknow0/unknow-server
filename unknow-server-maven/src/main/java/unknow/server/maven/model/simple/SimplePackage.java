
package unknow.server.maven.model.simple;

import unknow.server.maven.model.PackageModel;

public class SimplePackage extends SimpleWithAnnotation implements PackageModel {
	private final String name;

	public SimplePackage(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}
}
