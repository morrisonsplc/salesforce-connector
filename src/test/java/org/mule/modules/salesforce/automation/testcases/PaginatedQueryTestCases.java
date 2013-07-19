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
import org.mule.modules.salesforce.QueryResultObject;

import com.sforce.soap.partner.SaveResult;



public class PaginatedQueryTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("paginatedQueryTestData");
			
			flow = lookupMessageProcessor("create-single-from-message");
	        response = flow.process(getTestEvent(testObjects));

	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
	        
	        sObjectsIds.add(saveResult.getId());
	        
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
	public void testPaginatedQuery() {
		
		List<String> queriedRecordIds = (List<String>) testObjects.get("idsToDeleteFromMessage");
		String queriedRecordId = queriedRecordIds.get(0).toString();
		
		List<String> returnedSObjectsIds;
		QueryResultObject queryResult;
		List<Map<String, Object>> records;
		
		try {
			
			flow = lookupMessageProcessor("paginated-query");
			response = flow.process(getTestEvent(testObjects));
			
			do {
				
				returnedSObjectsIds = new ArrayList<String>();
				
				queryResult =  (QueryResultObject) response.getMessage().getPayload();
				
				records =  (List<Map<String, Object>>) queryResult.getData();
		        
		        Iterator<Map<String, Object>> iter = records.iterator();  

				while (iter.hasNext()) {
					
					Map<String, Object> sObject = iter.next();
					returnedSObjectsIds.add(sObject.get("Id").toString());
					
				}
				
				assertTrue(returnedSObjectsIds.size() > 0);
				
				testObjects.put("queryResultObjectRef", queryResult);
				
				flow = lookupMessageProcessor("paginated-query-by-queryResultObject-ref");
				response = flow.process(getTestEvent(testObjects));
				
			} while (!returnedSObjectsIds.contains(queriedRecordId) && queryResult.hasMore());
			
			assertTrue(returnedSObjectsIds.contains(queriedRecordId));
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}
	
	@Category({RegressionTests.class})
	@Test
	public void testPaginatedQueryWithDeletedRecords() {
		
		List<String> queriedRecordIds = (List<String>) testObjects.get("idsToDeleteFromMessage");
		String queriedRecordId = queriedRecordIds.get(0).toString();
		
		List<String> returnedSObjectsIds;
		QueryResultObject queryResult;
		List<Map<String, Object>> records;
		
		try {
			
			flow = lookupMessageProcessor("delete-from-message");
			flow.process(getTestEvent(testObjects));
			
			Thread.sleep(60000);
			
			flow = lookupMessageProcessor("paginated-query-with-deleted-records");
			response = flow.process(getTestEvent(testObjects));
			
			do {
				
				returnedSObjectsIds = new ArrayList<String>();
				
				queryResult =  (QueryResultObject) response.getMessage().getPayload();
				
				records =  (List<Map<String, Object>>) queryResult.getData();
		        
		        Iterator<Map<String, Object>> iter = records.iterator();  

				while (iter.hasNext()) {
					
					Map<String, Object> sObject = iter.next();
					returnedSObjectsIds.add(sObject.get("Id").toString());
					
				}
				
				assertTrue(returnedSObjectsIds.size() > 0);
				
				testObjects.put("queryResultObjectRef", queryResult);
				
				flow = lookupMessageProcessor("paginated-query-by-queryResultObject-ref");
				response = flow.process(getTestEvent(testObjects));
				
			} while (!returnedSObjectsIds.contains(queriedRecordId) && queryResult.hasMore());
			
			assertTrue(returnedSObjectsIds.contains(queriedRecordId));
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}