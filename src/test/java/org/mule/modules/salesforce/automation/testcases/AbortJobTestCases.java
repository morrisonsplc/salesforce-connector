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



public class AbortJobTestCases extends SalesforceTestParent {
	
	private MessageProcessor createJob;
	private MessageProcessor abortJob;
	
	@Before
	public void setUp() {
		
    	createJob = lookupFlowConstruct("create-job");
    	abortJob = lookupFlowConstruct("abort-job");
		
		testObjects = (HashMap<String,Object>) context.getBean("abortJobTestData");
		
		try {

			MuleEvent response = (MuleEvent) createJob.process(getTestEvent(testObjects));
			JobInfo jobInfo = (JobInfo) response.getMessage().getPayload();

			testObjects.put("jobId", jobInfo.getId());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}
	
    @Category({RegressionTests.class})
	@Test
	public void testAbortJob() {

		try {

			MuleEvent response = (MuleEvent) abortJob.process(getTestEvent(testObjects));
			JobInfo jobInfo = (JobInfo) response.getMessage().getPayload();

			assertEquals(com.sforce.async.JobStateEnum.Aborted, jobInfo.getState());
			assertEquals(testObjects.get("jobId").toString(), jobInfo.getId());
			assertEquals(testObjects.get("concurrencyMode").toString(), jobInfo.getConcurrencyMode().toString());
			assertEquals(testObjects.get("operation").toString(), jobInfo.getOperation().toString());
			assertEquals(testObjects.get("contentType").toString(), jobInfo.getContentType().toString());

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
	}

}