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
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.sforce.soap.partner.GetUserInfoResult;



public class GetUserInfoTestCases extends SalesforceTestParent {
     
    @Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testGetUserInfo() {
    	
		try {
			
			testObjects = (HashMap<String,Object>) context.getBean("getUserInfoResult");
			
			flow = lookupMessageProcessor("get-user-info");
	        response = flow.process(getTestEvent(null));
	        
	        GetUserInfoResult userInfoResult =  (GetUserInfoResult) response.getMessage().getPayload();

	        assertEquals(testObjects.get("userName").toString(), userInfoResult.getUserName()); 
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
     
	}

}