/**
 * $Id$
 */
package cl.quilt.tapestry.akismet.services;

/**
 * @author raul
 *
 */
public enum KnownAkismetCompatibleServices {
	
	/**
	 * Akismet (http://akismet.com/)
	 */
	AKISMET("rest.akismet.com"),
	
	/**
	 * TypePad Antispam (http://antispam.typepad.com/)
	 */
	TYPEPAD_ANTISPAM("api.antispam.typepad.com"),
	;
	
	private String apiEndpoint;
	
	/**
	 * @param apiEndpoint
	 */
	private KnownAkismetCompatibleServices(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}
	
	/**
	 * @return the apiEndpoint
	 */
	public String getApiEndpoint() {
		return apiEndpoint;
	}
	
	public static KnownAkismetCompatibleServices toEnum(String value) {
		try {
			return valueOf(value);
		} catch (Exception e) {
			return TYPEPAD_ANTISPAM;
		}
	}
	
}
