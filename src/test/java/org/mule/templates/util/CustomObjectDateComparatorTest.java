/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mule.templates.builders.SfdcObjectBuilder.aCustomObject;

import java.util.Map;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.mule.templates.util.CustomObjectDateComparator;

public class CustomObjectDateComparatorTest {

	@Test(expected = IllegalArgumentException.class)
	public void nullUserA() {
		Map<String, Object> customObjectA = null;

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		CustomObjectDateComparator.isAfter(customObjectA, customObjectB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullUserB() {
		Map<String, Object> customObjectA = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		Map<String, Object> customObjectB = null;

		CustomObjectDateComparator.isAfter(customObjectA, customObjectB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedUserA() throws TransformerException {

		Map<String, Object> customObjectA = aCustomObject().with("Id", "I0000323AE754F")
															.build();

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		CustomObjectDateComparator.isAfter(customObjectA, customObjectB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedUserB() throws TransformerException {

		Map<String, Object> customObjectA = aCustomObject().with("Id", "I0000323AE754F")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.build();

		CustomObjectDateComparator.isAfter(customObjectA, customObjectB);
	}

	@Test
	public void customObjectAIsAfterUserB() throws TransformerException {

		Map<String, Object> customObjectA = aCustomObject().with("Id", "I0000323AE754F")
															.with("LastModifiedDate", "2013-12-10T22:15:33.001Z")
															.build();

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		assertTrue("The customObject A should be after the customObject B", CustomObjectDateComparator.isAfter(customObjectA, customObjectB));
	}

	@Test
	public void customObjectAIsNotAfterUserB() throws TransformerException {

		Map<String, Object> customObjectA = aCustomObject().with("Id", "I0000323AE754F")
															.with("LastModifiedDate", "2013-12-08T22:15:33.001Z")
															.build();

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		assertFalse("The customObject A should not be after the customObject B", CustomObjectDateComparator.isAfter(customObjectA, customObjectB));
	}

	@Test
	public void customObjectAIsTheSameThatUserB() throws TransformerException {

		Map<String, Object> customObjectA = aCustomObject().with("Id", "I0000323AE754F")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		Map<String, Object> customObjectB = aCustomObject().with("Id", "I000032300ESE")
															.with("LastModifiedDate", "2013-12-09T22:15:33.001Z")
															.build();

		assertFalse("The customObject A should not be after the customObject B", CustomObjectDateComparator.isAfter(customObjectA, customObjectB));
	}

}
