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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.sforce.async.JobInfo;



public class CreateJobTestCases extends SalesforceTestParent {
	
	private MessageProcessor createJob;
	private MessageProcessor closeJob;
	
	@Before
	public void setUp() {
		
    	createJob = lookupFlowConstruct("create-job");
    	closeJob = lookupFlowConstruct("close-job");
		
		testObjects = (HashMap<String,Object>) context.getBean("createJobTestData");
		
	}
	
	@After
	public void tearDown() {

		try {

			if (testObjects.containsKey("jobId")) {
				closeJob.process(getTestEvent(testObjects));
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
    @Category({RegressionTests.class})
	@Test
	public void testCreateJob() {
    	
		try {
			
			MuleEvent response = (MuleEvent) createJob.process(getTestEvent(testObjects));
			JobInfo jobInfo = (JobInfo) response.getMessage().getPayload();
			
			assertEquals(com.sforce.async.JobStateEnum.Open, jobInfo.getState());
			assertEquals(testObjects.get("concurrencyMode").toString(), jobInfo.getConcurrencyMode().toString());
			assertEquals(testObjects.get("operation").toString(), jobInfo.getOperation().toString());
			assertEquals(testObjects.get("contentType").toString(), jobInfo.getContentType().toString());

			testObjects.put("jobId", jobInfo.getId());
	        
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
     
	}

}