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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

import com.sforce.async.BatchInfo;
import com.sforce.async.BatchResult;
import com.sforce.async.Result;
import com.sforce.soap.partner.SaveResult;



public class CreateBulkTestCases extends SalesforceTestParent {

	private MessageProcessor createBulkFlow;
	private MessageProcessor batchInfoFlow;
	private MessageProcessor batchResultFlow;
	private MessageProcessor deleteFlow;
	
    @Before
    public void setUp(){
    	
    	createBulkFlow = lookupFlowConstruct("create-bulk");
    	batchInfoFlow = lookupFlowConstruct("batch-info");
    	batchResultFlow = lookupFlowConstruct("batch-result");
		deleteFlow = lookupFlowConstruct("delete-from-message");

		testObjects = (HashMap<String,Object>) context.getBean("createBulkTestData");
		
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
	
    @Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testCreateBulk() {
    	
    	BatchInfo batchInfo;
    	
		try {
  
			batchInfo = getBatchInfoByOperation(createBulkFlow);
			
			do {
				
				Thread.sleep(BATCH_PROCESSING_DELAY);
				testObjects.put("batchInfoRef", batchInfo);
				batchInfo = getBatchInfoByOperation(batchInfoFlow);

			} while (batchInfo.getState().equals(com.sforce.async.BatchStateEnum.InProgress) || batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Queued));
	
			assertTrue(batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Completed));
			
			assertBatchSucessAndGetSObjectIds(getBatchResult(batchResultFlow)); 
	        
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
     
	}

}