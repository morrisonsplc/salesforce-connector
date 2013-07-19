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
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.sforce.soap.partner.SaveResult;



public class CreateSingleTestCases extends SalesforceTestParent {
	
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
	public void testCreateSingleChildElementsFromMessage() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("createSingleRecord");
			
			flow = lookupMessageProcessor("create-single-from-message");
	        response = flow.process(getTestEvent(testObjects));

	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
			
	        assertTrue(saveResult.getSuccess());
	        
	        sObjectsIds.add(saveResult.getId());
	        
			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}

}