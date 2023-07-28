/**
 * 
 */
package unknow.server.http.test;

import java.util.Arrays;
import java.util.List;

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
@Path("/rest/{q}")
@Produces("application/json")
public class Rest {

	@GET
	public void oneWay(@PathParam("q") String q, @BeanParam Bean bean) {
		System.out.println("oneWay>> q: '" + q + "' bean: " + bean);
	}

	@GET
	@Consumes({ "application/json", "application/x-ndjson" })
	public Response response(@PathParam("q") String q, @BeanParam Bean bean) {
		System.out.println("response>> q: '" + q + "' bean: " + bean);
		return Response.status(200).entity("echo").build();
	}

	@POST
	public Truc call(Truc truc, @QueryParam("debug") String debug) {
		System.out.println("call>> truc: " + truc + ", debug: " + debug);
		return truc;
	}

	public static class Truc {
		public String v;

		@Override
		public String toString() {
			return "Truc [v=" + v + "]";
		}
	}

	public static enum E {
		a, b, c, d, e;
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
		public List<? extends E> d;

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
