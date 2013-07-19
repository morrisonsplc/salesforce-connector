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

import com.sforce.soap.partner.GetUpdatedResult;
import com.sforce.soap.partner.SaveResult;



public class GetUpdatedTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("getUpdatedTestData");
			
			flow = lookupMessageProcessor("create-from-message");
	        response = flow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResultsList =  (List<SaveResult>) response.getMessage().getPayload();
	        Iterator<SaveResult> saveResultsIter = saveResultsList.iterator();  

	        List<Map<String,Object>> sObjects = (List<Map<String,Object>>) testObjects.get("salesforceSObjectsListFromMessage");
			Iterator<Map<String,Object>> sObjectsIterator = sObjects.iterator();
	        
			while (saveResultsIter.hasNext()) {
				
				SaveResult saveResult = saveResultsIter.next();
				Map<String,Object> sObject = (Map<String, Object>) sObjectsIterator.next();
				sObjectsIds.add(saveResult.getId());
		        sObject.put("Id", saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
			
			flow = lookupMessageProcessor("update-from-message");
			flow.process(getTestEvent(testObjects));
			
			// because of the rounding applied to the seconds 
			Thread.sleep(60000);
  
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
	public void testGetUpdated() {
		
		List<String> createdRecordsIds = (List<String>) testObjects.get("idsToDeleteFromMessage");
		
		try {
			
			flow = lookupMessageProcessor("get-updated");
			response = flow.process(getTestEvent(testObjects));
			
			GetUpdatedResult updatedResult =  (GetUpdatedResult) response.getMessage().getPayload();

			String[] ids = updatedResult.getIds();
			
			assertTrue(ids != null && ids.length > 0);

			for (int i = 0; i < ids.length; i++) {
				assertTrue(createdRecordsIds.contains(ids[i].toString())); 
		     }
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}