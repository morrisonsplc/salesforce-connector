/**
2 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.*;

import com.sforce.ws.bind.XmlObject;
import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.api.annotations.Category;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.InvalidateConnectionOn;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.oauth.OAuthInvalidateAccessTokenOn;
import org.mule.api.annotations.oauth.OAuthProtected;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.MetaDataKeyParam;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.callback.StopSourceCallback;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.Registry;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.springframework.util.StringUtils;

import com.sforce.async.AsyncApiException;
import com.sforce.async.AsyncExceptionCode;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchRequest;
import com.sforce.async.BatchResult;
import com.sforce.async.BulkConnection;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;
import com.sforce.async.QueryResultList;
import com.sforce.soap.partner.AssignmentRuleHeader_element;
import com.sforce.soap.partner.CallOptions_element;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.EmptyRecycleBinResult;
import com.sforce.soap.partner.GetDeletedResult;
import com.sforce.soap.partner.GetUpdatedResult;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.LeadConvert;
import com.sforce.soap.partner.LeadConvertResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.SearchRecord;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public abstract class BaseSalesforceConnector implements MuleContextAware {
    private static final Logger LOGGER = Logger.getLogger(BaseSalesforceConnector.class);

    /**
     * Object store manager to obtain a store to support {@link this#getUpdatedObjects}
     */
    private ObjectStoreManager objectStoreManager;

    /**
     * A ObjectStore instance to use in {@link this#getUpdatedObjects}
     */
    @Configurable
    @Optional
    private ObjectStore<? extends Serializable> timeObjectStore;

    /**
     * Client ID for partners
     */
    @Configurable
    @Optional
    private String clientId;

    /**
     * The ID of a specific assignment rule to run for the Case or Lead. The assignment rule can be active or inactive. The ID can be retrieved by querying the AssignmentRule object. If specified, do not specify useDefaultRule. This element is ignored for accounts, because all territory assignment rules are applied.
     *
     * If the value is not in correct ID format (15-character or 18-character Salesforce ID), the call fails and a MALFORMED_ID exception is returned.
     */
    @Configurable
    @Optional
    private String assignmentRuleId;

    /**
     * If true for a Case or Lead, uses the default (active) assignment rule for a Case or Lead. If specified, do not specify an assignmentRuleId. If true for an Account, all territory assignment rules are applied, and if false, no territory assignment rules are applied.
     */
    @Configurable
    @Optional
    private Boolean useDefaultRule;

    /**
     * If true, truncate field values that are too long, which is the behavior in API versions 14.0 and earlier.
     *
     * Default is false: no change in behavior. If a string or textarea value is too large, the operation fails and the fault code STRING_TOO_LONG is returned.
     */
    @Configurable
    @Optional
    private Boolean allowFieldTruncationSupport;

    private ObjectStoreHelper objectStoreHelper;

    private Registry registry;
    
    private static final List<Subscription> subscriptions = new ArrayList<Subscription>();
    
    private static class Subscription {
    	private String topic;
    	private SourceCallback callback;
        private boolean subscribed;
    	
    	private Subscription(String topic, SourceCallback callback, boolean subscribed) {
    		this.topic = topic;
    		this.callback = callback;
            this.subscribed = subscribed;
    	}
    	
    	public SourceCallback getCallback() {
			return callback;
		}
    	
    	public String getTopic() {
			return topic;
		}

        public boolean isSubscribed() {
            return subscribed;
        }
    }

    /**
     * Bayeux client
     */
    private SalesforceBayeuxClient bc;

    protected abstract PartnerConnection getConnection();

    protected abstract BulkConnection getBulkConnection();

    protected abstract String getSessionId();
    
    protected abstract boolean isReadyToSubscribe();

    protected SalesforceBayeuxClient getBayeuxClient() {
        try {
            if (bc == null && getConnection() != null &&
                getConnection().getConfig() != null) {
                bc = new SalesforceBayeuxClient(this);

                if (!bc.isHandshook()) {
                    bc.handshake();
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        }

        return bc;
    }

    protected boolean isInitializedBayeuxClient() {
        return this.bc != null;
    }
    
    protected void setBayeuxClient(SalesforceBayeuxClient bc) {
        this.bc = bc;
    }

    protected void setObjectStoreHelper(ObjectStoreHelper objectStoreHelper) {
        this.objectStoreHelper = objectStoreHelper;
    }

    /**
     * Adds one or more new records to your organization's data.
     * <p/>
     * <p class="caution">
     * IMPORTANT: When you map your objects to the input of this message processor keep in mind that they need
     * to match the expected type of the object at Salesforce.
     * <p/>
     * Take the CloseDate of an Opportunity as an example, if you set that field to a string of value "2011-12-13"
     * it will be sent to Salesforce as a string and operation will be rejected on the basis that CloseDate is not
     * of the expected type.
     * <p/>
     * The proper way to actually map it is to generate a Java Date object, you can do so using Groovy expression
     * evaluator as <i>#[groovy:Date.parse("yyyy-MM-dd", "2011-12-13")]</i>.
     * </p>
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create}
     *
     * @param objects An array of one or more sObjects objects.
     * @param type    Type of object to create
     * @return An array of {@link com.sforce.soap.partner.SaveResult} if async is false
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_create.htm">create()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<SaveResult> create(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                   @Placement(group = "sObject Field Mappings") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return Arrays.asList(getConnection().create(toSObjectList(type, objects)));
    }

    /**
     * Creates a Job in order to perform one or more batches through Bulk API Operations.
     *
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-job:example-1}
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-job:example-2}
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-job:example-3}
     *
     * @param operation           The {@link com.sforce.async.OperationEnum} that will be executed by the job.
     * @param type                The type of Salesforce object that the job will process.
     * @param externalIdFieldName Contains the name of the field on this object with the external ID field attribute
     *                            for custom objects or the idLookup field property for standard objects
     *                            (only required for Upsert Operations).
     * @param contentType         The Content Type for this Job results. When specifying a content type different from
     *                            XML for a query type use {@link #queryResultStream(com.sforce.async.BatchInfo)}
     *                            batchResultStream} method to retrieve results.
     * @param concurrencyMode     The concurrency mode of the job, either Parallel or Serial.
     * @return A {@link com.sforce.async.JobInfo} that identifies the created Job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_jobinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_jobs_create.htm">createJob()</a>
     * @since 4.3
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = AsyncApiException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public JobInfo createJob(OperationEnum operation, @MetaDataKeyParam String type, @Optional String externalIdFieldName, @Optional ContentType contentType, @Optional ConcurrencyMode concurrencyMode) throws Exception {
        return createJobInfo(operation, type, externalIdFieldName, contentType, concurrencyMode);
    }

    /**
     * Closes an open Job given its ID.
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:close-job}
     *
     * @param jobId The Job ID identifying the Job to be closed.
     * @return A {@link JobInfo} that identifies the closed Job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_jobinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_jobs_close.htm">closeJob()</a>
     * @since 4.3
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = AsyncApiException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public JobInfo closeJob(String jobId) throws Exception {
        return getBulkConnection().closeJob(jobId);
    }

    /**
     * Aborts an open Job given its ID.
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:abort-job}
     *
     * @param jobId The Job ID identifying the Job to be aborted.
     * @return A {@link JobInfo} that identifies the aborted Job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_jobinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_jobs_abort.htm">abortJob()</a>
     * @since 5.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = AsyncApiException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public JobInfo abortJob(String jobId) throws Exception {
        return getBulkConnection().abortJob(jobId);
    }

    /**
     * Creates a Batch using the given objects within the specified Job.
     * <p/>
     * This call uses the Bulk API. The operation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-batch}
     *
     * @param jobInfo The {@link JobInfo} in which the batch will be created.
     * @param objects A list of one or more sObjects objects. This parameter defaults to payload content.
     * @return A {@link com.sforce.async.BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.3
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo createBatch(JobInfo jobInfo, @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return createBatchAndCompleteRequest(jobInfo, objects);
    }

    /**
     * Creates a Batch using the given stream within the specified Job.
     * <p/>
     * This call uses the Bulk API. The operation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-batch-stream}
     *
     * @param jobInfo The {@link JobInfo} in which the batch will be created.
     * @param stream A stream containing the data. This parameter defaults to payload content.
     * @return A {@link com.sforce.async.BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 5.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo createBatchStream(JobInfo jobInfo, @Optional @Default("#[payload]") InputStream stream) throws Exception {
        return getBulkConnection().createBatchFromStream(jobInfo, stream);
    }

    /**
     * Creates a Batch using the given query.
     * <p/>
     * This call uses the Bulk API. The operation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-batch-for-query}
     *
     * @param jobInfo The {@link JobInfo} in which the batch will be created.
     * @param query   The query to be executed.
     * @return A {@link BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.5
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo createBatchForQuery(JobInfo jobInfo, @Optional @Default("#[payload]") String query) throws Exception {
        InputStream queryStream = new ByteArrayInputStream(query.getBytes());
        return createBatchForQuery(jobInfo, queryStream);
    }

    /**
     * Adds one or more new records to your organization's data.
     * <p/>
     * This call uses the Bulk API. The creation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-bulk}
     *
     * @param objects An array of one or more sObjects objects.
     * @param type    Type of object to create
     * @return A {@link BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo createBulk(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                @Placement(group = "sObject Field Mappings") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {

        return createBatchAndCompleteRequest(createJobInfo(OperationEnum.insert, type), objects);
    }

    /**
     * Adds one new records to your organization's data.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:create-single}
     * {@sample.java ../../../doc/mule-module-sfdc.java.sample sfdc:create-single}
     *
     * @param object SObject to create
     * @param type   Type of object to create
     * @return An array of {@link SaveResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_create.htm">create()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public SaveResult createSingle(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                   @Placement(group = "sObject Field Mappings") @FriendlyName("sObject") @Optional @Default("#[payload]") Map<String, Object> object) throws Exception {
        SaveResult[] saveResults = getConnection().create(new SObject[]{toSObject(type, object)});
        if (saveResults.length > 0) {
            return saveResults[0];
        }

        return null;
    }

    /**
     * Updates one or more existing records in your organization's data.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:update}
     *
     * @param objects An array of one or more sObjects objects.
     * @param type    Type of object to update
     * @return An array of {@link SaveResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_update.htm">update()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<SaveResult> update(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                   @Placement(group = "Salesforce sObjects list") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return Arrays.asList(getConnection().update(toSObjectList(type, objects)));
    }

    /**
     * Updates one or more existing records in your organization's data.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:update-single}
     *
     * @param object The object to be updated.
     * @param type   Type of object to update
     * @return A {@link SaveResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_update.htm">update()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public SaveResult updateSingle(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                   @Placement(group = "Salesforce Object") @FriendlyName("sObject") @Optional @Default("#[payload]") Map<String, Object> object) throws Exception {
        return getConnection().update(new SObject[]{toSObject(type, object)})[0];
    }

    /**
     * Updates one or more existing records in your organization's data.
     * <p/>
     * This call uses the Bulk API. The creation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:update-bulk}
     *
     * @param objects An array of one or more sObjects objects.
     * @param type    Type of object to update
     * @return A {@link BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo updateBulk(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                @Placement(group = "Salesforce sObjects list") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return createBatchAndCompleteRequest(createJobInfo(OperationEnum.update, type), objects);
    }

    /**
     * <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_upsert.htm">Upserts</a>
     * an homogeneous list of objects: creates new records and updates existing records, using a custom field to determine the presence of existing records.
     * In most cases, prefer {@link #upsert(String, String, List)} over {@link #create(String, List)},
     * to avoid creating unwanted duplicate records.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:upsert}
     *
     * @param externalIdFieldName Contains the name of the field on this object with the external ID field attribute
     *                            for custom objects or the idLookup field property for standard objects.
     * @param type                the type of the given objects. The list of objects to upsert must be homogeneous
     * @param objects             the objects to upsert
     * @return a list of {@link com.sforce.soap.partner.UpsertResult}, one for each passed object
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error if a connection error occurs
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_upsert.htm">upsert()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<UpsertResult> upsert(@Placement(group = "Information") String externalIdFieldName,
                                     @MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                     @Placement(group = "Salesforce sObjects list") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return Arrays.asList(getConnection().upsert(externalIdFieldName, toSObjectList(type, objects)));
    }

    /**
     * <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_upsert.htm">Upserts</a>
     * an homogeneous list of objects: creates new records and updates existing records, using a custom field to determine the presence of existing records.
     * In most cases, prefer {@link #upsert(String, String, List)} over {@link #create(String, List)},
     * to avoid creating unwanted duplicate records.
     * <p/>
     * This call uses the Bulk API. The creation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:upsert-bulk}
     *
     * @param externalIdFieldName Contains the name of the field on this object with the external ID field attribute
     *                            for custom objects or the idLookup field property for standard objects.
     * @param type                the type of the given objects. The list of objects to upsert must be homogeneous
     * @param objects             the objects to upsert
     * @return A {@link BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo upsertBulk(@MetaDataKeyParam @Placement(group = "Information", order = 1) @FriendlyName("sObject Type") String type,
                                @Placement(group = "Information", order = 2) String externalIdFieldName,
                                @Placement(group = "Salesforce sObjects list") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return createBatchAndCompleteRequest(createJobInfo(OperationEnum.upsert, type, externalIdFieldName, null, null), objects);
    }

    /**
     * Access latest {@link BatchInfo} of a submitted {@link BatchInfo}. Allows to track execution status.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:batch-info}
     *
     * @param batchInfo the {@link BatchInfo} being monitored
     * @return Latest {@link BatchInfo} representing status of the batch job result.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_get_info.htm">getBatchInfo()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo batchInfo(BatchInfo batchInfo) throws Exception {
        return getBulkConnection().getBatchInfo(batchInfo.getJobId(), batchInfo.getId());
    }

    /**
     * Access {@link com.sforce.async.BatchResult} of a submitted {@link BatchInfo}.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:batch-result}
     *
     * @param batchInfo the {@link BatchInfo} being monitored
     * @return {@link com.sforce.async.BatchResult} representing result of the batch job result.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_get_results.htm">getBatchResult()</a>
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_interpret_status.htm">BatchInfo status</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchResult batchResult(BatchInfo batchInfo) throws Exception {
        return getBulkConnection().getBatchResult(batchInfo.getJobId(), batchInfo.getId());
    }

    /**
     * Access {@link com.sforce.async.BatchResult} of a submitted {@link BatchInfo}.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:batch-result-stream}
     *
     * @param batchInfo the {@link BatchInfo} being monitored
     * @return {@link java.io.InputStream} representing result of the batch job result.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_get_results.htm">getBatchResult()</a>
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_interpret_status.htm">BatchInfo status</a>
     * @since 5.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public InputStream batchResultStream(BatchInfo batchInfo) throws Exception {
        return getBulkConnection().getBatchResultStream(batchInfo.getJobId(), batchInfo.getId());
    }

    /**
     * Returns an {@link InputStream} with the query results of a submitted {@link BatchInfo}
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:query-result-stream}
     *
     * @param batchInfo the {@link BatchInfo} being monitored
     * @return {@link InputStream} with the results of the Batch.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_get_results.htm">getBatchResult()</a>
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_interpret_status.htm">BatchInfo status</a>
     * @since 4.5
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public InputStream queryResultStream(BatchInfo batchInfo) throws Exception {
        QueryResultList queryResultList = getBulkConnection().getQueryResultList(batchInfo.getJobId(), batchInfo.getId());
        String[] results = queryResultList.getResult();
        if (results.length > 0) {
            List<InputStream> inputStreams = new ArrayList<InputStream>(results.length);
            for (String resultId : queryResultList.getResult()) {
                inputStreams.add(getBulkConnection().getQueryResultStream(batchInfo.getJobId(), batchInfo.getId(), resultId));
            }
            return new SequenceInputStream(Collections.enumeration(inputStreams));
        }
        return null;
    }

    /**
     * Retrieves a list of available objects for your organization's data.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:describe-global}
     *
     * @return A {@link com.sforce.soap.partner.DescribeGlobalResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_describeglobal.htm">describeGlobal()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Describe Calls", description = "A set of calls to describe record structure in Salesforce.")
    public DescribeGlobalResult describeGlobal() throws Exception {
        return getConnection().describeGlobal();
    }

    /**
     * Retrieves one or more records based on the specified IDs.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:retrieve}
     *
     * @param type   Object type. The sp ecified value must be a valid object for your organization.
     * @param ids    The ids of the objects to retrieve
     * @param fields The fields to return for the matching objects
     * @return An array of {@link SObject}s
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<Map<String, Object>> retrieve(@MetaDataKeyParam @Placement(group = "Information", order = 1) @FriendlyName("sObject Type") String type,
                                              @Placement(group = "Ids to Retrieve") List<String> ids,
                                              @Placement(group = "Fields to Retrieve") List<String> fields) throws Exception {
        String fiedsCommaDelimited = StringUtils.collectionToCommaDelimitedString(fields);
        SObject[] sObjects = getConnection().retrieve(fiedsCommaDelimited, type, ids.toArray(new String[ids.size()]));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (sObjects != null) {
            for (SObject sObject : sObjects) {
                result.add(SalesforceUtils.toMap(sObject));
            }
        }
        return result;
    }

    /**
     * Executes a paginated query against the specified object and returns data that matches the specified criteria.
     * The returned class QueryResultObject provides the methods getData() to retrieve the results in a List<Maps> 
     * and hasMore() to check if there are more pages to retrieve from the server.
     * The query result object contains up to 500 rows of data by default and can be increased up to 2,000 rows.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:paginated-query}
     *
     * @param query Query string that specifies the object to query, the fields to return, and any conditions for
     *              including a specific object in the query. For more information, see Salesforce Object Query
     *              Language (SOQL).
     * @param queryResultObject QueryResultObject returned by a previous call to this operation.
     *                          If this is set the other parameter will be ignored.
     * @param withDeletedRecords Flag that specifies whether or not to retrieve records that have been deleted.
     * @return {@link QueryResultObject} with the results of the query or null.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_query.htm">query()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public QueryResultObject paginatedQuery(@Placement(group = "Query") @Optional String query, 
                                            @Optional QueryResultObject queryResultObject, 
                                            @Optional @Default("false") Boolean withDeletedRecords) 
           throws Exception {
        
        if (queryResultObject == null) {
            QueryResult queryResult;
            if (withDeletedRecords) queryResult = getConnection().queryAll(query);
            else queryResult = getConnection().query(query);
            if (queryResult != null) return new QueryResultObject(queryResult);
        }
        else {
            if (queryResultObject.hasMore()){
                QueryResult queryResult = getConnection().queryMore(queryResultObject.getQueryLocator());
                if (queryResult != null) return new QueryResultObject(queryResult);
            }
        }
        
        return null;
    }

    /**
     * Executes a query against the specified object and returns data that matches the specified criteria.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:query}
     *
     * @param query Query string that specifies the object to query, the fields to return, and any conditions for
     *              including a specific object in the query. For more information, see Salesforce Object Query
     *              Language (SOQL).
     * @return An array of {@link SObject}s
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_query.htm">query()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<Map<String, Object>> query(@Placement(group = "Query") String query) throws Exception {
        QueryResult queryResult = getConnection().query(query);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        while (queryResult != null) {
            for (SObject object : queryResult.getRecords()) {
                result.add(SalesforceUtils.toMap(object));
            }
            if (queryResult.isDone()) {
                break;
            }
            queryResult = getConnection().queryMore(queryResult.getQueryLocator());
        }

        return result;
    }

    /**
     * Retrieves data from specified objects, whether or not they have been deleted.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:query}
     *
     * @param query Query string that specifies the object to query, the fields to return, and any conditions for including a specific object in the query. For more information, see Salesforce Object Query Language (SOQL).
     * @return An array of {@link SObject}s
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_query.htm">query()</a>
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<Map<String, Object>> queryAll(@Placement(group = "Query") String query) throws Exception {
        QueryResult queryResult = getConnection().queryAll(query);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        while (queryResult != null) {
            for (SObject object : queryResult.getRecords()) {
                result.add(SalesforceUtils.toMap(object));
            }
            if (queryResult.isDone()) {
                break;
            }
            queryResult = getConnection().queryMore(queryResult.getQueryLocator());
        }

        return result;
    }

    /**
     * Search for objects using Salesforce Object Search Language
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:search}
     *
     * @param query Query string that specifies the object to query, the fields to return, and any conditions for including a specific object in the query. For more information, see Salesforce Object Search Language (SOSL).
     * @return An array of {@link SObject}s
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_search.htm">search()</a>
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<Map<String, Object>> search(@Placement(group = "Query") String query) throws Exception {
        SearchResult searchResult = getConnection().search(query);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        for (SearchRecord object : searchResult.getSearchRecords()) {
            result.add(SalesforceUtils.toMap(object.getRecord()));
        }

        return result;
    }    
    
    /**
     * Executes a query against the specified object and returns the first record that matches the specified criteria.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:query-single}
     *
     * @param query Query string that specifies the object to query, the fields to return, and any conditions for
     *              including a specific object in the query. For more information, see Salesforce Object Query
     *              Language (SOQL).
     * @return A single {@link SObject}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_query.htm">query()</a>
     * @since 4.1
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public Map<String, Object> querySingle(@Placement(group = "Query") String query) throws Exception {
        SObject[] result = getConnection().query(query).getRecords();
        if (result.length > 0) {
            return SalesforceUtils.toMap(result[0]);
        }

        return null;
    }

    /**
     * Converts a Lead into an Account, Contact, or (optionally) an Opportunity.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:convert-lead}
     *
     * @param leadId                 ID of the Lead to convert. Required. For information on IDs, see ID Field Type.
     * @param contactId              ID of the Contact into which the lead will be merged (this contact must be
     *                               associated with the specified accountId, and an accountId must be specified).
     *                               Required only when updating an existing contact.IMPORTANT if you are converting
     *                               a lead into a person account, do not specify the contactId or an error will result.
     *                               Specify only the accountId of the person account. If no contactID is specified,
     *                               then the API creates a new contact that is implicitly associated with the Account.
     *                               To create a new contact, the client application must be logged in with sufficient
     *                               access rights. To merge a lead into an existing contact, the client application
     *                               must be logged in with read/write access to the specified contact. The contact
     *                               name and other existing data are not overwritten (unless overwriteLeadSource is
     *                               set to true, in which case only the LeadSource field is overwritten).
     *                               For information on IDs, see ID Field Type.
     * @param accountId              ID of the Account into which the lead will be merged. Required
     *                               only when updating an existing account, including person accounts.
     *                               If no accountID is specified, then the API creates a new account. To
     *                               create a new account, the client application must be logged in with
     *                               sufficient access rights. To merge a lead into an existing account,
     *                               the client application must be logged in with read/write access to the
     *                               specified account. The account name and other existing data are not overwritten.
     *                               For information on IDs, see ID Field Type.
     * @param overWriteLeadSource    Specifies whether to overwrite the LeadSource field on the target Contact object
     *                               with the contents of the LeadSource field in the source Lead object (true), or
     *                               not (false, the default). To set this field to true, the client application
     *                               must specify a contactId for the target contact.
     * @param doNotCreateOpportunity Specifies whether to create an Opportunity during lead conversion (false, the
     *                               default) or not (true). Set this flag to true only if you do not want to create
     *                               an opportunity from the lead. An opportunity is created by default.
     * @param opportunityName        Name of the opportunity to create. If no name is specified, then this value
     *                               defaults to the company name of the lead. The maximum length of this field is
     *                               80 characters. If doNotCreateOpportunity argument is true, then no Opportunity
     *                               is created and this field must be left blank; otherwise, an error is returned.
     * @param convertedStatus        Valid LeadStatus value for a converted lead. Required.
     *                               To obtain the list of possible values, the client application queries the
     *                               LeadStatus object, as in:
     *                               Select Id, MasterLabel from LeadStatus where IsConverted=true
     * @param sendEmailToOwner       Specifies whether to send a notification email to the owner specified in the
     *                               ownerId (true) or not (false, the default).
     * @return A {@link com.sforce.soap.partner.LeadConvertResult} object
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_convertlead.htm">convertLead()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public LeadConvertResult convertLead(String leadId, @Optional String contactId,
                                         @Optional String accountId,
                                         @Optional @Default("false") Boolean overWriteLeadSource,
                                         @Optional @Default("false") Boolean doNotCreateOpportunity,
                                         @Optional String opportunityName,
                                         String convertedStatus,
                                         @Optional @Default("false") Boolean sendEmailToOwner)
            throws Exception {

        LeadConvert leadConvert = new LeadConvert();
        leadConvert.setLeadId(leadId);
        leadConvert.setContactId(contactId);
        leadConvert.setAccountId(accountId);
        leadConvert.setOverwriteLeadSource(overWriteLeadSource);
        leadConvert.setDoNotCreateOpportunity(doNotCreateOpportunity);
        if (opportunityName != null) {
            leadConvert.setOpportunityName(opportunityName);
        }
        leadConvert.setConvertedStatus(convertedStatus);
        leadConvert.setSendNotificationEmail(sendEmailToOwner);
        LeadConvert[] list = new LeadConvert[1];
        list[0] = leadConvert;

        return getConnection().convertLead(list)[0];
    }

    /**
     * The recycle bin lets you view and restore recently deleted records for 30 days before they are
     * permanently deleted. Your organization can have up to 5000 records per license in the Recycle Bin at any
     * one time. For example, if your organization has five user licenses, 25,000 records can be stored in the
     * Recycle Bin. If your organization reaches its Recycle Bin limit, Salesforce.com automatically removes
     * the oldest records, as long as they have been in the recycle bin for at least two hours.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:empty-recycle-bin}
     *
     * @param ids Array of one or more IDs associated with the records to delete from the recycle bin.
     *            Maximum number of records is 200.
     * @return A list of {@link com.sforce.soap.partner.EmptyRecycleBinResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_emptyrecyclebin.htm">emptyRecycleBin()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<EmptyRecycleBinResult> emptyRecycleBin(@Placement(group = "Ids to Delete") List<String> ids) throws Exception {
        return Arrays.asList(getConnection().emptyRecycleBin(ids.toArray(new String[]{})));
    }


    /**
     * Deletes one or more records from your organization's data.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:delete}
     *
     * @param ids Array of one or more IDs associated with the objects to delete.
     * @return An array of {@link com.sforce.soap.partner.DeleteResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_delete.htm">delete()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public List<DeleteResult> delete(@Optional @Default("#[payload]") @Placement(group = "Ids to Delete") List<String> ids) throws Exception {
        return Arrays.asList(getConnection().delete(ids.toArray(new String[]{})));
    }

    /**
     * Deletes one or more records from your organization's data.
     * The deleted records are not stored in the Recycle Bin.
     * Instead, they become immediately eligible for deletion.
     * <p/>
     * This call uses the Bulk API. The creation will be done in asynchronous fashion.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:hard-delete-bulk}
     *
     * @param objects An array of one or more sObjects objects.
     * @param type    Type of object to update
     * @return A {@link BatchInfo} that identifies the batch job. {@see http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_create.htm">createBatch()</a>
     * @since 4.3
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Bulk API", description = "The Bulk API provides programmatic access to allow you to quickly load your organization's data into Salesforce.")
    public BatchInfo hardDeleteBulk(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                    @Placement(group = "Salesforce sObjects list") @FriendlyName("sObjects") @Optional @Default("#[payload]") List<Map<String, Object>> objects) throws Exception {
        return createBatchAndCompleteRequest(createJobInfo(OperationEnum.hardDelete, type), objects);
    }

    /**
     * Retrieves the list of individual records that have been created/updated within the given timespan for the specified object.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-updated-range}
     *
     * @param type      Object type. The specified value must be a valid object for your organization.
     * @param startTime Starting date/time (Coordinated Universal Time (UTC)not local timezone) of the timespan for
     *                  which to retrieve the data. The API ignores the seconds portion of the specified dateTime value '
     *                  (for example, 12:30:15 is interpreted as 12:30:00 UTC).
     * @param endTime   Ending date/time (Coordinated Universal Time (UTC)not local timezone) of the timespan for
     *                  which to retrieve the data. The API ignores the seconds portion of the specified dateTime value
     *                  (for example, 12:35:15 is interpreted as 12:35:00 UTC). If it is not provided, the current
     *                  server time will be used.
     * @return {@link com.sforce.soap.partner.GetUpdatedResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getupdatedrange.htm">getUpdatedRange()</a>
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public GetUpdatedResult getUpdatedRange(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                            @Placement(group = "Information") @FriendlyName("Start Time Reference") Calendar startTime,
                                            @Placement(group = "Information") @FriendlyName("End Time Reference") @Optional Calendar endTime) throws Exception {
        if (endTime == null) {
            Calendar serverTime = getConnection().getServerTimestamp().getTimestamp();
            endTime = (Calendar) serverTime.clone();
        }
        if (endTime.getTimeInMillis() - startTime.getTimeInMillis() < 60000) {
            endTime.add(Calendar.MINUTE, 1);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting updated " + type + " objects between " + startTime.getTime() + " and " + endTime.getTime());
        }
        return getConnection().getUpdated(type, startTime, endTime);
    }

    /**
     * Retrieves the list of individual records that have been deleted within the given timespan for the specified object.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-deleted-range}
     * {@sample.java ../../../doc/mule-module-sfdc.java.sample sfdc:get-deleted-range}
     *
     * @param type      Object type. The specified value must be a valid object for your organization.
     * @param startTime Starting date/time (Coordinated Universal Time (UTC)not local timezone) of the timespan for
     *                  which to retrieve the data. The API ignores the seconds portion of the specified dateTime value '
     *                  (for example, 12:30:15 is interpreted as 12:30:00 UTC).
     * @param endTime   Ending date/time (Coordinated Universal Time (UTC)not local timezone) of the timespan for
     *                  which to retrieve the data. The API ignores the seconds portion of the specified dateTime value
     *                  (for example, 12:35:15 is interpreted as 12:35:00 UTC). If not specific, the current server
     *                  time will be used.
     * @return {@link com.sforce.soap.partner.GetDeletedResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getdeletedrange.htm">getDeletedRange()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public GetDeletedResult getDeletedRange(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                            @Placement(group = "Information") @FriendlyName("Start Time Reference") Calendar startTime,
                                            @Placement(group = "Information") @FriendlyName("End Time Reference") @Optional Calendar endTime) throws Exception {
        if (endTime == null) {
            Calendar serverTime = getConnection().getServerTimestamp().getTimestamp();
            endTime = (Calendar) serverTime.clone();
            if (endTime.getTimeInMillis() - startTime.getTimeInMillis() < 60000) {
                endTime.add(Calendar.MINUTE, 1);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting deleted " + type + " objects between " + startTime.getTime() + " and " + endTime.getTime());
        }
        return getConnection().getDeleted(type, startTime, endTime);
    }

    /**
     * Describes metadata (field list and object properties) for the specified object.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:describe-sobject}
     *
     * @param type Object. The specified value must be a valid object for your organization. For a complete list
     *             of objects, see Standard Objects
     * @return {@link com.sforce.soap.partner.DescribeSObjectResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_describesobject.htm">describeSObject()</a>
     * @since 4.0
     */
    @Processor(name = "describe-sobject", friendlyName = "Describe sObject")
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Describe Calls", description = "A set of calls to describe record structure in Salesforce.")
    public DescribeSObjectResult describeSObject(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type) throws Exception {
        return getConnection().describeSObject(type);
    }

    /**
     * Retrieves the list of individual records that have been deleted between the range of now to the duration before now.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-deleted}
     *
     * @param type     Object type. The specified value must be a valid object for your organization.
     * @param duration The amount of time in minutes before now for which to return records from.
     * @return {@link GetDeletedResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getdeleted.htm">getDeleted()</a>
     * @since 4.2
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public GetDeletedResult getDeleted(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                       @Placement(group = "Information") int duration) throws Exception {
        Calendar serverTime = getConnection().getServerTimestamp().getTimestamp();
        Calendar startTime = (Calendar) serverTime.clone();
        Calendar endTime = (Calendar) serverTime.clone();

        startTime.add(Calendar.MINUTE, -(duration));
        return getDeletedRange(type, startTime, endTime);
    }

    /**
     * Retrieves the list of individual records that have been updated between the range of now to the duration before now.
     * <p/>
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-updated}
     *
     * @param type     Object type. The specified value must be a valid object for your organization.
     * @param duration The amount of time in minutes before now for which to return records from.
     * @return {@link GetUpdatedResult} object containing an array of GetUpdatedResult objects containing the ID of each
     * created or updated object and the date/time (Coordinated Universal Time (UTC) time zone) on which it was created
     * or updated, respectively
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getupdated.htm">getUpdated()</a>
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Core Calls", description = "A set of calls that compromise the core of the API.")
    public GetUpdatedResult getUpdated(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                       @Placement(group = "Information") int duration) throws Exception {
        Calendar serverTime = getConnection().getServerTimestamp().getTimestamp();
        Calendar startTime = (Calendar) serverTime.clone();
        Calendar endTime = (Calendar) serverTime.clone();

        startTime.add(Calendar.MINUTE, -(duration));
        return getUpdatedRange(type, startTime, endTime);
    }

    /**
     * Retrieves the list of records that have been updated between the last time this method was called and now. This
     * method will save the timestamp of the latest date covered by Salesforce represented by {@link GetUpdatedResult#latestDateCovered}.
     * IMPORTANT: In order to use this method in a reliable way user must ensure that right after this method returns the result is
     * stored in a persistent way since the timestamp of the latest . In order to reset the latest update time
     * use {@link #resetUpdatedObjectsTimestamp(String) resetUpdatedObjectsTimestamp}
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-updated-objects}
     *
     * @param type              Object type. The specified value must be a valid object for your organization.
     * @param initialTimeWindow Time window (in minutes) used to calculate the start time (in time range) the first time
     *                          this operation is called. E.g: if initialTimeWindow equals 2, the start time will be
     *                          the current time (now) minus 2 minutes, then the range to retrieve the updated object will
     *                          be (now - 2 minutes; now). After first call the start time will be calculated from the
     *                          object store getting the last time this operation was exec
     * @param fields            The fields to retrieve for the updated objects
     * @return List with the updated objects in the calculated time range
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc This operation extends <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getupdated.htm">getUpdated()</a>
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Utility Calls", description = "API calls that your client applications can invoke to obtain the system timestamp, user information, and change user passwords.")
    public List<Map<String, Object>> getUpdatedObjects(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type,
                                                       @Placement(group = "Information") int initialTimeWindow,
                                                       @Optional @Default("#[payload]") @Placement(group = "Fields") List<String> fields) throws Exception {

        Calendar now = (Calendar) getConnection().getServerTimestamp().getTimestamp().clone();
        boolean initialTimeWindowUsed = false;
        ObjectStoreHelper objectStoreHelper = getObjectStoreHelper(getConnection().getConfig().getUsername());
        Calendar startTime = objectStoreHelper.getTimestamp(type);
        if (startTime == null) {
            startTime = (Calendar) now.clone();
            startTime.add(Calendar.MINUTE, -1 * initialTimeWindow);
            initialTimeWindowUsed = true;
        }

        GetUpdatedResult getUpdatedResult = getUpdatedRange(type, startTime, now);

        if (getUpdatedResult.getLatestDateCovered().equals(startTime)) {
            if (!initialTimeWindowUsed && getUpdatedResult.getIds().length > 0) {
                LOGGER.debug("Ignoring duplicated results from getUpdated() call");
                return Collections.emptyList();
            }
        }

        List<Map<String, Object>> updatedObjects = retrieve(type, Arrays.asList(getUpdatedResult.getIds()), fields);
        objectStoreHelper.updateTimestamp(getUpdatedResult, type);
        return updatedObjects;
    }

    /**
     * Resets the timestamp of the last updated object. After resetting this, a call to {@link this#getUpdatedObjects} will
     * use the initialTimeWindow to get the updated objects. If no timeObjectStore has been explicitly specified and {@link this#getUpdatedObjects}
     * has not been called then calling this method has no effect.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:reset-updated-objects-timestamp}
     *
     * @param type The object type for which the timestamp should be reset.
     * @throws org.mule.api.store.ObjectStoreException {@link com.sforce.ws.ConnectionException} when there is an error
     */
    @Processor
    @Category(name = "Utility Calls", description = "API calls that your client applications can invoke to obtain the system timestamp, user information, and change user passwords.")
    public void resetUpdatedObjectsTimestamp(@MetaDataKeyParam @Placement(group = "Information") @FriendlyName("sObject Type") String type) throws ObjectStoreException {
        if (timeObjectStore == null) {
            LOGGER.warn("Trying to reset updated objects timestamp but no object store has been set, was getUpdatedObjects ever executed?");
            return;
        }
        ObjectStoreHelper objectStoreHelper = getObjectStoreHelper(getConnection().getConfig().getUsername());
        objectStoreHelper.resetTimestamps(type);
    }

    /**
     * Change the password of a User or SelfServiceUser to a value that you specify.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:set-password}
     *
     * @param userId The user to set the password for.
     * @param newPassword The new password for the user.
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     *
     */
    @Processor
    @Category(name = "Utility Calls", description = "API calls that your client applications can invoke to obtain the system timestamp, user information, and change user passwords.")
    public void setPassword(@Placement(group = "Information") @FriendlyName("User ID") String userId, @Placement(group = "Information") @FriendlyName("Password") String newPassword) throws Exception {
        getConnection().setPassword(userId, newPassword);
    }

    
    /**
     * Creates a topic which represents a query that is the basis for notifying
     * listeners of changes to records in an organization.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:publish-topic}
     *
     * @param topicName   Descriptive name of the push topic, such as MyNewCases or TeamUpdatedContacts. The
     *                    maximum length is 25 characters. This value identifies the channel.
     * @param description Description of what kinds of records are returned by the query. Limit: 400 characters
     * @param query       The SOQL query statement that determines which records' changes trigger events to be sent to
     *                    the channel. Maximum length: 1200 characters
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_streaming/Content/pushtopic.htm">Push Topic</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Streaming API", description = "Create topics, to which applications can subscribe, receiving asynchronous notifications of changes to data in Salesforce, via the Bayeux protocol.")
    public void publishTopic(@Placement(group = "Information") String topicName,
                             @Placement(group = "Information") String query,
                             @Placement(group = "Information") @Optional String description) throws Exception {
        QueryResult result = getConnection().query("SELECT Id FROM PushTopic WHERE Name = '" + topicName + "'");
        if (result.getSize() == 0) {
            SObject pushTopic = new SObject();
            pushTopic.setType("PushTopic");
            pushTopic.setField("ApiVersion", "28.0");
            if (description != null) {
                pushTopic.setField("Description", description);
            }

            pushTopic.setField("Name", topicName);
            pushTopic.setField("Query", query);

            SaveResult[] saveResults = getConnection().create(new SObject[]{pushTopic});
            if (!saveResults[0].isSuccess()) {
                throw new SalesforceException(saveResults[0].getErrors()[0].getStatusCode(), saveResults[0].getErrors()[0].getMessage());
            }
        } else {
            SObject pushTopic = result.getRecords()[0];
            if (description != null) {
                pushTopic.setField("Description", description);
            }

            pushTopic.setField("Query", query);

            SaveResult[] saveResults = getConnection().update(new SObject[]{pushTopic});
            if (!saveResults[0].isSuccess()) {
                throw new SalesforceException(saveResults[0].getErrors()[0].getStatusCode(), saveResults[0].getErrors()[0].getMessage());
            }
        }
    }

    /**
     * Retrieves personal information for the user associated with the current session.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:get-user-info}
     *
     * @return {@link com.sforce.soap.partner.GetUserInfoResult}
     * @throws Exception {@link com.sforce.ws.ConnectionException} when there is an error
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_getuserinfo.htm">getUserInfo()</a>
     * @since 4.0
     */
    @Processor
    @OAuthProtected
    @InvalidateConnectionOn(exception = ConnectionException.class)
    @OAuthInvalidateAccessTokenOn(exception = ConnectionException.class)
    @Category(name = "Utility Calls", description = "API calls that your client applications can invoke to obtain the system timestamp, user information, and change user passwords.")
    public GetUserInfoResult getUserInfo() throws Exception {
        return getConnection().getUserInfo();
    }

    /**
     * Subscribe to a topic.
     * <p/>
     * {@sample.xml ../../../doc/mule-module-sfdc.xml.sample sfdc:subscribe-topic}
     *
     * @param topic    The name of the topic to subscribe to
     * @param callback The callback to be called when a message is received
     * @return {@link org.mule.api.callback.StopSourceCallback}
     * @api.doc <a href="http://www.salesforce.com/us/developer/docs/api_streaming/index_Left.htm">Streaming API</a>
     * @since 4.0
     */
    @Source(primaryNodeOnly = true, threadingModel = SourceThreadingModel.NONE)
    @OAuthProtected
    @Category(name = "Streaming API", description = "Create topics, to which applications can subscribe, receiving asynchronous notifications of changes to data in Salesforce, via the Bayeux protocol.")
    public StopSourceCallback subscribeTopic(final String topic, final SourceCallback callback) {
       final String topicName = "/topic" + topic;
       boolean subscribed = false;

       if (this.isReadyToSubscribe()) {
    	   this.subscribe(topicName, callback);
           subscribed = true;
       }

       subscriptions.add(new Subscription(topicName, callback, subscribed));

       return new StopSourceCallback() {
            @Override
            public void stop() throws Exception {
                getBayeuxClient().unsubscribe(topicName);
            }
        };
    }
    
    protected void processSubscriptions() {
    	boolean resubscribe = false;

        if (this.bc == null) {
            resubscribe = true;
        }

        for (Subscription p : subscriptions) {
            if (resubscribe || !p.isSubscribed()) {
    		    this.subscribe(p.getTopic(), p.getCallback());
            }
    	}
    }
    
    private void subscribe(String topicName, SourceCallback callback) {
    	this.getBayeuxClient().subscribe(topicName, new SalesforceBayeuxMessageListener(callback));
    }

    public void setObjectStoreManager(ObjectStoreManager objectStoreManager) {
        this.objectStoreManager = objectStoreManager;
    }

    public void setTimeObjectStore(ObjectStore<? extends Serializable> timeObjectStore) {
        this.timeObjectStore = timeObjectStore;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }


    protected com.sforce.async.SObject[] toAsyncSObjectList(List<Map<String, Object>> objects) {
        com.sforce.async.SObject[] sobjects = new com.sforce.async.SObject[objects.size()];
        int s = 0;
        for (Map<String, Object> map : objects) {
            sobjects[s] = toAsyncSObject(map);
            s++;
        }
        return sobjects;
    }

    protected SObject[] toSObjectList(String type, List<Map<String, Object>> objects) {
        SObject[] sobjects = new SObject[objects.size()];
        int s = 0;
        for (Map<String, Object> map : objects) {
            sobjects[s] = toSObject(type, map);
            s++;
        }
        return sobjects;
    }

    private BatchInfo createBatchAndCompleteRequest(JobInfo jobInfo, List<Map<String, Object>> objects) throws ConnectionException {
        try {
            BatchRequest batchRequest = getBulkConnection().createBatch(jobInfo);
            batchRequest.addSObjects(toAsyncSObjectList(objects));
            return batchRequest.completeRequest();
        } catch (AsyncApiException e) {
            if (e.getExceptionCode() == AsyncExceptionCode.InvalidSessionId) {
                throw new ConnectionException(e.getMessage(), e);
            }
        }

        return null;
    }

    private BatchInfo createBatchForQuery(JobInfo jobInfo, InputStream query) throws ConnectionException {
        try {
            return getBulkConnection().createBatchFromStream(jobInfo, query);
        } catch (AsyncApiException e) {
            if (e.getExceptionCode() == AsyncExceptionCode.InvalidSessionId) {
                throw new ConnectionException(e.getMessage(), e);
            }
        }
        return null;
    }

    private JobInfo createJobInfo(OperationEnum op, String type) throws AsyncApiException {
        return createJobInfo(op, type, null, null, null);
    }

    private JobInfo createJobInfo(OperationEnum op, String type, String externalIdFieldName, ContentType contentType, ConcurrencyMode concurrencyMode) throws AsyncApiException {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setOperation(op);
        jobInfo.setObject(type);
        if (externalIdFieldName != null) {
            jobInfo.setExternalIdFieldName(externalIdFieldName);
        }
        if (contentType != null) {
            jobInfo.setContentType(contentType);
        }
        if (concurrencyMode != null) {
            jobInfo.setConcurrencyMode(concurrencyMode);
        }
        return getBulkConnection().createJob(jobInfo);
    }

    private com.sforce.async.SObject toAsyncSObject(Map<String, Object> map) {
        com.sforce.async.SObject sObject = new com.sforce.async.SObject();
        for (String key : map.keySet()) {
            if (map.get(key) != null) {
                sObject.setField(key, map.get(key).toString());
            } else {
                sObject.setField(key, null);
            }
        }
        return sObject;
    }

    protected SObject toSObject(String type, Map<String, Object> map) {
        SObject sObject = new SObject();
        sObject.setType(type);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.equals("fieldsToNull")) {
            	sObject.setFieldsToNull((String[]) entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                sObject.setField(key, toSObject(key, toSObjectMap((Map) entry.getValue())));
            } else {
                sObject.setField(key, entry.getValue());
            }
        }
        return sObject;
    }

    /**
     * Enforce map keys are converted to String to comply with generic signature in toSObject
     *
     * @see #toSObject(String, java.util.Map)
     */
    protected Map<String, Object> toSObjectMap(Map map) {
        Map<String, Object> sObjectMap = new HashMap<String, Object>();
        for(Object key : map.keySet()) {
            sObjectMap.put(key.toString(), map.get(key));
        }
        return sObjectMap;
    }

    private synchronized ObjectStoreHelper getObjectStoreHelper(String username) {
        if (objectStoreHelper == null) {
            if (timeObjectStore == null) {
                timeObjectStore = registry.lookupObject(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME);
                if (timeObjectStore == null) {
                    timeObjectStore = objectStoreManager.getObjectStore(username, true);
                }
                if (timeObjectStore == null) {
                    throw new IllegalArgumentException("Unable to acquire an object store.");
                }
            }
            objectStoreHelper = new ObjectStoreHelper(username, timeObjectStore);
        }
        return objectStoreHelper;
    }

    protected void setConnectionOptions(PartnerConnection connection) {
        //call options
        String clientId = getClientId();
        if (clientId != null) {
            CallOptions_element callOptions = new CallOptions_element();
            callOptions.setClient(clientId);
            connection.__setCallOptions(callOptions);
        }

        //assignment rule
        String assignmentRuleId = getAssignmentRuleId();
        Boolean useDefaultRule = getUseDefaultRule();
        if (assignmentRuleId != null || useDefaultRule != null) {
            AssignmentRuleHeader_element assignmentRule = new AssignmentRuleHeader_element();
            if (assignmentRuleId != null) {
                assignmentRule.setAssignmentRuleId(assignmentRuleId);
            }
            if (useDefaultRule != null) {
                assignmentRule.setUseDefaultRule(useDefaultRule);
            }
            connection.__setAssignmentRuleHeader(assignmentRule);
        }

        //allow field truncation
        Boolean allowFieldTruncationSupport = getAllowFieldTruncationSupport();
        if (allowFieldTruncationSupport != null) {
            connection.setAllowFieldTruncationHeader(allowFieldTruncationSupport);
        }
    }

    public ObjectStore<? extends Serializable> getTimeObjectStore() {
        return timeObjectStore;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAssignmentRuleId() {
        return assignmentRuleId;
    }

    public void setAssignmentRuleId(String assignmentRuleId) {
        this.assignmentRuleId = assignmentRuleId;
    }

    public Boolean getUseDefaultRule() {
        return useDefaultRule;
    }

    public void setUseDefaultRule(Boolean useDefaultRule) {
        this.useDefaultRule = useDefaultRule;
    }

    public Boolean getAllowFieldTruncationSupport() {
        return allowFieldTruncationSupport;
    }

    public void setAllowFieldTruncationSupport(Boolean allowFieldTruncationSupport) {
        this.allowFieldTruncationSupport = allowFieldTruncationSupport;
    }
    
    @Override
    public void setMuleContext(MuleContext context) {
        setObjectStoreManager(((ObjectStoreManager) context.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)));
        setRegistry((Registry) context.getRegistry());
    }
}
