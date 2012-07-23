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
 * Avoid interacting with the user.
 */
public enum SalesforceOAuthImmediate {
    /**
     * Prompt the user for login and approval
     */
    TRUE,

    /**
     * If the user is currently logged in and has previously approved the client_id, the approval step is skipped,
     * and the browser is immediately redirected to the callback with an authorization code. If the user is not
     * logged in or has not previously approved the client, the flow immediately terminates with
     * the immediate_unsuccessful error code.
     */
    FALSE;
}
