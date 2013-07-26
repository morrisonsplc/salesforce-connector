/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce.automation.testcases;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.sforce.soap.partner.SaveResult;

public class GetUpdatedObjectsTestCases extends SalesforceTestParent {
	
	private MessageProcessor createSingleFromMessageFlow;
	private MessageProcessor resetUpdatedObjectsTimestampFlow;
	private MessageProcessor getUpdatedObjectsFlow;
	private MessageProcessor updateSingleFlow;
	private MessageProcessor deleteFlow;
		
	@Before
	public void setUp() {
		
		createSingleFromMessageFlow = lookupFlowConstruct("create-single-from-message");
		resetUpdatedObjectsTimestampFlow = lookupFlowConstruct("reset-updated-objects-timestamp");
		getUpdatedObjectsFlow = lookupFlowConstruct("get-updated-objects");
		updateSingleFlow = lookupFlowConstruct("update-single-from-message");
		deleteFlow = lookupFlowConstruct("delete-from-message");
    	
		List<String> sObjectsIds = new ArrayList<String>();
		
		testObjects = (HashMap<String,Object>) context.getBean("getUpdatedObjectsTestData");
		Map<String,Object> sObject = (HashMap<String,Object>) testObjects.get("salesforceObjectFromMessage");

		try {
	
	        response = createSingleFromMessageFlow.process(getTestEvent(testObjects));
	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
	        sObjectsIds.add(saveResult.getId());
	        sObject.put("Id", saveResult.getId());
			testObjects.put("idsToDeleteFromMessage", sObjectsIds);

			getUpdatedObjectsFlow.process(getTestEvent(testObjects));
			
			updateSingleFlow.process(getTestEvent(testObjects));
			Thread.sleep(UPDATE_DELAY);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@After
	public void tearDown() {
    	
		try {
			
			deleteFlow.process(getTestEvent(testObjects));
  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testGetUpdatedObjects() {
		
		List<String> updatedRecordId = (List<String>) testObjects.get("idsToDeleteFromMessage");
		List<String> returnedSObjectsIds;

		try {

	        returnedSObjectsIds = getReturnedSObjectsIds(getUpdatedObjectsFlow.process(getTestEvent(testObjects)));
			
			assertTrue(returnedSObjectsIds.size() > 0);
			assertTrue(returnedSObjectsIds.containsAll(updatedRecordId)); 
			
			resetUpdatedObjectsTimestampFlow.process(getTestEvent(testObjects));
			
			Thread.sleep(UPDATE_DELAY);
			
	        returnedSObjectsIds = getReturnedSObjectsIds(getUpdatedObjectsFlow.process(getTestEvent(testObjects)));
			
			assertTrue(!returnedSObjectsIds.containsAll(updatedRecordId)); 
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

	private List<String> getReturnedSObjectsIds(MuleEvent response) {

		List<String> sObjectsIds = new ArrayList<String>();
		
		List<Map<String, Object>> records;
		Iterator<Map<String, Object>> iter;

		records =  (List<Map<String, Object>>) response.getMessage().getPayload();
        iter = records.iterator();  

		while (iter.hasNext()) {
			Map<String, Object> sObject = iter.next();
			sObjectsIds.add(sObject.get("Id").toString());	
		}
		
		return sObjectsIds;
		
	}

}