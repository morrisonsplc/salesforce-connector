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
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sforce.soap.partner.LeadConvertResult;
import com.sforce.soap.partner.SaveResult;



public class ConvertLeadTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	SaveResult saveResult;
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("convertLeadTestData");
			
			Map<String,Object> lead = (HashMap<String,Object>) testObjects.get("lead");
			Map<String,Object> account = (HashMap<String,Object>) testObjects.get("account");
			
			flow = lookupMessageProcessor("create-single-from-message");
	        
			response = flow.process(getTestEvent(lead)); 
	        saveResult = (SaveResult) response.getMessage().getPayload();
	        sObjectsIds.add(saveResult.getId());
	        testObjects.put("leadId", saveResult.getId());
	        
	        response = flow.process(getTestEvent(account)); 
	        saveResult = (SaveResult) response.getMessage().getPayload();
	        sObjectsIds.add(saveResult.getId());
	        testObjects.put("contactId", saveResult.getId());

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@After
	public void tearDown() {
    	
		try {
			
			flow = lookupMessageProcessor("delete-from-message");
			flow.process(getTestEvent(testObjects));
  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@Category({RegressionTests.class})
	@Test
	public void testConvertLeadDefaultValues() {
			
		try {
			
			flow = lookupMessageProcessor("convert-lead-default-values");
			response = flow.process(getTestEvent(testObjects));
			
			LeadConvertResult leadConvertResult =  (LeadConvertResult) response.getMessage().getPayload();

			assertTrue(leadConvertResult.isSuccess());
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}