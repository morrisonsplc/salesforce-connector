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
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;

import java.util.Calendar;

public class ObjectStoreHelper {

    private static final String LATEST_UPDATE_TIME_KEY = "latestUpdateTime";
    private static final String LATEST_UPDATE_TIME_BACKUP_KEY = "latestUpdateTimeBackup";
    private ObjectStore objectStore;
    private String keyPrefix;

    public ObjectStoreHelper(String keyPrefix, ObjectStore objectStore) {
        this.keyPrefix = keyPrefix + '-';
        this.objectStore = objectStore;
    }

    public void updateTimestamp(GetUpdatedResult getUpdatedResult) throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey())) {
            if (objectStore.contains(getLatestUpdateTimeBackupKey())) {
                objectStore.remove(getLatestUpdateTimeBackupKey());
            }
            objectStore.store(getLatestUpdateTimeBackupKey(), objectStore.retrieve(getLastUpdateTimeKey()));
            objectStore.remove(getLastUpdateTimeKey());
        }
        objectStore.store(getLastUpdateTimeKey(), getUpdatedResult.getLatestDateCovered());
    }

    public Calendar getTimestamp() throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey())) {
            return (Calendar) objectStore.retrieve(getLastUpdateTimeKey());
        } else if (objectStore.contains(getLatestUpdateTimeBackupKey())) {
            return (Calendar) objectStore.retrieve(getLatestUpdateTimeBackupKey());
        }
        return null;
    }

    public void resetTimestamps() throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey())) {
            objectStore.remove(getLastUpdateTimeKey());
        }
        if (objectStore.contains(getLatestUpdateTimeBackupKey())) {
            objectStore.remove(getLatestUpdateTimeBackupKey());
        }
    }

    public String getLastUpdateTimeKey() {
        return keyPrefix + LATEST_UPDATE_TIME_KEY;
    }

    public String getLatestUpdateTimeBackupKey() {
        return keyPrefix + LATEST_UPDATE_TIME_BACKUP_KEY;
    }
}