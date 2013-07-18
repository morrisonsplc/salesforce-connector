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

import com.sforce.ws.bind.XmlObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Mulesoft, Inc
 */
public class SalesforceUtils {

    public static Map<String, Object> toMap(XmlObject xmlObject) {
        Map<String, Object> map = new HashMap<String, Object>();
        Object value = xmlObject.getValue();

        if (value == null && xmlObject.hasChildren()) {
            XmlObject child;
            Iterator childrenIterator = xmlObject.getChildren();

            while (childrenIterator.hasNext()) {
                child = (XmlObject) childrenIterator.next();
                if (child != null && child.getValue() != null) {
                    map.put(child.getName().getLocalPart(), child.getValue());
                } else if( child.getChildren().hasNext() ) {
                    map.put(child.getName().getLocalPart(), toMap(child));
                } else {
                    map.put(child.getName().getLocalPart(), null);
                }
            }
        }

        return map;
    }
}
