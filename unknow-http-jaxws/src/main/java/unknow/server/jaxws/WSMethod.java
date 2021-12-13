package unknow.server.jaxws;

public interface WSMethod {
	Envelope call(Envelope e) throws Exception;
}
