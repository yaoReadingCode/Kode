package com.vaani.java.apache;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.HttpContext;

public class ApacheAsyncClient implements ILogger {
	private CloseableHttpAsyncClient client;

	public ApacheAsyncClient() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(3000).setConnectTimeout(3000).build();
		client = HttpAsyncClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setMaxConnPerRoute(1000).setMaxConnTotal(1000).build();
		client.start();
	}

	//
	// public ApacheHttpClient(final String userName, final String password) {
	// client = HttpClientBuilder
	// .create()
	// .setRedirectStrategy(getRedirectStrategy())
	// .setRetryHandler(new DefaultHttpRequestRetryHandler(3, false))
	// .setDefaultCredentialsProvider(
	// getCredentials(userName, password))
	// .setHostnameVerifier(getHostNameVerifer())
	// .setSslcontext(getTrustedFactory())
	// .build();
	// }

	public void close() {
		try {
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static RedirectStrategy getRedirectStrategy() {
		return new DefaultRedirectStrategy() {
			public boolean isRedirected(HttpRequest request,
					HttpResponse response, HttpContext context) {
				boolean isRedirect = false;
				try {
					isRedirect = super.isRedirected(request, response, context);
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				if (!isRedirect) {
					int responseCode = response.getStatusLine().getStatusCode() / 100;
					if (responseCode == 3) {
						return true;
					}
				}
				return isRedirect;
			}
		};
	}

	private static SSLContext getTrustedFactory() {
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) {
			}
		} };
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, trustAllCerts, new SecureRandom());
			return context;
		} catch (GeneralSecurityException e) {
			IOException ioException = new IOException(
					"Security exception configuring SSL context");
			ioException.initCause(e);
			throw new RuntimeException(ioException);
		}
	}

	public HttpResponse get(final String url) throws ClientProtocolException,
			IOException {
		return get(url, null);
	}

	public HttpResponse get(final String url,
			Map<String, String> additionalHeader, int i)
			throws ClientProtocolException, IOException, InterruptedException,
			ExecutionException {
		if (url == null || url.trim().isEmpty()) {
			logger.error("URL cannot be null or empty");
			return null;
		}
		logger.debug("URL -> " + url);
		final HttpGet get = new HttpGet(url);
		for (String additional : additionalHeader.keySet()) {
			get.addHeader(additional, additionalHeader.get(additional));
		}

		Future<HttpResponse> future = client.execute(get, null);
		// and wait until a response is received
		HttpResponse response1 = future.get();
		logger.debug(get.getRequestLine() + "->" + response1.getStatusLine());
		return response1;

		// return getResponse(url,execute(get));
	}

	public void multiGetPost(String url, Map<String, String> getHeader) throws Exception {

		final HttpGet[] requests = new HttpGet[] { new HttpGet(
				"http://192.168.26.175:8080/examples/eye/abc10000.jsp") };
		final CountDownLatch latch = new CountDownLatch(1);
		for (int v = 0; v < 1000; v++) {
			client.execute(requests[0], new FutureCallback<HttpResponse>() {

				public void completed(final HttpResponse response) {
					latch.countDown();
					System.out.println(requests[0].getRequestLine() + "->"
							+ response.getStatusLine());
				}

				public void failed(final Exception ex) {
					latch.countDown();
					System.out.println(requests[0].getRequestLine() + "->" + ex);
				}

				public void cancelled() {
					latch.countDown();
					System.out.println(requests[0].getRequestLine()
							+ " cancelled");
				}

			});
		}
		latch.await();
		System.out.println("Shutting down");

		System.out.println("Done");
	}

	public HttpResponse get(final String url, final String authCredentials)
			throws ClientProtocolException, IOException {
		if (url == null || url.trim().isEmpty()) {
			logger.error("URL cannot be null or empty");
			return null;
		}
		logger.debug("URL -> " + url);
		HttpGet get = new HttpGet(url.trim());

		if (!(authCredentials == null || authCredentials.trim().isEmpty())) {
			get.addHeader(
					"Authorization",
					"Basic "
							+ Base64.encodeBase64String(authCredentials.trim()
									.getBytes()));
		}
		return null;
	}

	/**
	 * 
	 * @param url
	 * @param jsonBody
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse put(final String url, final String jsonBody)
//			throws ClientProtocolException, IOException {
//		return put(url, jsonBody, null);
//	}

	/**
	 * 
	 * @param url
	 * @param jsonBody
	 * @param authCredentials
	 *            in format "USERNAME:PASSWORD"
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse put(final String url, final String jsonBody,
//			final String authCredentials) throws ClientProtocolException,
//			IOException {
//		if (url == null || url.trim().isEmpty()) {
//			logger.error("URL cannot be null or empty");
//			return null;
//		}
//		logger.debug("URL -> " + url);
//		HttpPut put = new HttpPut(url.trim());
//		put.addHeader("Content-Type", "application/json");
//		put.addHeader("Accept", "application/json");
//		if (!(authCredentials == null || authCredentials.trim().isEmpty())) {
//			put.addHeader(
//					"Authorization",
//					"Basic "
//							+ Base64.encodeBase64String(authCredentials.trim()
//									.getBytes()));
//		}
//		put.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
//		HttpResponse response = getResponse(url, execute(put));
//		return response;
//	}

	/**
	 * 
	 * @param url
	 * @param body
	 * @param authCredentials
	 *            in format "USERNAME:PASSWORD"
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse put(final String url, final List<NameValuePair> body,
//			final String authCredentials) throws ClientProtocolException,
//			IOException {
//		if (url == null || url.trim().isEmpty()) {
//			logger.error("URL cannot be null or empty");
//			return null;
//		}
//		logger.debug("URL -> " + url);
//		HttpPut put = new HttpPut(url.trim());
//		put.addHeader("Content-Type", "application/x-www-form-urlencoded");
//		if (!(authCredentials == null || authCredentials.trim().isEmpty())) {
//			put.addHeader(
//					"Authorization",
//					"Basic "
//							+ Base64.encodeBase64String(authCredentials.trim()
//									.getBytes()));
//		}
//		put.addHeader("Connection", "keep-alive");
//		put.addHeader("X-Requested-With", "XMLHttpRequest");
//		put.addHeader(
//				"User-Agent",
//				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36");
//		put.setEntity(new UrlEncodedFormEntity(body));
//		return getResponse(url, execute(put));
//	}

	/**
	 * 
	 * @param url
	 * @param jsonBody
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse post(final String url, final String jsonBody)
//			throws ClientProtocolException, IOException {
//		return post(url, jsonBody, null);
//	}

	/**
	 * 
	 * @param url
	 * @param jsonBody
	 * @param authCredentials
	 *            in format "USERNAME:PASSWORD"
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse post(final String url, final String jsonBody,
//			final String authCredentials) throws ClientProtocolException,
//			IOException {
//		if (url == null || url.trim().isEmpty()) {
//			logger.error("URL cannot be null or empty");
//			return null;
//		}
//		logger.debug("URL -> " + url);
//		HttpPost post = new HttpPost(url.trim());
//		post.addHeader("Content-Type", "application/json");
//		post.addHeader("Accept", "application/json");
//		if (!(authCredentials == null || authCredentials.trim().isEmpty())) {
//			post.addHeader(
//					"Authorization",
//					"Basic "
//							+ Base64.encodeBase64String(authCredentials.trim()
//									.getBytes()));
//		}
//		post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
//		return getResponse(url, execute(post));
//	}

	/**
	 * 
	 * @param url
	 * @param body
	 * @param authCredentials
	 *            in format "USERNAME:PASSWORD"
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse post(final String url, final List<NameValuePair> body)
//			throws ClientProtocolException, IOException {
//		return post(url, body, null);
//	}

	/**
	 * 
	 * @param url
	 * @param body
	 * @param authCredentials
	 *            in format "USERNAME:PASSWORD"
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
//	public HttpResponse post(final String url, final List<NameValuePair> body,
//			final String authCredentials) throws ClientProtocolException,
//			IOException {
//		if (url == null || url.trim().isEmpty()) {
//			logger.error("URL cannot be null or empty");
//			return null;
//		}
//		logger.debug("URL -> " + url);
//		HttpPost post = new HttpPost(url.trim());
//		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
//		if (!(authCredentials == null || authCredentials.trim().isEmpty())) {
//			post.addHeader(
//					"Authorization",
//					"Basic "
//							+ Base64.encodeBase64String(authCredentials.trim()
//									.getBytes()));
//		}
//		post.addHeader("Connection", "keep-alive");
//		post.addHeader("X-Requested-With", "XMLHttpRequest");
//		post.addHeader(
//				"User-Agent",
//				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36");
//		post.setEntity(new UrlEncodedFormEntity(body));
//		return getResponse(url, execute(post));
//	}

	private HttpResponse getResponse(final String url,
			final HttpResponse response) {
		if (response == null) {
			logger.error("Response is null...");
			return null;
		}
		int responseCode = response.getStatusLine().getStatusCode();
		switch (responseCode / 100) {
		case 2:
			return response;
		case 4:
		case 5:
			logger.error("URL -> " + url + "          Status Code -> "
					+ responseCode + "          Status Message -> "
					+ response.getStatusLine().getReasonPhrase());
			return null;
		}
		return null;
	}

//	private synchronized HttpResponse execute(final HttpUriRequest request)
//			throws ClientProtocolException, IOException {
//		if (request == null) {
//			logger.error("Request cannot be NULL");
//			return null;
//		}
//		return client.execute(request);
//	}
}