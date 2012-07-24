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

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.MessageHandler;
import org.apache.log4j.Logger;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAuthorizationParameter;
import org.mule.api.annotations.oauth.OAuthCallbackParameter;
import org.mule.api.annotations.oauth.OAuthConsumerKey;
import org.mule.api.annotations.oauth.OAuthConsumerSecret;
import org.mule.api.annotations.oauth.OAuthPostAuthorization;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The Salesforce Connector will allow to connect to the Salesforce application using OAuth as the authentication
 * mechanism. Almost every operation that can be done via the Salesforce's API can be done thru this connector.
 * This connector will also work if your Salesforce objects are customized with additional fields or even you are
 * working with custom objects.
 * <p/>
 * Integrating with Salesforce consists of web service calls utilizing XML request/response setup
 * over an HTTPS connection. The technical details of this connection such as request headers,
 * error handling, HTTPS connection, etc. are all abstracted from the user to make implementation
 * quick and easy.
 * <p/>
 * This version of the connector allows you to use OAuth for authentication instead of the
 * username/password/securityToken combination.
 * <p/>
 * {@sample.config ../../../doc/mule-module-sfdc.xml.sample sfdc:config-with-oauth}
 *
 * @author MuleSoft, Inc.
 */
@org.mule.api.annotations.Connector(name = "sfdc",
        schemaVersion = "5.0",
        friendlyName = "Salesforce",
        minMuleVersion = "3.3",
        configElementName = "config-with-oauth")
@OAuth2(authorizationUrl = "https://login.salesforce.com/services/oauth2/authorize",
        accessTokenUrl = "https://login.salesforce.com/services/oauth2/token",
        authorizationParameters = {
                @OAuthAuthorizationParameter(name = "display", type = SalesforceOAuthDisplay.class),
                @OAuthAuthorizationParameter(name = "immediate", type = SalesforceOAuthImmediate.class,
                        optional = true, defaultValue = "FALSE")
        })
public class SalesforceOAuthConnector extends BaseSalesforceConnector {
    private static final Logger LOGGER = Logger.getLogger(SalesforceOAuthConnector.class);

    private PartnerConnection partnerConnection;

    /**
     * REST connection to the bulk API
     */
    private BulkConnection bulkConnection;

    /**
     * Your application's client identifier (consumer key in Remote Access Detail).
     */
    @Configurable
    @OAuthConsumerKey
    private String consumerKey;

    /**
     * Your application's client secret (consumer secret in Remote Access Detail).
     */
    @Configurable
    @OAuthConsumerSecret
    private String consumerSecret;

    @OAuthAccessToken
    private String accessToken;

    @OAuthCallbackParameter(expression = "#[json:instance_url]")
    private String instanceId;

    @OAuthPostAuthorization
    public void postAuthorize() throws ConnectionException, MalformedURLException, AsyncApiException {
        ConnectorConfig config = new ConnectorConfig();
        config.addMessageHandler(new MessageHandler() {
            @Override
            public void handleRequest(URL endpoint, byte[] request) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sending request to " + endpoint.toString());
                    LOGGER.debug(new String(request));
                }
            }

            @Override
            public void handleResponse(URL endpoint, byte[] response) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Receiving response from " + endpoint.toString());
                    LOGGER.debug(new String(response));
                }
            }
        });

        config.setSessionId(accessToken);
        config.setManualLogin(true);

        config.setCompression(false);

        this.partnerConnection = Connector.newConnection(config);

        String restEndpoint = "https://" + (new URL(instanceId)).getHost() + "/services/async/23.0";
        config.setRestEndpoint(restEndpoint);

        this.bulkConnection = new BulkConnection(config);
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected PartnerConnection getConnection() {
        return this.partnerConnection;
    }

    @Override
    protected BulkConnection getBulkConnection() {
        return this.bulkConnection;
    }

    @Override
    protected String getSessionId() {
        return this.accessToken;
    }
}
