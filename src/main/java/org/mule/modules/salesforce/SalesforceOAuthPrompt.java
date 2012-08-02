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

/**
 * Specifies how the authorization server prompts the user for reauthentication and reapproval
 */
public enum SalesforceOAuthPrompt {
    /**
     * The authorization server must prompt the user for reauthentication, forcing the user to log in again.
     */
    LOGIN,

    /**
     * The authorization server must prompt the user for reapproval before returning information to the client.
     * It is valid to pass both values, separated by a space, to require the user to both log in and reauthorize.
     */
    CONSENT
}
