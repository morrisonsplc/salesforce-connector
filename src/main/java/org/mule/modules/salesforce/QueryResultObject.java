/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;

public class QueryResultObject {
    
    private QueryResult queryResult;
    
    public QueryResultObject(QueryResult queryResult) {
        Validate.notNull(queryResult);
        this.queryResult = queryResult;
    }
    
    public List<Map<String, Object>> getData(){
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SObject object : queryResult.getRecords()) result.add(SalesforceUtils.toMap(object));
        return result;
    }

    public boolean hasMore(){
        return !queryResult.isDone();
    }

    public String getQueryLocator() {
        return queryResult.getQueryLocator();
    }
    
}