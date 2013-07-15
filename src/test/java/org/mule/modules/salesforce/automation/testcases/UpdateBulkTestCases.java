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
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

import com.sforce.async.BatchInfo;
import com.sforce.soap.partner.SaveResult;



public class UpdateBulkTestCases extends SalesforceTestParent {

	
	private MessageProcessor createFlow;
	private MessageProcessor updateBulkFlow;
	private MessageProcessor batchInfoFlow;
	private MessageProcessor batchResultFlow;
	private MessageProcessor deleteFlow;

	private List<String> createdSObjectsIds = new ArrayList<String>();
	
	
	@Before
	public void setUp() {
		
		createFlow = lookupFlowConstruct("create-from-message");
    	updateBulkFlow = lookupFlowConstruct("update-bulk");
    	batchInfoFlow = lookupFlowConstruct("batch-info");
    	batchResultFlow = lookupFlowConstruct("batch-result");
		deleteFlow = lookupFlowConstruct("delete-from-message");
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("updateBulkTestData");

			createAndSetIdsToSObjects();
  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	private void createAndSetIdsToSObjects() throws Exception {
		 	
			MuleEvent response = createFlow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResultsList =  (List<SaveResult>) response.getMessage().getPayload();
	        Iterator<SaveResult> saveResultsIter = saveResultsList.iterator();  

	        List<Map<String,Object>> sObjects = (List<Map<String,Object>>) testObjects.get("objectsRef");
			Iterator<Map<String,Object>> sObjectsIterator = sObjects.iterator();
	        
			while (saveResultsIter.hasNext()) {
				
				SaveResult saveResult = saveResultsIter.next();
				Map<String,Object> sObject = (Map<String, Object>) sObjectsIterator.next();
				createdSObjectsIds.add(saveResult.getId());
		        sObject.put("Id", saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", createdSObjectsIds);
		
	}

	@After
	public void tearDown() {
		
		try {
			
			if (testObjects.containsKey("idsToDeleteFromMessage")) {		
				deleteFlow.process(getTestEvent(testObjects));	
			}

		} catch (Exception e) {
				e.printStackTrace();
				fail();
		}
		
	}
	
	@Category({RegressionTests.class})
	@Test
	public void testUpdateBulk() {
		
    	BatchInfo batchInfo;
		
		try {
			
			batchInfo = getBatchInfoByOperation(updateBulkFlow);
			
			do {
				
				Thread.sleep(BATCH_PROCESSING_DELAY);
				testObjects.put("batchInfoRef", batchInfo);
				batchInfo = getBatchInfoByOperation(batchInfoFlow);

			} while (batchInfo.getState().equals(com.sforce.async.BatchStateEnum.InProgress) || batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Queued));
	
			assertTrue(batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Completed));
			
			assertBatchSucessAndCompareSObjectIds(getBatchResult(batchResultFlow), createdSObjectsIds); 
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}