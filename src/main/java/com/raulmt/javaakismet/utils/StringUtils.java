/**
 * $Id$
 */
package com.raulmt.javaakismet.utils;

/**
 * @author raul
 *
 */
public class StringUtils {

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
