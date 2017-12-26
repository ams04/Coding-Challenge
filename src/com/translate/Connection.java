package com.translate;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Connection {

	private String apiUrl = "";

	public Connection(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	/**
	 * Performs the http GET request to the API and returns the translated String.
	 * 
	 * @return The translated string.
	 * @throws IOException Handled for Http requests.
	 * @throws ClientProtocolException Handled for Http requests.
	 */
	public String execute() throws ClientProtocolException, IOException{

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(apiUrl);
		CloseableHttpResponse response = httpClient.execute(httppost);

		int status = response.getStatusLine().getStatusCode();

		Header[] alternate = response.getAllHeaders();
		String redirectedURL = alternate[0].toString();
		redirectedURL = redirectedURL.substring(10, redirectedURL.length());

		if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
			httppost = new HttpPost(redirectedURL);
			response = httpClient.execute(httppost);
			return "";
		}
		if (status == HttpStatus.SC_OK) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		}
		return "";
	}

}
