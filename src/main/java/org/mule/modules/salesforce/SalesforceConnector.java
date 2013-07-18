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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.*;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.MessageHandler;
import com.sforce.ws.SessionRenewer;
import org.mule.common.metadata.*;
import org.mule.common.metadata.datatype.DataType;


/**
 * The Salesforce Connector will allow to connect to the Salesforce application using regular username and password via
 * the SOAP API. Almost every operation that can be done via the Salesforce's API can be done thru this connector. This
 * connector will also work if your Salesforce objects are customized with additional fields or even you are working
 * with custom objects.
 * <p/>
 * Integrating with Salesforce consists of web service calls utilizing XML request/response setup
 * over an HTTPS connection. The technical details of this connection such as request headers,
 * error handling, HTTPS connection, etc. are all abstracted from the user to make implementation
 * quick and easy.
 * <p/>
 * {@sample.config ../../../doc/mule-module-sfdc.xml.sample sfdc:config}
 *
 * @author MuleSoft, Inc.
 */

@org.mule.api.annotations.Connector(name = "sfdc", schemaVersion = "5.0", friendlyName = "Salesforce", minMuleVersion = "3.4")
public class SalesforceConnector extends BaseSalesforceConnector {
    private static final Logger LOGGER = Logger.getLogger(SalesforceConnector.class);

    @MetaDataKeyRetriever
    public List<MetaDataKey> getMetaDataKeys() throws Exception {

        List<MetaDataKey> keys = new ArrayList<MetaDataKey>();
        DescribeGlobalResult describeGlobal = describeGlobal();


        if (describeGlobal != null) {
            DescribeGlobalSObjectResult[] sobjects = describeGlobal.getSobjects();
            for (DescribeGlobalSObjectResult sobject : sobjects) {
                keys.add(new DefaultMetaDataKey(sobject.getName(), sobject.getLabel()));
            }
        }


        return keys;
    }

    @MetaDataRetriever
    public MetaData getMetaData(MetaDataKey key) throws Exception {
        DescribeSObjectResult describeSObject = describeSObject(key.getId());

        MetaData metaData = null;
        if (describeSObject != null) {
            Field[] fields = describeSObject.getFields();
            Map<String, MetaDataModel> map = new HashMap<String, MetaDataModel>(fields.length);
            for (Field f : fields) {
                MetaDataModel fieldModel = getModelForField(f);
                map.put(f.getName(), fieldModel);
            }

            MetaDataModel model = new DefaultDefinedMapMetaDataModel(map, key.getId());
            metaData = new DefaultMetaData(model);
        }
        return metaData;
    }

    private MetaDataModel getModelForField(Field f) {
        DataType dataType = getDataType(f.getType());
        if (DataType.POJO.equals(dataType)) {
            return new DefaultPojoMetaDataModel(f.getClass());
        } else {
            return new DefaultSimpleMetaDataModel(dataType);
        }
    }

    private DataType getDataType(FieldType fieldType) {
        DataType dt;
        switch (fieldType) {
            case _boolean:
                dt = DataType.BOOLEAN;
                break;
            case _double:
                dt = DataType.NUMBER;
                break;
            case _int:
                dt = DataType.NUMBER;
                break;
            case anyType:
                dt = DataType.POJO;
                break;
            case base64:
                dt = DataType.STRING;
                break;
            case combobox:
                dt = DataType.ENUM;
                break;
            case currency:
                dt = DataType.STRING;
                break;
            case datacategorygroupreference:
                dt = DataType.STRING;
                break;
            case date:
                dt = DataType.DATE_TIME;
                break;
            case datetime:
                dt = DataType.DATE_TIME;
                break;
            case email:
                dt = DataType.STRING;
                break;
            case encryptedstring:
                dt = DataType.STRING;
                break;
            case id:
                dt = DataType.STRING;
                break;
            case multipicklist:
                dt = DataType.ENUM;
                break;
            case percent:
                dt = DataType.STRING;
                break;
            case phone:
                dt = DataType.STRING;
                break;
            case picklist:
                dt = DataType.ENUM;
                break;
            case reference:
                dt = DataType.STRING;
                break;
            case string:
                dt = DataType.STRING;
                break;
            case textarea:
                dt = DataType.STRING;
                break;
            case time:
                dt = DataType.DATE_TIME;
                break;
            case url:
                dt = DataType.STRING;
                break;
            default:
                dt = DataType.STRING;
        }
        return dt;
    }


