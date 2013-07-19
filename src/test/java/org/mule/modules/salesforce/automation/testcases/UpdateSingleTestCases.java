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

import com.sforce.soap.partner.SaveResult;



public class UpdateSingleTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("updateSingleTestData");
			
			flow = lookupMessageProcessor("create-single-from-message");
	        response = flow.process(getTestEvent(testObjects)); 
			
	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
	        Map<String,Object> sObject = (HashMap<String,Object>) testObjects.get("salesforceObjectFromMessage");
	        
	        sObjectsIds.add(saveResult.getId());
	        sObject.put("Id", saveResult.getId());

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
	
	@Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testUpdateSingleChildElementsFromMessage() {
			
		try {
			
			flow = lookupMessageProcessor("update-single-from-message");
			response = flow.process(getTestEvent(testObjects));
			
			SaveResult saveResult =  (SaveResult) response.getMessage().getPayload();

			assertTrue(saveResult.getSuccess());
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}