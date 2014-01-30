package org.mule.kicks.util;

import java.util.Map;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * The function of this class is to establish a relation happens before between
 * two maps representing SFDC customObjects.
 * 
 * It's assumed that these maps are well formed maps from SFDC thus they both
 * contain an entry with the expected key. Never the less validations are being
 * done.
 * 
 * @author damiansima
 */
public class CustomObjectDateComparator {
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";

	/**
	 * Validate which customObject has the latest last modification date.
	 * 
	 * @param customObjectA
	 *            SFDC customObject map
	 * @param customObjectB
	 *            SFDC customObject map
	 * @return true if the last modified date from customObjectA is after the one
	 *         from customObject B
	 */
	public static boolean isAfter(Map<String, Object> customObjectA, Map<String, Object> customObjectB) {
		Validate.notNull(customObjectA, "The customObject A should not be null");
		Validate.notNull(customObjectB, "The customObject B should not be null");

		Validate.isTrue(customObjectA.containsKey(LAST_MODIFIED_DATE), "The customObject A map should containt the key " + LAST_MODIFIED_DATE);
		Validate.isTrue(customObjectB.containsKey(LAST_MODIFIED_DATE), "The customObject B map should containt the key " + LAST_MODIFIED_DATE);

		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		DateTime lastModifiedDateOfA = formatter.parseDateTime(String.valueOf(customObjectA.get(LAST_MODIFIED_DATE)));
		DateTime lastModifiedDateOfB = formatter.parseDateTime(String.valueOf(customObjectB.get(LAST_MODIFIED_DATE)));

		return lastModifiedDateOfA.isAfter(lastModifiedDateOfB);
	}
}
