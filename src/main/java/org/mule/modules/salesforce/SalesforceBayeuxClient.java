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

import org.apache.log4j.Logger;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;

import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>{@link SalesforceBayeuxClient} is an extension of a {@link BayeuxClient} that can deal with Salesforce session
 * management.
 */
public class SalesforceBayeuxClient extends BayeuxClient {
    protected static final int HANDSHAKE_TIMEOUT = 30 * 1000;
    protected static final int LONG_POLLING_TIMEOUT = 120000;
    protected static final Map<String, Object> LONG_POLLING_OPTIONS = createLongPollingOptions();
    protected static final Logger LOGGER = Logger.getLogger(SalesforceBayeuxClient.class);
    protected static final String LOGIN_COOKIE = "login";
    protected static final String LOCALEINFO_COOKIE = "com.salesforce.LocaleInfo";
    protected static final String SESSIONID_COOKIE = "sid";
    protected static final String LANGUAGE_COOKIE = "language";
    protected Map<String, org.cometd.bayeux.client.ClientSessionChannel.MessageListener> subscriptions;
    protected BaseSalesforceConnector salesforceConnector;

    private static Map<String, Object> createLongPollingOptions() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientTransport.TIMEOUT_OPTION, LONG_POLLING_TIMEOUT);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Create a new instance of this Bayeux client.
     *
     * @param salesforceConnector Salesforce connection
     */
    public SalesforceBayeuxClient(BaseSalesforceConnector salesforceConnector) throws MalformedURLException {
        super("https://" + (new URL(salesforceConnector.getConnection().getConfig().getServiceEndpoint())).getHost() + "/cometd/26.0",
                SalesforceLongPollingTransport.create(salesforceConnector, LONG_POLLING_OPTIONS));

        this.salesforceConnector = salesforceConnector;
        this.subscriptions = Collections.synchronizedMap(new HashMap<String, ClientSessionChannel.MessageListener>());
        setCookies();

        getChannel(Channel.META_CONNECT).addListener(new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (message.isSuccessful() && subscriptions.size() > 0) {
                    for (String subscriptionChannel : subscriptions.keySet()) {
                        LOGGER.info("Subscribing to channel: " + subscriptionChannel);
                        getChannel(subscriptionChannel).subscribe(subscriptions.get(subscriptionChannel));
                    }
                    // Removing the subscriptions already made so it doesn't re-subscribe on reconnect
                    subscriptions.clear();
                }
            }
        });
    }

    private void setCookies() {
        setCookie(LOCALEINFO_COOKIE, "us");
        setCookie(LOGIN_COOKIE, salesforceConnector.getConnection().getConfig().getUsername());
        setCookie(SESSIONID_COOKIE, salesforceConnector.getSessionId());
        setCookie(LANGUAGE_COOKIE, "en_US");
    }

    /**
     * <p>Callback method invoked when the given messages have failed to be sent.</p>
     * <p>The default implementation logs the failure at INFO level.</p>
     *
     * @param x        the exception that caused the failure
     * @param messages the messages being sent
     */
    @Override
    public void onFailure(Throwable x, Message[] messages) {
        if (x instanceof ProtocolException) {
            try {
                // EL: not sure this is the best way of doing this.
                // Ideally it should be the same for OAuth and non-OAuth
                // ways of reconnecting.
                if( salesforceConnector instanceof SalesforceConnector ) {
                    ((SalesforceConnector)salesforceConnector).reconnect();
                }
                setCookies();
                handshake();
            } catch (org.mule.api.ConnectionException e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.error(x.getMessage());
        }
    }

    @Override
    public void handshake() {
        super.handshake(HANDSHAKE_TIMEOUT);
    }

    public void unsubscribe(String channel) {
        getChannel(channel).unsubscribe();

        this.subscriptions.remove(channel);
    }

    public void subscribe(String channel, ClientSessionChannel.MessageListener messageListener) {
        if (isConnected()) {
            LOGGER.info("Subscribing to channel: " + channel);
            getChannel(channel).subscribe(messageListener);
        } else {
            this.subscriptions.put(channel, messageListener);
        }
    }
}
