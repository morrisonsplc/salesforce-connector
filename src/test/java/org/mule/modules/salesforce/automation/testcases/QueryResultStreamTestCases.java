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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.IOUtils;

import com.sforce.async.BatchInfo;
import com.sforce.async.JobInfo;



public class QueryResultStreamTestCases extends SalesforceTestParent {
	
	private MessageProcessor createJobFlow;
	private MessageProcessor createBatchFlow;
	private MessageProcessor batchInfoFlow;
	private MessageProcessor batchQueryStreamFlow;
	private MessageProcessor closeJobFlow;
	private MessageProcessor deleteFlow;

	@Before
	public void setUp() {
		
    	createJobFlow = lookupFlowConstruct("create-job");
    	createBatchFlow = lookupFlowConstruct("create-batch");
    	batchInfoFlow = lookupFlowConstruct("batch-info");
    	batchQueryStreamFlow = lookupFlowConstruct("query-result-stream");
    	closeJobFlow = lookupFlowConstruct("close-job");
		deleteFlow = lookupFlowConstruct("delete-from-message");
		
		testObjects = (HashMap<String,Object>) context.getBean("createBatchTestData");
		
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
	public void testQueryResultStream() {
    	
    	BatchInfo batchInfo;
    	
		String[] results;
		String id;
		String success;
		boolean isSuccess = true;

		List<String> sObjectsIds = new ArrayList<String>();
		
		try {
  
			batchInfo = getBatchInfoByOperation(createBatchFlow);
			
			do {
				
				Thread.sleep(BATCH_PROCESSING_DELAY);
				testObjects.put("batchInfoRef", batchInfo);
				batchInfo = getBatchInfoByOperation(batchInfoFlow);

			} while (batchInfo.getState().equals(com.sforce.async.BatchStateEnum.InProgress) || batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Queued));
	
			assertTrue(batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Completed));
			
			MuleEvent response = (MuleEvent) batchQueryStreamFlow.process(getTestEvent(testObjects));
			String operationResponse = IOUtils.toString((InputStream) response.getMessage().getPayload());
			
			results = StringUtils.substringsBetween(operationResponse,"<result>","</result>");
			for (int index=0; index<results.length; index++) {
				
				id = StringUtils.substringBetween(results[index],"<id>","</id>");
				success = StringUtils.substringBetween(results[index],"<success>","</success>");
				
				if (success.equals("true")) {
					sObjectsIds.add(id);
				} else {
					isSuccess = false;	
				}
				
			}
			
			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
			
			assertTrue(isSuccess);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
    	     
	}

}