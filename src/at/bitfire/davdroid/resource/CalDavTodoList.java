package at.bitfire.davdroid.resource;

import java.net.URISyntaxException;

import at.bitfire.davdroid.webdav.DavMultiget;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;

public class CalDavTodoList extends RemoteCollection<Task> { 
	@Override
	protected String memberContentType() {
		return "text/calendar";
	}

	@Override
	protected DavMultiget.Type multiGetType() {
		return DavMultiget.Type.CALENDAR;
	}
	
	@Override
	protected Task newResourceSkeleton(String name, String ETag) {
		return new Task(name, ETag);
	}
	
	
	public CalDavTodoList(CloseableHttpClient httpClient, String baseURL, String user, String password, boolean preemptiveAuth) throws URISyntaxException {
		super(httpClient, baseURL, user, password, preemptiveAuth);
	}
}
