/**
 * $Id$
 */
package cl.quilt.tapestry.akismet.services;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;

import ac.simons.akismet.AkismetImpl;

/**
 * @author raul
 * 
 */
public class AkismetModule {
	
	public static void bind(ServiceBinder binder) {
		binder.bind(Akismet.class, AkismetImpl.class);
	}

	public HttpClient buildHttpClient() {
		return new DefaultHttpClient();
	}
	
	public static void contributeFactoryDefaults(MappedConfiguration<String, Object> configuration) {
		configuration.add(AkismetSymbols.AKISMET_COMPATIBLE_SERVICE, KnownAkismetCompatibleServices.TYPEPAD_ANTISPAM.toString());
		configuration.add(AkismetSymbols.API_CONSUMER, "");
		configuration.add(AkismetSymbols.API_KEY, "");
		configuration.add(AkismetSymbols.API_ENDPOINT, "");
	}

}
