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

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.sforce.async.BatchInfo;
import com.sforce.async.JobInfo;



public class CreateBatchForQueryTestCases extends SalesforceTestParent {
	
	private MessageProcessor createJobFlow;
	private MessageProcessor createBatchForQueryFlow;
	private MessageProcessor batchInfoFlow;
	private MessageProcessor batchResultFlow;
	private MessageProcessor closeJobFlow;
	private MessageProcessor deleteFlow;

	@Before
	public void setUp() {
		
    	createJobFlow = lookupFlowConstruct("create-job");
    	createBatchForQueryFlow = lookupFlowConstruct("create-batch-for-query");
    	batchInfoFlow = lookupFlowConstruct("batch-info");
    	batchResultFlow = lookupFlowConstruct("batch-result");
    	closeJobFlow = lookupFlowConstruct("close-job");
		deleteFlow = lookupFlowConstruct("delete-from-message");
		
		testObjects = (HashMap<String,Object>) context.getBean("createBatchForQueryTestData");
		
		try {

			MuleEvent response = (MuleEvent) createJobFlow.process(getTestEvent(testObjects));
			JobInfo jobInfo = (JobInfo) response.getMessage().getPayload();
			
			testObjects.put("jobId", jobInfo.getId());
			testObjects.put("jobInfoRef", jobInfo);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	@After
	public void tearDown() {

		try {

			if (testObjects.containsKey("idsToDeleteFromMessage")) {		
				deleteFlow.process(getTestEvent(testObjects));	
			}
			
			closeJobFlow.process(getTestEvent(testObjects));

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
    @Category({RegressionTests.class})
	@Test
	public void testCreateBatchForQuery() {
    	
    	BatchInfo batchInfo;
    	
		try {
  
			batchInfo = getBatchInfoByOperation(createBatchForQueryFlow);
			
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