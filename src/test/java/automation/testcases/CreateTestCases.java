/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package automation.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.SaveResult;



public class CreateTestCases extends SalesforceTestParent {
	
	@After
	public void tearDown() {
		
		try {
			
	    flow = lookupFlowConstruct("delete-from-message");
		flow.process(getTestEvent(testObjects));
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}
	
    @Category({SmokeTests.class, SanityTests.class})
	@Test
	public void testCreateChildElementsFromMessage() {
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("createRecord");
			
			flow = lookupFlowConstruct("create-from-message");
	        response = flow.process(getTestEvent(testObjects));
	        
	        List<SaveResult> saveResults =  (List<SaveResult>) response.getMessage().getPayload();
	        
	        Iterator<SaveResult> iter = saveResults.iterator();  

			while (iter.hasNext()) {
				
				SaveResult saveResult = iter.next();
				assertTrue(saveResult.getSuccess());
				sObjectsIds.add(saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}

}