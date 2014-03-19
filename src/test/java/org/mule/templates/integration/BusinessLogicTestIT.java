package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mule.templates.builders.SfdcObjectBuilder.aCustomObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.api.BatchJobInstance;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceAdapter;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends AbstractTemplateTestCase {

	private static final String KICK_NAME = "sfdc2sfdc-customobjectsync";

	private static SubflowInterceptingChainLifecycleWrapper checkCustomObjectflow;
	private static List<Map<String, Object>> createdCustomObjectsInA = new ArrayList<Map<String, Object>>();

	protected static final int TIMEOUT = 60;

	private Prober prober;
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
		failed = null;
		jobInstanceStore = muleContext.getRegistry().lookupObject(BatchJobInstanceStore.class);
		muleContext.registerListener(new BatchWaitListener());

		// Flow to retrieve custom objects from target system after syncing
		checkCustomObjectflow = getSubFlowAndInitialiseIt("retrieveCustomObjectFlow");

		createTestDataInSandBox();
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		// Create object in target system to be updated
		SubflowInterceptingChainLifecycleWrapper flowB = getSubFlowAndInitialiseIt("createCustomObjectFlowB");

		List<Map<String, Object>> createdCustomObjectsInB = new ArrayList<Map<String, Object>>();
		// This custom object should BE synced (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInB.add(aCustomObject() //
				.with("Name", "Physical Graffiti") //
				.with("interpreter__c", "Lead Zep") //
				.with("genre__c", "Hard Rock") //
				.build());

		flowB.process(getTestEvent(createdCustomObjectsInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create custom objects in source system to be or not to be synced
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlowAndInitialiseIt("createCustomObjectFlowA");

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject() //
				.with("Name", generateUnique("Are You Experienced")) //
				.with("interpreter__c", "Jimi Hendrix") //
				.with("year__c", "1967") //
				.build());

		// This custom object should not be synced as the year is not greater
		// than 1968
		createdCustomObjectsInA.add(aCustomObject() //
				.with("Name", generateUnique("Revolver")) //
				.with("interpreter__c", "The Beatles") //
				.with("year__c", "1966") //
				.build());

		// This custom object should BE synced (inserted) as the year is greater
		// than 1968 and the record doesn't exist in the target system
		createdCustomObjectsInA.add(aCustomObject() //
				.with("Name", generateUnique("Amputechture")) //
				.with("interpreter__c", "The Mars Volta") //
				.with("year__c", "2006") //
				.build());

		// This custom object should BE synced (updated) as the year is greater
		// than 1968 and the record exists in the target system
		createdCustomObjectsInA.add(aCustomObject() //
				.with("Name", generateUnique("Physical Graffiti")) //
				.with("interpreter__c", "Led Zeppelin") //
				.with("year__c", "1975") //
				.build());

		MuleEvent event = flow.process(getTestEvent(createdCustomObjectsInA, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdCustomObjectsInA.get(i).put("Id", results.get(i).getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		failed = null;
		deleteTestDataFromSandBox();
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created custom objects in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlowAndInitialiseIt("deleteCustomObjectFromAFlow");

		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : createdCustomObjectsInA) {
			idList.add(String.valueOf(c.get("Id")));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created custom objects in B
		flow = getSubFlowAndInitialiseIt("deleteCustomObjectFromBFlow");

		idList.clear();
		for (Map<String, Object> c : createdCustomObjectsInA) {
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
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
		BatchJobInstance batchJobInstance = (BatchJobInstance) event.getMessage().getPayload();

		awaitJobTermination();

		assertTrue("Batch job was not successful", wasJobSuccessful());

		batchJobInstance = getUpdatedInstance(batchJobInstance);

		assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(0)));

		assertEquals("The custom object should not have been sync", null, invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(1)));

		Map<String, Object> payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(2));
		assertEquals("The custom object should have been sync", createdCustomObjectsInA.get(2).get("Name"), payload.get("Name"));

		payload = invokeRetrieveCustomObjectFlow(checkCustomObjectflow, createdCustomObjectsInA.get(3));
		assertEquals("The custom object should have been sync (Name)", createdCustomObjectsInA.get(3).get("Name"), payload.get("Name"));
		assertEquals("The custom object should have been sync (interpreter__c)", createdCustomObjectsInA.get(3).get("interpreter__c"), payload.get("interpreter__c"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> invokeRetrieveCustomObjectFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> customObject) throws Exception {
		Map<String, Object> customObjectMap = aCustomObject() //
				.with("Name", customObject.get("Name")) //
				.build();

		MuleEvent event = flow.process(getTestEvent(customObjectMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, Object>) payload;
		}
	}

	private SubflowInterceptingChainLifecycleWrapper getSubFlowAndInitialiseIt(String name) throws InitialisationException {
		SubflowInterceptingChainLifecycleWrapper subFlow = getSubFlow(name);
		subFlow.initialise();
		return subFlow;
	}

	private String generateUnique(String string) {
		return MessageFormat.format("{0}-{1}-{2}", KICK_NAME, String.valueOf(System.currentTimeMillis()).replaceAll(",", ""), string);
	}

	protected void awaitJobTermination() throws Exception {
		this.awaitJobTermination(TIMEOUT);
	}

	protected void awaitJobTermination(long timeoutSecs) throws Exception {
		this.prober = new PollingProber(timeoutSecs * 1000, 500);
		this.prober.check(new Probe() {

			@Override
			public boolean isSatisfied() {
				return failed != null;
			}

			@Override
			public String describeFailure() {
				return "batch job timed out";
			}
		});
	}

	protected boolean wasJobSuccessful() {
		return this.failed != null ? !this.failed : false;
	}

	protected BatchJobInstanceAdapter getUpdatedInstance(BatchJobInstance jobInstance) {
		return this.jobInstanceStore.getJobInstance(jobInstance.getOwnerJobName(), jobInstance.getId());
	}

}
