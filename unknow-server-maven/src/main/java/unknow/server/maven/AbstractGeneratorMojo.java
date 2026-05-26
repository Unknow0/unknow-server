/**
 * 
 */
package unknow.server.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;

import unknow.maven.codegen.AbstractCodeGenMojo;
import unknow.maven.codegen.CodeGenConfig;

/**
 * @author unknow
 */
public abstract class AbstractGeneratorMojo extends AbstractCodeGenMojo {
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	protected MavenSession session;
	@Parameter(defaultValue = "${mojo}", required = true, readonly = true)
	protected MojoExecution mojo;
	@Component
	protected RepositorySystem repository;

	@Parameter
	protected CodeGenConfig codegen;
}