    /**
     * Partner connection
     */
    private PartnerConnection connection;

    /**
     * REST connection to the bulk API
     */
    private BulkConnection bulkConnection;

    /**
     * Login result
     */
    private LoginResult loginResult;

    protected void setConnection(PartnerConnection connection) {
        this.connection = connection;
    }

    protected void setBulkConnection(BulkConnection bulkConnection) {
        this.bulkConnection = bulkConnection;
    }

    protected void setLoginResult(LoginResult loginResult) {
        this.loginResult = loginResult;
    }

    protected LoginResult getLoginResult() {
        return loginResult;
    }

    @ValidateConnection
    public boolean isConnected() {
        if (bulkConnection != null) {
            if (connection != null) {
                if (loginResult != null) {
                    if (loginResult.getSessionId() != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the session id for the current connection
     *
     * @return the session id for the current connection
     */
    @ConnectionIdentifier
    public String getSessionId() {
        if (connection != null) {
            if (loginResult != null) {
                return loginResult.getSessionId();
            }
        }

        return null;
    }


    /**
     * End the current session
     *
     * @throws Exception
     */
    @Disconnect
    public synchronized void destroySession() {
        if (isInitializedBayeuxClient()) {
            if (getBayeuxClient().isConnected()) {
                getBayeuxClient().disconnect();
            }
        }

        if (connection != null && loginResult != null) {
            try {
                connection.logout();
            } catch (ConnectionException ce) {
                LOGGER.error(ce);
            } finally {
                loginResult = null;
                connection = null;
                setBayeuxClient(null);
            }
        }
    }
    
    @Override
    protected boolean isReadyToSubscribe() {
    	return this.isConnected();
    }

    /**
     * Creates a new Salesforce session
     *
     * @param username      Username used to initialize the session
     * @param password      Password used to authenticate the user
     * @param securityToken User's security token
     * @param url           Salesforce endpoint URL
     * @param proxyHost     Hostname of the proxy
     * @param proxyPort     Port of the proxy
     * @param proxyUsername Username used to authenticate against the proxy
     * @param proxyPassword Password used to authenticate against the proxy
     * @param sessionId  This value could be used for specifing an active Salesforce session.
     * Please take into account you must specify all the connection parameters anyway since they will be used
     * in case of needing a reconnection.
     * @param serviceEndpoint Specifies the service endpoint. This value will only be used in case of using sessionId configuration property.
     * Otherwise the service endpoint will be retrieved from the login results.
     * @throws ConnectionException if a problem occurred while trying to create the session
     */
    @Connect
    public synchronized void connect(@ConnectionKey String username,
                                     @Password String password,
                                     String securityToken,
                                     @Optional @Default("https://login.salesforce.com/services/Soap/u/28.0") String url,
                                     @Optional @Placement(group = "Proxy Settings") String proxyHost,
                                     @Optional @Placement(group = "Proxy Settings") @Default("80") int proxyPort,
                                     @Optional @Placement(group = "Proxy Settings") String proxyUsername,
                                     @Optional @Placement(group = "Proxy Settings") @Password String proxyPassword,
                                     @Optional @Placement(group = "Session") String sessionId,
                                     @Optional @Placement(group = "Session") String serviceEndpoint) throws org.mule.api.ConnectionException {

        ConnectorConfig connectorConfig = createConnectorConfig(url, username, password + securityToken, proxyHost, proxyPort, proxyUsername, proxyPassword);
        if (LOGGER.isDebugEnabled()) {
            connectorConfig.addMessageHandler(new MessageHandler() {
                @Override
                public void handleRequest(URL endpoint, byte[] request) {
                    LOGGER.debug("Sending request to " + endpoint.toString());
                    LOGGER.debug(new String(request));
                }

                @Override
                public void handleResponse(URL endpoint, byte[] response) {
                    LOGGER.debug("Receiving response from " + endpoint.toString());
                    LOGGER.debug(new String(response));
                }
            });
        }

        try {
            connection = Connector.newConnection(connectorConfig);
            setConnectionOptions(connection);
        } catch (ConnectionException e) {
            throw new org.mule.api.ConnectionException(ConnectionExceptionCode.UNKNOWN, null, e.getMessage(), e);
        }

        if (sessionId != null && serviceEndpoint != null) {
            connection.getSessionHeader().setSessionId(sessionId);
            connection.getConfig().setSessionId(sessionId);
            connection.getConfig().setServiceEndpoint(serviceEndpoint);
        }
        else {
            reconnect();
        }

        try {
            String restEndpoint = "https://" + (new URL(connectorConfig.getServiceEndpoint())).getHost() + "/services/async/28.0";
            connectorConfig.setRestEndpoint(restEndpoint);
            bulkConnection = new BulkConnection(connectorConfig);
        } catch (AsyncApiException e) {
            throw new org.mule.api.ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getExceptionCode().toString(), e.getMessage(), e);
        } catch (MalformedURLException e) {
            throw new org.mule.api.ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, null, e.getMessage(), e);
        }
        
        this.processSubscriptions();
    }

    public void reconnect() throws org.mule.api.ConnectionException {
        try {
            LOGGER.debug("Creating a Salesforce session using " + connection.getConfig().getUsername());
            loginResult = connection.login(connection.getConfig().getUsername(), connection.getConfig().getPassword());

            if (loginResult.isPasswordExpired()) {
                try {
                    connection.logout();
                } catch (ConnectionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                String username = connection.getConfig().getUsername();
                connection = null;
                throw new org.mule.api.ConnectionException(ConnectionExceptionCode.CREDENTIALS_EXPIRED, null, "The password for the user " + username + " has expired");
            }

            LOGGER.debug("Session established successfully with ID " + loginResult.getSessionId() + " at instance " + loginResult.getServerUrl());
            connection.getSessionHeader().setSessionId(loginResult.getSessionId());
            connection.getConfig().setServiceEndpoint(loginResult.getServerUrl());
            connection.getConfig().setSessionId(loginResult.getSessionId());
        } catch (ConnectionException e) {
            if (e instanceof ApiFault) {
                throw new org.mule.api.ConnectionException(ConnectionExceptionCode.UNKNOWN, ((ApiFault) e).getExceptionCode().name(), ((ApiFault) e).getExceptionMessage(), e);
            } else {
                throw new org.mule.api.ConnectionException(ConnectionExceptionCode.UNKNOWN, null, e.getMessage(), e);
            }
        }
    }

    /**
     * Create connector config
     *
     * @param endpoint      Salesforce endpoint
     * @param username      Username to use for authentication
     * @param password      Password to use for authentication
     * @param proxyHost
     * @param proxyPort
     * @param proxyUsername
     * @param proxyPassword
     * @return
     */
    protected ConnectorConfig createConnectorConfig(String endpoint, String username, String password, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        ConnectorConfig config = new ConnectorConfig();
        config.setUsername(username);
        config.setPassword(password);

        config.setAuthEndpoint(endpoint);
        config.setServiceEndpoint(endpoint);

        config.setManualLogin(true);

        config.setCompression(false);

        if (proxyHost != null) {
            config.setProxy(proxyHost, proxyPort);
            if (proxyUsername != null) {
                config.setProxyUsername(proxyUsername);
            }
            if (proxyPassword != null) {
                config.setProxyPassword(proxyPassword);
            }
        }

        SessionRenewer sessionRenewer = new SessionRenewer() {
            public SessionRenewalHeader renewSession(ConnectorConfig config) throws ConnectionException {

                try {
                    reconnect();
                } catch (org.mule.api.ConnectionException e) {
                    throw new ConnectionException(e.getMessage(), e);
                }

                SessionRenewer.SessionRenewalHeader sessionRenewalHeader = new SessionRenewer.SessionRenewalHeader();
                sessionRenewalHeader.name = new javax.xml.namespace.QName("urn:partner.soap.sforce.com", "SessionHeader");
                sessionRenewalHeader.headerElement = connection.getSessionHeader();

                return sessionRenewalHeader;
            }
        };

        config.setSessionRenewer(sessionRenewer);

        return config;
    }

    @Override
    protected PartnerConnection getConnection() {
        return connection;
    }

    @Override
    protected BulkConnection getBulkConnection() {
        return bulkConnection;
    }
}
