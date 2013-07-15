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
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.sforce.soap.partner.EmptyRecycleBinResult;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.SaveResult;



public class EmptyRecycleBinTestCases extends SalesforceTestParent {
	
	private MessageProcessor createFlow;
	private MessageProcessor deleteFlow;
	private MessageProcessor emptyRecycleBinFlow;

	@Before
	public void setUp() {
		
		createFlow = lookupFlowConstruct("create-from-message");
		deleteFlow = lookupFlowConstruct("delete-from-message");
		emptyRecycleBinFlow = lookupFlowConstruct("empty-recycle-bin");
		
	}
	
    @Category({RegressionTests.class})
    @Test
	public void testEmptyRecycleBin() {
    	
    	
    	
    	List<String> sObjectsIds = new ArrayList<String>();
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("emptyRecycleBinTestData");
			
	        MuleEvent createResponse = createFlow.process(getTestEvent(testObjects));
	        List<SaveResult> saveResults =  (List<SaveResult>) createResponse.getMessage().getPayload();
	        Iterator<SaveResult> saveResultsIter = saveResults.iterator();  

			while (saveResultsIter.hasNext()) {
				
				SaveResult saveResult = saveResultsIter.next();
				sObjectsIds.add(saveResult.getId());
				
			}

			testObjects.put("idsToDeleteFromMessage", sObjectsIds);
			testObjects.put("idsRef", sObjectsIds);
			
			deleteFlow.process(getTestEvent(testObjects));	
			
			MuleEvent emptyResponse = emptyRecycleBinFlow.process(getTestEvent(testObjects));
	        List<EmptyRecycleBinResult> emptyRecycleBinResults =  (List<EmptyRecycleBinResult>) emptyResponse.getMessage().getPayload();
	        Iterator<EmptyRecycleBinResult> emptyRecycleBinResultsIter = emptyRecycleBinResults.iterator(); 
	        
			while (emptyRecycleBinResultsIter.hasNext()) {
				
				EmptyRecycleBinResult emptyRecycleBinResult = emptyRecycleBinResultsIter.next();
				assertTrue(emptyRecycleBinResult.getSuccess());
				assertTrue(sObjectsIds.contains(emptyRecycleBinResult.getId()));
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}

}