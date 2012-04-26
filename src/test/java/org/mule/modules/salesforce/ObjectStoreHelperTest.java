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

import com.sforce.soap.partner.GetUpdatedResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class ObjectStoreHelperTest {

    private static final String TYPE = "Account";
    private ObjectStore objectStore;
    @Mock
    private GetUpdatedResult getUpdatedResult;
    @Mock
    private Calendar timestamp1;
    @Mock
    private Calendar timestamp2;
    @Mock
    private Calendar timestampInSalesforceResponse;
    private ObjectStoreHelper objectStoreHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        objectStore = new SimpleMemoryObjectStore();
        objectStoreHelper = new ObjectStoreHelper("myPrefix", objectStore);
    }

    @Test
    public void testUpdateTimestampObjectStoreEmtpy() throws Exception {
        when(getUpdatedResult.getLatestDateCovered()).thenReturn(timestampInSalesforceResponse);
        objectStoreHelper.updateTimestamp(getUpdatedResult, TYPE);
        assertLastUpdateTimeIs(timestampInSalesforceResponse);
    }

    @Test
    public void testUpdateTimestampOnlyBackupTimestampAvailable() throws Exception {
        when(getUpdatedResult.getLatestDateCovered()).thenReturn(timestampInSalesforceResponse);
        storeLastUpdateTimeBackup(timestamp1);
        objectStoreHelper.updateTimestamp(getUpdatedResult, TYPE);
        assertLastUpdateTimeIs(timestampInSalesforceResponse);
        assertLastUpdateTimeBackupIs(timestamp1);
    }

    @Test
    public void testUpdateTimestamp() throws Exception {
        when(getUpdatedResult.getLatestDateCovered()).thenReturn(timestampInSalesforceResponse);
        storeLastUpdateTime(timestamp1);
        storeLastUpdateTimeBackup(timestamp2);
        objectStoreHelper.updateTimestamp(getUpdatedResult, TYPE);
        assertLastUpdateTimeIs(timestampInSalesforceResponse);
        assertLastUpdateTimeBackupIs(timestamp1);
    }

    @Test
    public void testUpdateTimestampStoreTimestampFailsMakeSureBackUpIsInPlace() throws Exception {
        ObjectStore objectStore = Mockito.spy(this.objectStore);
        ObjectStoreHelper objectStoreHelper = new ObjectStoreHelper("myPrefix", objectStore);
        when(getUpdatedResult.getLatestDateCovered()).thenReturn(timestampInSalesforceResponse);
        storeLastUpdateTime(timestamp1);
        storeLastUpdateTimeBackup(timestamp2);
        doThrow(new ObjectStoreException()).when(objectStore).store(eq(objectStoreHelper.getLastUpdateTimeKey(TYPE)), Matchers.<Serializable>anyObject());
        try {
            objectStoreHelper.updateTimestamp(getUpdatedResult, TYPE);
            fail("ObjectStoreException should have been thrown");
        } catch (ObjectStoreException ose) {
            assertLastUpdateTimeBackupIs(timestamp1);
        }
    }

    @Test
    public void testGetTimestampEmptyStore() throws Exception {
        assertNull(objectStoreHelper.getTimestamp(TYPE));
    }

    @Test
    public void testGetTimestamp() throws Exception {
        storeLastUpdateTime(timestamp1);
        assertEquals(timestamp1, objectStoreHelper.getTimestamp(TYPE));
    }

    @Test
    public void testGetTimestampOnlyBackupAvailable() throws Exception {
        storeLastUpdateTimeBackup(timestamp2);
        assertEquals(timestamp2, objectStoreHelper.getTimestamp(TYPE));
    }

    @Test
    public void testResetTimestamps() throws Exception {
        storeLastUpdateTime(timestamp1);
        storeLastUpdateTimeBackup(timestamp2);
        objectStoreHelper.resetTimestamps(TYPE);
        assertFalse(objectStore.contains(lastUpdateTimeKey()));
        assertFalse(objectStore.contains(lastUpdateTimeBackupKey()));
    }

    private void assertLastUpdateTimeIs(Calendar timestamp) throws ObjectStoreException {
        assertEquals(timestamp, objectStore.retrieve(lastUpdateTimeKey()));
    }

    private void assertLastUpdateTimeBackupIs(Calendar timestamp) throws ObjectStoreException {
        assertEquals(timestamp, objectStore.retrieve(lastUpdateTimeBackupKey()));
    }

    private void storeLastUpdateTime(Calendar timestamp) throws ObjectStoreException {
        objectStore.store(lastUpdateTimeKey(), timestamp);
    }

    private String lastUpdateTimeKey() {
        return objectStoreHelper.getLastUpdateTimeKey(TYPE);
    }

    private void storeLastUpdateTimeBackup(Calendar timestamp) throws ObjectStoreException {
        objectStore.store(lastUpdateTimeBackupKey(), timestamp);
    }

    private String lastUpdateTimeBackupKey() {
        return objectStoreHelper.getLatestUpdateTimeBackupKey(TYPE);
    }
}
