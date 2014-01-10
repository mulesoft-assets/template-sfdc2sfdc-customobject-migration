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
		createdCustomObjects.add(aCustomObject()
                                    .withProperty("Name", "Name_" + 0)
                                    .withProperty("interpreter__c", "interpreter__c_" + 0)
                                    .withProperty("year__c", String.valueOf(1950 + 0))
                                    .build());

		// This custom object should not be sync
		createdCustomObjects.add(aCustomObject()
                                     .withProperty("Name", "Name_" + 1)
                                     .withProperty("interpreter__c", "interpreter__c_" + 1)
                                     .withProperty("year__c", String.valueOf(1950 + 1))
                                     .build());

		// This custom object should BE sync
		createdCustomObjects.add(aCustomObject()
                                     .withProperty("Name", "Name_" + 2)
                                     .withProperty("interpreter__c", "interpreter__c_" + 2)
                                     .withProperty("year__c", "2014")
                                     .build());

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

		Assert.assertEquals("The custom object should have been sync", 
		        createdCustomObjects.get(2).get("Name"),
				payload.get("Name"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveCustomObjectFlow(SubflowInterceptingChainLifecycleWrapper flow,
			Map<String, String> customObject) throws Exception {
		Map<String, String> customObjectMap = new HashMap<String, String>();

		customObjectMap.put("Name", customObject.get("Name"));

		MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}
	
	private CustomObjectBuilder aCustomObject() {
	    return new CustomObjectBuilder(); 
	}
	
	private static class CustomObjectBuilder {
	    
	    private Map<String, String> customObject = new HashMap<String, String>();
	    
	    public CustomObjectBuilder withProperty(String key, String value) {
	        customObject.put(key, value);
	        return this;
	    }
	    
	    public Map<String, String> build() {
            return customObject;
        }
	    
	}

}
