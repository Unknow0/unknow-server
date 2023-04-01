/**
 * 
 */
package unknow.server.http.test;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * @author unknow
 */
@Path("/rest/{q}")
public class Rest {

	@GET
	public void oneWay(@PathParam("q") String q, @BeanParam Bean bean) {

	}

	@POST
	public Truc call(Truc truc, @QueryParam("debug") String debug) {
		return truc;
	}

	public static class Truc {
		public String v;
	}

	public static class Bean {
		@BeanParam
		public Login login;
		@CookieParam("sid")
		public String sid;
		@MatrixParam("key")
		public String p;

		public Truc truc;
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
	}
}