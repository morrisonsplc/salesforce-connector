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

    public void updateTimestamp(GetUpdatedResult getUpdatedResult, String type) throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey(type))) {
            if (objectStore.contains(getLatestUpdateTimeBackupKey(type))) {
                objectStore.remove(getLatestUpdateTimeBackupKey(type));
            }
            objectStore.store(getLatestUpdateTimeBackupKey(type), objectStore.retrieve(getLastUpdateTimeKey(type)));
            objectStore.remove(getLastUpdateTimeKey(type));
        }
        objectStore.store(getLastUpdateTimeKey(type), getUpdatedResult.getLatestDateCovered());
    }

    public Calendar getTimestamp(String type) throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey(type))) {
            return (Calendar) objectStore.retrieve(getLastUpdateTimeKey(type));
        } else if (objectStore.contains(getLatestUpdateTimeBackupKey(type))) {
            return (Calendar) objectStore.retrieve(getLatestUpdateTimeBackupKey(type));
        }
        return null;
    }

    public void resetTimestamps(String type) throws ObjectStoreException {
        if (objectStore.contains(getLastUpdateTimeKey(type))) {
            objectStore.remove(getLastUpdateTimeKey(type));
        }
        if (objectStore.contains(getLatestUpdateTimeBackupKey(type))) {
            objectStore.remove(getLatestUpdateTimeBackupKey(type));
        }
    }

    public String getLastUpdateTimeKey(String type) {
        return keyPrefix + '/' + type + '/' + LATEST_UPDATE_TIME_KEY;
    }

    public String getLatestUpdateTimeBackupKey(String type) {
        return keyPrefix + '/' + type + '/' + LATEST_UPDATE_TIME_BACKUP_KEY;
    }
}