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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sforce.soap.partner.DeletedRecord;
import com.sforce.soap.partner.GetDeletedResult;
import com.sforce.soap.partner.SaveResult;



public class GetDeletedRangeTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("getDeletedRangeTestData");
			
			flow = lookupMessageProcessor("create-from-message");
	        response = flow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResults =  (List<SaveResult>) response.getMessage().getPayload();
	        
	        Iterator<SaveResult> saveResultsIter = saveResults.iterator();  

			while (saveResultsIter.hasNext()) {
				
				SaveResult saveResult = saveResultsIter.next();
				sObjectsIds.add(saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
			
			flow = lookupMessageProcessor("delete-from-message");
			flow.process(getTestEvent(testObjects));
	
			flow = lookupMessageProcessor("get-deleted");
			response = flow.process(getTestEvent(testObjects));
			
			GetDeletedResult deletedResult =  (GetDeletedResult) response.getMessage().getPayload();
			DeletedRecord[] deletedRecords = deletedResult.getDeletedRecords();
			
			GregorianCalendar endTime = (GregorianCalendar) ((DeletedRecord) deletedRecords[0]).getDeletedDate();
			endTime.add(GregorianCalendar.MINUTE, 1);
			
			GregorianCalendar startTime = (GregorianCalendar) endTime.clone(); 
			startTime.add(GregorianCalendar.MINUTE, -(Integer.parseInt((String) testObjects.get("duration"))));
			
			testObjects.put("endTime", endTime);
			testObjects.put("startTime", startTime);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testGetDeletedRange() {
		
		List<String> createdRecordsIds = (List<String>) testObjects.get("idsToDeleteFromMessage");
		
		try {
			
			flow = lookupMessageProcessor("get-deleted-range");
			response = flow.process(getTestEvent(testObjects));
			
			GetDeletedResult deletedResult =  (GetDeletedResult) response.getMessage().getPayload();
			
			DeletedRecord[] deletedRecords = deletedResult.getDeletedRecords();
			
			assertTrue(deletedRecords != null && deletedRecords.length > 0);

			for (int i = 0; i < deletedRecords.length; i++) {
				assertTrue(createdRecordsIds.contains(((DeletedRecord) deletedRecords[i]).getId())); 
		     }
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}