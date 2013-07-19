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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sforce.soap.partner.SaveResult;



public class QueryAllTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("queryAllTestData");
			
			flow = lookupMessageProcessor("create-from-message");
	        response = flow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResultsList =  (List<SaveResult>) response.getMessage().getPayload();
	        Iterator<SaveResult> saveResultsIter = saveResultsList.iterator();  

	        List<Map<String,Object>> sObjects = (List<Map<String,Object>>) testObjects.get("sObjectFieldMappingsFromMessage");
			Iterator<Map<String,Object>> sObjectsIterator = sObjects.iterator();
	        
			while (saveResultsIter.hasNext()) {
				
				SaveResult saveResult = saveResultsIter.next();
				Map<String,Object> sObject = (Map<String, Object>) sObjectsIterator.next();
				sObjectsIds.add(saveResult.getId());
		        sObject.put("Id", saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
			
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
	public void testQueryAll() {
		
		List<String> queriedRecordIds = (List<String>) testObjects.get("idsToDeleteFromMessage");
		List<String> returnedSObjectsIds = new ArrayList<String>();
		
		try {
			
			flow = lookupMessageProcessor("query-all");
			response = flow.process(getTestEvent(testObjects));
			
			List<Map<String, Object>> records =  (List<Map<String, Object>>) response.getMessage().getPayload();
	        
	        Iterator<Map<String, Object>> iter = records.iterator();  

			while (iter.hasNext()) {
				
				Map<String, Object> sObject = iter.next();
				returnedSObjectsIds.add(sObject.get("Id").toString());
				
			}
			
			assertTrue(returnedSObjectsIds.size() > 0);

			for (int index = 0; index < queriedRecordIds.size(); index++) {
				assertTrue(returnedSObjectsIds.contains(queriedRecordIds.get(index).toString())); 
		     }
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}