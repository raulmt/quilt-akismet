/**
 * Created by Michael Simons, michael-simons.eu
 * and released under The BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * Copyright (c) 2011, Michael Simons
 * All rights reserved.
 * 
 * Modified by Raúl Montes, Quilt Technologies Ltda.
 * 
 * Redistribution  and  use  in  source   and  binary  forms,  with  or   without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source   code must retain   the above copyright   notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary  form must reproduce  the above copyright  notice,
 *   this list of conditions  and the following  disclaimer in the  documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name  of  michael-simons.eu   nor the names  of its contributors
 *   may be used  to endorse   or promote  products derived  from  this  software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS  PROVIDED BY THE  COPYRIGHT HOLDERS AND  CONTRIBUTORS "AS IS"
 * AND ANY  EXPRESS OR  IMPLIED WARRANTIES,  INCLUDING, BUT  NOT LIMITED  TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL  THE COPYRIGHT HOLDER OR CONTRIBUTORS  BE LIABLE
 * FOR ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL,  EXEMPLARY, OR  CONSEQUENTIAL
 * DAMAGES (INCLUDING,  BUT NOT  LIMITED TO,  PROCUREMENT OF  SUBSTITUTE GOODS OR
 * SERVICES; LOSS  OF USE,  DATA, OR  PROFITS; OR  BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE  USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ac.simons.akismet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.slf4j.Logger;

import com.raulmt.javaakismet.utils.StringUtils;

import cl.quilt.tapestry.akismet.services.Akismet;
import cl.quilt.tapestry.akismet.services.AkismetSymbols;
import cl.quilt.tapestry.akismet.services.KnownAkismetCompatibleServices;

/**
 * Loosely oriented at http://akismet.com/development/api/
 * 
 * Optimized version for a Tapestry IOC Service
 * 
 * @author Michael J. Simons
 * @author Raúl Montes
 */
public class AkismetImpl<T> implements Akismet<T> {

	/**
	 * Version of Akismet API this library supports
	 */
	private static final String API_VERSION = "1.1";

	/**
	 * The default Akismet Compatible Service to use
	 */
	private static final KnownAkismetCompatibleServices DEFAULT_AKISMET_SERVICE = KnownAkismetCompatibleServices.TYPEPAD_ANTISPAM;

	/**
	 * The default user agent
	 */
	private static final String DEFAULT_USER_AGENT = String.format("Java/%s java-akismet/0.0.2", System.getProperty("java.version"));

	/**
	 * The default content type
	 */
	private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

	private final Logger logger;

	/**
	 * {@link HttpClient} to make requests to the service
	 */
	private final HttpClient httpClient;
	
	/**
	 * {@link TypeCoercer} to coerce comments to {@link AkismetComment}s
	 */
	private final TypeCoercer coercer;

	/**
	 * API Endpoint
	 */
	private String apiEndpoint;

	/**
	 * The API key being verified for use with the API
	 */
	private String apiKey;

	/**
	 * A.k.a "blog". The front page or home URL of the instance making the
	 * request. For a blog, site, or wiki this would be the front page. Note:
	 * Must be a full URI, including http://.
	 */
	private String apiConsumer;

	/**
	 * If set to false, all comments are treated as ham and no Akismet calls are
	 * made
	 */
	private boolean enabled = true;

	public AkismetImpl(final HttpClient httpClient, final TypeCoercer coercer, Logger logger, @Symbol(AkismetSymbols.API_KEY) final String apiKey,
			@Symbol(AkismetSymbols.API_CONSUMER) final String apiConsumer,
			@Symbol(AkismetSymbols.AKISMET_COMPATIBLE_SERVICE) final String conpatibleService,
			@Symbol(AkismetSymbols.API_ENDPOINT) final String apiEndpoint) {

		this.httpClient = httpClient;
		this.coercer = coercer;
		this.logger = logger;


		try {
			new URL(apiConsumer);
			this.apiConsumer = apiConsumer;
		} catch (MalformedURLException e) {
			logger.warn("You should specify a compliant HTTP Address as an API Consumer (for example, http://example.org");
			this.apiConsumer = "";
		}

		if (!StringUtils.isBlank(apiEndpoint)) {
			this.apiEndpoint = apiEndpoint;
		} else if (!StringUtils.isBlank(conpatibleService)) {
			this.apiEndpoint = KnownAkismetCompatibleServices.toEnum(conpatibleService).getApiEndpoint();
		} else {
			this.apiEndpoint = DEFAULT_AKISMET_SERVICE.getApiEndpoint();
		}
		
		if (apiKey == null || apiKey.trim().isEmpty()) {
			disableService("You have to configure an API Key or the service will not be enabled. You can go to http://antispam.typepad.com/info/get-api-key.html and get one for free.");
		}
		
		this.apiKey = apiKey;
		
		// verify API key
		try {
			if (!verifyKey()) {
				disableService("Your API Key is not valid.");
			}
		} catch (AkismetException e) {
			disableService("Your API Key could not be verified.");
		}
	}
	
