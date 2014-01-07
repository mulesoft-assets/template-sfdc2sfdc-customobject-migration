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

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, String>> createdContacts = new ArrayList<Map<String, String>>();

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlow");
		flow.initialise();

		// This contact should not be sync
		Map<String, String> contact = createContact("A", 0);
		contact.put("Email", "");
		createdContacts.add(contact);

		// This contact should not be sync
		contact = createContact("A", 1);
		contact.put("MailingCountry", "ARG");
		createdContacts.add(contact);

		// This contact should BE sync
		contact = createContact("A", 2);
		createdContacts.add(contact);

		MuleEvent event = flow.process(getTestEvent(createdContacts, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdContacts.get(i).put("Id", results.get(i).getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		// Delete the created contacts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteContactFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, String> c : createdContacts) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created contacts in B
		flow = getSubFlow("deleteContactFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, String> c : createdContacts) {
			Map<String, String> contact = invokeRetrieveContactFlow(checkContactflow, c);
			if (contact != null) {
				idList.add(contact.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		Assert.assertEquals("The contact should not have been sync", null,
				invokeRetrieveContactFlow(checkContactflow, createdContacts.get(0)));

		Assert.assertEquals("The contact should not have been sync", null,
				invokeRetrieveContactFlow(checkContactflow, createdContacts.get(1)));

		Map<String, String> payload = invokeRetrieveContactFlow(checkContactflow, createdContacts.get(2));
		Assert.assertEquals("The contact should have been sync", createdContacts.get(2).get("Email"),
				payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveContactFlow(SubflowInterceptingChainLifecycleWrapper flow,
			Map<String, String> contact) throws Exception {
		Map<String, String> contactMap = new HashMap<String, String>();

		contactMap.put("Email", contact.get("Email"));
		contactMap.put("FirstName", contact.get("FirstName"));
		contactMap.put("LastName", contact.get("LastName"));

		MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	private Map<String, String> createContact(String orgId, int sequence) {
		Map<String, String> contact = new HashMap<String, String>();

		contact.put("FirstName", "FirstName_" + sequence);
		contact.put("LastName", "LastName_" + sequence);
		contact.put("Email", "some.email." + sequence + "@fakemail.com");
		contact.put("Description", "Some fake description");
		contact.put("MailingCity", "Denver");
		contact.put("MailingCountry", "USA");
		contact.put("MobilePhone", "123456789");
		contact.put("Department", "department_" + sequence + "_" + orgId);
		contact.put("Phone", "123456789");
		contact.put("Title", "Dr");

		return contact;
	}

}
