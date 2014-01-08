package org.mule.kicks.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
	private static List<Map<String, String>> createdCustomObjects = new ArrayList<Map<String, String>>();

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		checkCustomObjectflow = getSubFlow("retrieveCustomObjectFlow");
		checkCustomObjectflow.initialise();

		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createCustomObjectFlow");
		flow.initialise();

		// This custom object should not be sync
		Map<String, String> customObject = createCustomObject("A", 0);
		customObject.put("Email", "");
		createdCustomObjects.add(customObject);

		// This custom object should not be sync
		customObject = createCustomObject("A", 1);
		customObject.put("MailingCountry", "ARG");
		createdCustomObjects.add(customObject);

		// This custom object should BE sync
		customObject = createCustomObject("A", 2);
		createdCustomObjects.add(customObject);

		MuleEvent event = flow.process(getTestEvent(createdCustomObjects, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjects.get(i).put("Id", results.get(i).getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteCustomObjectFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, String> c : createdCustomObjects) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created custom objects in B
		flow = getSubFlow("deleteCustomObjectFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, String> c : createdCustomObjects) {
			Map<String, String> customObject = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, c);
			if (customObject != null) {
				idList.add(customObject.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		Assert.assertEquals("The custom object should not have been sync", null,
				invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjects.get(0)));

		Assert.assertEquals("The custom object should not have been sync", null,
				invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjects.get(1)));

		Map<String, String> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjects.get(2));
		Assert.assertEquals("The custom object should have been sync", createdCustomObjects.get(2).get("Email"),
				payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveCustomObjectFlow(SubflowInterceptingChainLifecycleWrapper flow,
			Map<String, String> customObject) throws Exception {
		Map<String, String> customObjectMap = new HashMap<String, String>();

		customObjectMap.put("Email", customObject.get("Email"));
		customObjectMap.put("FirstName", customObject.get("FirstName"));
		customObjectMap.put("LastName", customObject.get("LastName"));

		MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	private Map<String, String> createCustomObject(String orgId, int sequence) {
		Map<String, String> customObject = new HashMap<String, String>();

		customObject.put("FirstName", "FirstName_" + sequence);
		customObject.put("LastName", "LastName_" + sequence);
		customObject.put("Email", "some.email." + sequence + "@fakemail.com");
		customObject.put("Description", "Some fake description");
		customObject.put("MailingCity", "Denver");
		customObject.put("MailingCountry", "USA");
		customObject.put("MobilePhone", "123456789");
		customObject.put("Department", "department_" + sequence + "_" + orgId);
		customObject.put("Phone", "123456789");
		customObject.put("Title", "Dr");

		return customObject;
	}

}
