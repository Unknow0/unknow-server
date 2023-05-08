/**
 * 
 */
package unknow.server.http.test;

import java.util.Arrays;

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

/**
 * @author unknow
 */
@Path("/rest/{q}")
@Produces("application/json")
public class Rest {

	@GET
	public void oneWay(@PathParam("q") String q, @BeanParam Bean bean, @QueryParam("p") String[] p) {
		System.out.println("oneWay>> q: '" + q + "' bean: " + bean + " p: " + Arrays.toString(p));
	}

	@GET
	@Consumes("text/*")
	public void onWay2(@PathParam("q") String q, @BeanParam Bean bean) {
		System.out.println("oneWay2>> q: '" + q + "' bean: " + bean);
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

	public static class Bean {
		@BeanParam
		public Login login;
		@CookieParam("sid")
		public String sid;
		@MatrixParam("key")
		public String p;

		public Truc truc;

		@Override
		public String toString() {
			return "Bean [login=" + login + ", sid=" + sid + ", p=" + p + ", truc=" + truc + "]";
		}
	}

	public static class Login {
		@HeaderParam("x-apikey")
		private String apikey;

		public String getApikey() {
			return apikey;
		}

		public void setApikey(String apikey) {
			this.apikey = apikey;
		}

		@Override
		public String toString() {
			return "Login [apikey=" + apikey + "]";
		}
	}
}
