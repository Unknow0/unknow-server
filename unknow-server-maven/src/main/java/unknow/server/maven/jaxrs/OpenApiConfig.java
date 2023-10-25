package unknow.server.maven.jaxrs;

import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class OpenApiConfig {
	private Info info;
	private List<Server> servers;
	private List<SecurityRequirement> security;
	private Map<String, SecurityScheme> securityScheme;
	private List<Tag> tags;
	private ExternalDocumentation externalDocs;

	public OpenAPI getSpec(MavenProject project) {
		OpenAPI spec = new OpenAPI().info(info == null ? info = new Info() : info);
		if (info.getTitle() == null)
			info.setTitle(project.getArtifactId());
		if (info.getVersion() == null)
			info.setVersion(project.getVersion());

		if (servers != null)
			spec.setServers(servers);
		if (security != null)
			spec.setSecurity(security);
		if (tags != null)
			spec.setTags(tags);
		if (externalDocs != null)
			spec.setExternalDocs(externalDocs);
		return spec.components(new Components().securitySchemes(securityScheme));
	}

	public Info getInfo() {
		return info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}

	public List<Server> getServers() {
		return servers;
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;
	}

	public List<SecurityRequirement> getSecurity() {
		return security;
	}

	public void setSecurity(List<SecurityRequirement> security) {
		this.security = security;
	}

	public Map<String, SecurityScheme> getSecurityScheme() {
		return securityScheme;
	}

	public void setSecurityScheme(Map<String, SecurityScheme> securityScheme) {
		this.securityScheme = securityScheme;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	public void setExternalDocs(ExternalDocumentation externalDocs) {
		this.externalDocs = externalDocs;
	}

}
