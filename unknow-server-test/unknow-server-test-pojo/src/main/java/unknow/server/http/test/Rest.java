/**
 * 
 */
package unknow.server.http.test;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * @author unknow
 */
@Path("/{q}")
@Produces("application/json")
public class Rest {
	private static final Logger logger = LoggerFactory.getLogger(Rest.class);

	@GET
	@Path("t")
	public void t() throws InterruptedException {
		Thread.sleep(3000);
		throw new NullPointerException("test");
	}

	@GET
	@Path("q/{v}")
	public void q() { // ok
	}

	@GET
	public void oneWay(@PathParam("q") String q, @BeanParam Bean bean) {
		logger.info("oneWay>> q: '{}' bean: {}", q, bean);
	}

	@GET
	@Consumes({ "application/json", "application/x-ndjson" })
	public Response response(@PathParam("q") String q, @BeanParam Bean bean) {
		logger.info("response>> q: '{}' bean: {}", q, bean);
		return Response.status(200).entity("echo").build();
	}

	@POST
	public Truc call(Truc truc, @QueryParam("debug") String debug) {
		logger.info("call>> truc: {}, debug: {}", truc, debug);
		return truc;
	}

	public static class Truc {
		public String v;

		@Override
		public String toString() {
			return "Truc [v=" + v + "]";
		}
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

		@QueryParam("d")
		public List<? super E> d;

		public Truc[] truc;

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

		@Override
		public String toString() {
			return "Bean [login=" + login + ", sid=" + sid + ", key=" + key + ", p=" + Arrays.toString(p) + ", d=" + d + ", truc=" + Arrays.toString(truc) + "]";
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
