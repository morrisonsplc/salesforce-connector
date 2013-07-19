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

import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;



public class UpsertTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("upsertTestData");
			
			flow = lookupMessageProcessor("create-single-from-message");
	        response = flow.process(getTestEvent(testObjects));

	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
	        
	        Map<String,Object> sObjectToBeUpdated = (Map<String,Object>) testObjects.get("sObjectFieldMappingsFromMessage");
	        sObjectToBeUpdated.put("Id", saveResult.getId());
	        testObjects.put("sObjectToBeUpdatedId", saveResult.getId());
	        
	        List<Map<String,Object>> sObjectsList = (List<Map<String,Object>>) testObjects.get("salesforceSObjectsListFromMessage");
	        sObjectsList.add(sObjectToBeUpdated);
  
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
	public void testUpsertChildElementsFromMessage() {
		
    	List<String> sObjectsIds = new ArrayList<String>();
    	String sObjectToBeUpdatedId = (String) testObjects.get("sObjectToBeUpdatedId");
			
		try {
			
			flow = lookupMessageProcessor("upsert-from-message");
			response = flow.process(getTestEvent(testObjects));
			
			List<UpsertResult> upsertResults =  (List<UpsertResult>) response.getMessage().getPayload();
	        
	        Iterator<UpsertResult> upsertResultsIter = upsertResults.iterator();  
	        
			while (upsertResultsIter.hasNext()) {
				
				UpsertResult upsertResult = upsertResultsIter.next();
				sObjectsIds.add(upsertResult.getId());
				
				assertTrue(upsertResult.getSuccess());
				
				if (!sObjectToBeUpdatedId.equals(upsertResult.getId())) {
					assertTrue(upsertResult.isCreated());
				}
					 
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);	        
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}