	/**
	 * The key verification call should be made before beginning to use the
	 * service. It requires two variables, key and blog.
	 * 
	 * @return True if the key is valid. This is the one call that can be made
	 *         without the API key subdomain.
	 * @throws AkismetException
	 */
	public boolean verifyKey() throws AkismetException {
		boolean rv = false;
		try {
			final HttpPost request = newHttpPostRequest(String.format("http://%s/%s/verify-key", apiEndpoint, API_VERSION));
			final List<NameValuePair> p = new ArrayList<NameValuePair>();
			p.add(new BasicNameValuePair("key", apiKey));
			p.add(new BasicNameValuePair("blog", apiConsumer));
			request.setEntity(new UrlEncodedFormEntity(p, "UTF-8"));
			final HttpResponse response = httpClient.execute(request);
			final String body = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
				rv = body.trim().equals("valid");
			else
				logger.warn(String.format("Something bad happened while verifying key, assuming key is invalid: %s", response
						.getStatusLine().getReasonPhrase()));
		} catch (Exception e) {
			throw new AkismetException(e);
		}
		return rv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ac.simons.akismet.Akismet#commentCheck(ac.simons.akismet.AkismetComment)
	 */
	@Override
	public boolean commentCheck(final T comment) throws AkismetException {
		// When in doubt, assume that the comment is ham
		boolean rv = false;
		if (enabled) {
			try {
				final HttpResponse response = this.callAkismet("comment-check", comment);
				final String body = EntityUtils.toString(response.getEntity());
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					rv = body.trim().equals("true");
				else
					logger.warn(String.format("Something bad happened while checking a comment, assuming comment is ham: %s", response
							.getStatusLine().getReasonPhrase()));
			} catch (Exception e) {
				throw new AkismetException(e);
			}
		}
		return rv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ac.simons.akismet.Akismet#submitSpam(ac.simons.akismet.AkismetComment)
	 */
	@Override
	public boolean submitSpam(final T comment) throws AkismetException {
		boolean rv = false;
		try {
			final HttpResponse response = this.callAkismet("submit-spam", comment);
			final String body = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
				logger.warn(String.format("Something bad happened while submitting Spam: %s", response.getStatusLine().getReasonPhrase()));
			else {
				logger.debug(String.format("Spam successfully submitted, response was '%s'", body));
				rv = true;
			}
		} catch (Exception e) {
			throw new AkismetException(e);
		}
		return rv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ac.simons.akismet.Akismet#submitHam(ac.simons.akismet.AkismetComment)
	 */
	@Override
	public boolean submitHam(final T comment) throws AkismetException {
		boolean rv = false;
		try {
			final HttpResponse response = this.callAkismet("submit-ham", comment);
			final String body = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
				logger.warn(String.format("Something bad happened while submitting ham: %s", response.getStatusLine().getReasonPhrase()));
			else {
				logger.debug(String.format("Ham successfully submitted, response was '%s'", body));
				rv = true;
			}
		} catch (Exception e) {
			throw new AkismetException(e);
		}
		return rv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ac.simons.akismet.Akismet#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ac.simons.akismet.Akismet#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @param apiConsumer
	 *            the apiConsumer to set
	 */
	public void setApiConsumer(String apiConsumer) {
		this.apiConsumer = apiConsumer;
	}

	/**
	 * @param apiEndpoint
	 *            the apiEndpoint to set
	 */
	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	/**
	 * @param apiKey
	 *            the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	private HttpPost newHttpPostRequest(final String uri) {
		final HttpPost request = new HttpPost(uri);
		request.setHeader("User-Agent", DEFAULT_USER_AGENT);
		request.setHeader("Content-Type", DEFAULT_CONTENT_TYPE);
		return request;
	}

	private HttpResponse callAkismet(final String function, final T comment) throws Exception {
		AkismetComment akismetComment = coercer.coerce(comment, AkismetComment.class);
		final HttpPost request = newHttpPostRequest(String.format("http://%s.%s/%s/%s", apiKey, apiEndpoint, API_VERSION, function));
		request.setEntity(akismetComment.toEntity(apiConsumer));
		return httpClient.execute(request);
	}

	private void disableService(String message) {
		logger.error(message);
		this.enabled = false;
	}
}