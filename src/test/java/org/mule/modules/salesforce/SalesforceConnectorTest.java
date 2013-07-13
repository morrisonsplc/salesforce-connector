package org.mule.modules.salesforce;

import com.sforce.soap.partner.sobject.SObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public class SalesforceConnectorTest {

    SalesforceConnector connector;

    @Before
    public void createConnector() {
        connector = new SalesforceConnector();
    }

    @Test
    public void shouldRecursivelyConvertMapsToSObjects() {
        Map<String, Object> account = new HashMap<String, Object>();
        account.put("ExternalId__c", 138);
        Map<String, Object> contact = new HashMap<String, Object>();
        contact.put("ExternalId__c", "bmurray");
        contact.put("FirstName", "Bill");
        contact.put("LastName", "Murray");
        contact.put("Account", account);


        SObject record = connector.toSObject("Contact", contact);
        assertEquals("Contact", record.getType());
        assertEquals("Bill", record.getField("FirstName"));
        assertEquals("Murray", record.getField("LastName"));
        assertEquals("bmurray", record.getField("ExternalId__c"));

        SObject parentRecord = (SObject) record.getField("Account");
        assertEquals("Account", parentRecord.getType());
        assertEquals(138, parentRecord.getField("ExternalId__c"));

    }

}
