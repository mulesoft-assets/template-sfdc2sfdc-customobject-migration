package org.mule.kicks.transformers;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.NullPayload;


@RunWith(MockitoJUnitRunner.class)
public class SFDCContactFilterTest {

	private static final String CONTACT_IN_COMPANY_B = "contactInB";
	
	@Mock
	private MuleContext muleContext;

	@Test
	public void testContactNotInOrgB() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getContactFromSFDCA());
		message.setInvocationProperty(CONTACT_IN_COMPANY_B, NullPayload.getInstance());
		
		SFDCContactFilter transformer = new SFDCContactFilter();
		
		MuleMessage resultingContact = (MuleMessage) transformer.transform(message);
		
		Map<String,String> expectedContact = getContactFromSFDCA();
		expectedContact.remove("LastModifiedDate");
		
		Assert.assertEquals(expectedContact ,resultingContact.getPayload());

	}
	
	@Test
	public void testSyncContact() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getContactFromSFDCA());
		Map<String,String> contactInB = new HashMap<String,String>();
		contactInB.put("Id", "I000032300ESE");
		contactInB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");
		message.setInvocationProperty(CONTACT_IN_COMPANY_B, contactInB);
		
		SFDCContactFilter transformer = new SFDCContactFilter();
		
		MuleMessage resultingContact = (MuleMessage) transformer.transform(message);
		
		Map<String,String> expectedContact = getContactFromSFDCA();
		expectedContact.remove("LastModifiedDate");
		expectedContact.put("Id", "I000032300ESE");

		Assert.assertEquals(expectedContact, resultingContact.getPayload());

	}
	
	@Test
	public void testDoNotSyncContact() throws TransformerException {
		
		MuleMessage message = new DefaultMuleMessage(null, muleContext);
		message.setPayload(getContactFromSFDCA());
		Map<String,String> contactInB = new HashMap<String,String>();
		contactInB.put("Id", "I000032300ESE");
		contactInB.put("LastModifiedDate", "2013-12-11T22:15:33.001Z");
		message.setInvocationProperty(CONTACT_IN_COMPANY_B, contactInB);
		
		SFDCContactFilter transformer = new SFDCContactFilter();
		
		MuleMessage resultingContact = (MuleMessage) transformer.transform(message);
		
		Assert.assertEquals(NullPayload.getInstance(), resultingContact.getPayload());

	}
	
	private Map<String,String> getContactFromSFDCA () {
		
		Map<String,String> contactFromSFDCA = new HashMap<String,String>();
		contactFromSFDCA.put("Id", "I0000323AE754F");
		contactFromSFDCA.put("Department", "Engineering");
		contactFromSFDCA.put("Email", "carlos@mendez.com.ar");
		contactFromSFDCA.put("FirstName", "Carlos Saul");
		contactFromSFDCA.put("MailingCity", "Buenos Aires");
		contactFromSFDCA.put("MailingCountry", "Argentina");
		contactFromSFDCA.put("MobilePhone", "1555896532");
		contactFromSFDCA.put("Phone", "1555845632");
		contactFromSFDCA.put("Title", "Ex President");
		contactFromSFDCA.put("LastModifiedDate", "2013-12-10T22:15:33.001Z");
		
		return contactFromSFDCA;
	}
	
}
