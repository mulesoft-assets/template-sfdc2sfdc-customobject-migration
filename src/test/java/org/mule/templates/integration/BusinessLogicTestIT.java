/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;

import static org.mule.templates.builders.SfdcObjectBuilder.aCustomObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.BatchTestHelper;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Template that make calls to external systems.
 * 
 * @author cesar.garcia
 */
public class BusinessLogicTestIT extends AbstractTemplateTestCase {

	private static final String TEMPLATE_NAME = "sfdc2sfdc-customobject-migration";

	protected static final int TIMEOUT_SECONDS = 60;

	private BatchTestHelper helper;

	private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
	private static List<Map<String, Object>> createdCustomObjectsInA = new ArrayList<Map<String, Object>>();
	private static List<Map<String, Object>> createdCustomObjectsInB = new ArrayList<Map<String, Object>>();

	protected static final int TIMEOUT = 60;

	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	protected class BatchWaitListener implements BatchNotificationListener {

		public synchronized void onNotification(ServerNotification notification) {
			final int action = notification.getAction();

			if (action == BatchNotification.JOB_SUCCESSFUL || action == BatchNotification.JOB_STOPPED) {
				failed = false;
			} else if (action == BatchNotification.JOB_PROCESS_RECORDS_FAILED || action == BatchNotification.LOAD_PHASE_FAILED || action == BatchNotification.INPUT_PHASE_FAILED
					|| action == BatchNotification.ON_COMPLETE_FAILED) {

				failed = true;
			}
		}
	}

	@Before
	public void setUp() throws Exception {

		helper = new BatchTestHelper(muleContext);

		// Flow to retrieve custom objects from target system after syncing
		checkCustomObjectflow = getSubFlow("retrieveCustomObjectFlow");
		checkCustomObjectflow.initialise();

		createTestDataInSandBox();
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		// Create object in target system to be updated
		SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createCustomObjectFlowB");
		flowB.initialise();

		createdCustomObjectsInB = new ArrayList<Map<String, Object>>();
		// This custom object should BE synced (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInB.add(aCustomObject() //
		.with("Name", buildUniqueName(TEMPLATE_NAME, "CustomObjectNameInB0"))
													//
													.with("interpreter__c", "IntepreterInB0")
													//
													//
													.build());

		flowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

		MuleEvent event = flowB.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage()
															.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i)
									.put("Id", results.get(i)
														.getId());
		}

		// Create custom objects in source system to be or not to be synced
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createCustomObjectFlowA");
		flow.initialise();

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject() //
		.with("Name", buildUniqueName(TEMPLATE_NAME, "CustomObjectNameInA0"))
													//
													.with("interpreter__c", "IntepreterInA0")
													//
													.with("year__c", "1967")
													//
													.build());

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject() //
		.with("Name", buildUniqueName(TEMPLATE_NAME, "CustomObjectNameInA1"))
													//
													.with("interpreter__c", "IntepreterInA1")
													//
													.with("year__c", "1966")
													//
													.build());

		// This custom object should BE synced (inserted) as the year is greater
		// than 1968 and the record doesn't exist in the target system
		createdCustomObjectsInA.add(aCustomObject() //
		.with("Name", buildUniqueName(TEMPLATE_NAME, "CustomObjectNameInA2"))
													//
													.with("interpreter__c", "IntepreterInA2")
													//
													.with("year__c", "2006")
													//
													.build());

		// This custom object should BE synced (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInA.add(aCustomObject() //
		.with("Name", buildUniqueName(TEMPLATE_NAME, "CustomObjectNameInA3"))
													//
													.with("interpreter__c", "IntepreterInA3")
													//
													.with("year__c", "1975")
													//
													.build());

		event = flow.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		results = (List<SaveResult>) event.getMessage()
											.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i)
									.put("Id", results.get(i)
														.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		failed = null;
		deleteTestDataFromSandBox();
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteCustomObjectFromAFlow");
		flow.initialise();
		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : createdCustomObjectsInA) {
			idList.add(String.valueOf(c.get("Id")));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created custom objects in B
		flow = getSubFlow("deleteCustomObjectFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, Object> c : createdCustomObjectsInA) {
			Map<String, Object> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
			if (customObject != null) {
				idList.add(String.valueOf(customObject.get("Id")));
			}
		}
		for (Map<String, Object> c : createdCustomObjectsInB) {
			Map<String, Object> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
			if (customObject != null) {
				idList.add(String.valueOf(customObject.get("Id")));
			}
		}

		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow() throws Exception {

		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		helper.awaitJobTermination(TIMEOUT_SECONDS * 1000, 500);
		helper.assertJobWasSuccessful();

		assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(0)));

		assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(1)));

		Map<String, Object> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(2));
		assertEquals("The custom object should have been sync", createdCustomObjectsInA.get(2)
																						.get("Name"), payload.get("Name"));

		payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(3));
		assertEquals("The custom object should have been sync (Name)", createdCustomObjectsInA.get(3)
																								.get("Name"), payload.get("Name"));
		assertEquals("The custom object should have been sync (interpreter__c)", createdCustomObjectsInA.get(3)
																										.get("interpreter__c"), payload.get("interpreter__c"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> invokeRetrieveCustomObjectFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> customObject) throws Exception {
		Map<String, Object> customObjectMap = aCustomObject() //
		.with("Name", customObject.get("Name"))
																//
																.build();

		MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage()
								.getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, Object>) payload;
		}
	}

}
