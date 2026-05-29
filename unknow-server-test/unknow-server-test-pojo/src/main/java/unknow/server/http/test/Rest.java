/**
 * 
 */
package unknow.server.http.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * @author unknow
 */
@Path("/")
@Produces("application/json")
public class Rest {
	private static final Logger logger = LoggerFactory.getLogger(Rest.class);

	@GET
	@Path("{q}/t")
	@SuppressWarnings("unused")
	public void t(@PathParam("q") String q) throws InterruptedException {
		Thread.sleep(3000);
		throw new NullPointerException("test");
	}

	@GET
	@Path("q/{v}")
	@SuppressWarnings("unused")
	public void q(@PathParam("v") String v) { // ok
	}

	@POST
	public void oneWay(@BeanParam Bean bean) throws Exception {
		logger.info("oneWay>> bean: {}", bean);
	}

	@PUT
	@Consumes({ "application/json", "application/x-ndjson" })
	public Response response(@FormParam("k") String k) {
		logger.info("response>> bean: {}", k);
		return Response.status(200).entity("echo").build();
	}

	@POST
	@Consumes({ "application/x-protobuf", "application/json", "application/jsonl", "application/x-ndjson" })
	@Produces({ "application/x-protobuf", "application/json", "application/jsonl", "application/x-ndjson" })
	public Truc call(Truc truc) {
		return truc;
	}

	@POST
	@Path("list")
	@Consumes({ "application/x-protobuf", "application/json", "application/jsonl", "application/x-ndjson" })
	@Produces({ "application/x-protobuf", "application/json", "application/jsonl", "application/x-ndjson" })
	public Collection<Truc> list(Collection<Truc> truc) {
		return truc;
	}

	public enum E {
		A, B, C, D, E;
	}

	public static class Bean {
		@BeanParam
		private Login login;
		private String sid;
		@MatrixParam("key")
		public String key;

		@QueryParam("p")
		public String[] p;

		@FormParam("d")
		public List<? super E> d;

		public Truc truc;

		public Login getLogin() {
			return login;
		}

		public void setLogin(Login login) {
			this.login = login;
		}

		@CookieParam("sid")
		public String getSid() {
			return sid;
		}

		public void setSid(String sid) {
			this.sid = sid;
		}

		public Truc getTruc() {
			return truc;
		}

		public void setTruc(Truc truc) {
			this.truc = truc;
		}

		@Override
		public String toString() {
			return "Bean [login=" + login + ", sid=" + sid + ", key=" + key + ", p=" + Arrays.toString(p) + ", d=" + d + ", truc=" + truc + "]";
		}
	}

	public static class Login {
		private String apikey;

		public String getApikey() {
			return apikey;
		}

		@HeaderParam("x-apikey")
		public void setApikey(String apikey) {
			this.apikey = apikey;
		}

		@Override
		public String toString() {
			return "Login [apikey=" + apikey + "]";
		}
	}
}
