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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.SaveResult;



public class DeleteTestCases extends SalesforceTestParent {

	@Before
	public void setUp() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("createRecord");
			
			flow = lookupMessageProcessor("create-from-message");
	        response = flow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResults =  (List<SaveResult>) response.getMessage().getPayload();
	        
	        Iterator<SaveResult> iter = saveResults.iterator();  

			while (iter.hasNext()) {
				
				SaveResult saveResult = iter.next();
				sObjectsIds.add(saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}
	
	@Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testDeleteChildElementsFromMessage() {
		
		try {
			
	    flow = lookupMessageProcessor("delete-from-message");
	    response = flow.process(getTestEvent(testObjects));
		
		List<DeleteResult> deleteResults =  (List<DeleteResult>) response.getMessage().getPayload();
		
		Iterator<DeleteResult> iter = deleteResults.iterator();  

		while (iter.hasNext()) {
			
			DeleteResult deleteResult = iter.next();
			assertTrue(deleteResult.getSuccess());
			
		}

		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}