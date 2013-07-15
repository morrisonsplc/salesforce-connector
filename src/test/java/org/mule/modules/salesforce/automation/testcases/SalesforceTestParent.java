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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sforce.async.BatchInfo;
import com.sforce.async.BatchResult;
import com.sforce.async.Result;



public class SalesforceTestParent extends FunctionalTestCase {
	
	protected static final String[] SPRING_CONFIG_FILES = new String[] {"AutomationSpringBeans.xml"};
	protected static ApplicationContext context;
	protected MessageProcessor flow;
	protected MuleEvent response;
	protected Map<String,Object> testObjects;
	public static final long UPDATE_DELAY = 60000;
	public static final long BATCH_PROCESSING_DELAY = 10000;
	
	
	@Override
	protected String getConfigResources() {
		return "automation-test-flows.xml";
	}
	
    protected MessageProcessor lookupFlowConstruct(String name) {
        return (MessageProcessor) muleContext.getRegistry().lookupFlowConstruct(name);
    }
	
    @BeforeClass
    public static void beforeClass(){
    	
    	context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILES);
    	
    }
    
    protected BatchInfo getBatchInfoByOperation(MessageProcessor flow) throws MuleException, Exception {
		
		MuleEvent response = (MuleEvent) flow.process(getTestEvent(testObjects));
		return (BatchInfo) response.getMessage().getPayload();
		
	}
	
	protected BatchResult getBatchResult(MessageProcessor batchResultFlow) throws MuleException, Exception {
		
		MuleEvent response = (MuleEvent) batchResultFlow.process(getTestEvent(testObjects));
		return (BatchResult) response.getMessage().getPayload();
		
	}
	
	protected void assertBatchSucessAndGetSObjectIds(BatchResult batchResult) {
		
		List<String> createdSObjectsIds = new ArrayList<String>();
		
		boolean isSuccess = true;
		Result[] results = batchResult.getResult();
		
		for (int index=0; index<results.length; index++) {
			
			if (results[index].isSuccess()) {
				createdSObjectsIds.add(results[index].getId());
			} else {
				isSuccess = false;	
			}
	
		}
		
		testObjects.put("idsToDeleteFromMessage", createdSObjectsIds);
		
		assertTrue(isSuccess);

	}
	
	protected void assertBatchSucessAndCompareSObjectIds(BatchResult batchResult, List<String> createdSObjectsIds) {
		
		boolean isSuccess = true;
		Result[] results = batchResult.getResult();
		
		for (int index=0; index<results.length; index++) {
			
			if (!(results[index].isSuccess() && createdSObjectsIds.contains(results[index].getId()))) {
				isSuccess = false;
			}
	
		}
		
		testObjects.put("idsToDeleteFromMessage", createdSObjectsIds);
		
		assertTrue(isSuccess);

	}
    
	protected void assertBatchSucessAndUpdatedSObjectId(BatchResult batchResult) {
		
		List<String> sObjectsIds = new ArrayList<String>();
		
		boolean isSuccess = true;
		Result[] results = batchResult.getResult();
		
		for (int index=0; index<results.length; index++) {
			
			if (results[index].isSuccess()) {
				sObjectsIds.add(results[index].getId());
			} else {
				isSuccess = false;	
			}
	
		}
		
		testObjects.put("idsToDeleteFromMessage", sObjectsIds);
		
		assertTrue(sObjectsIds.contains(((HashMap<String,Object>) context.getBean("upsertBulkSObjectToBeUpdated")).get("Id")));
		assertTrue(isSuccess);

	}
	

}