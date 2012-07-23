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
 * Tailors the login page to the user's device type.
 */
public enum SalesforceOAuthDisplay {
    /**
     * Full-page authorization screen (default)
     */
    PAGE,

    /**
     * Compact dialog optimized for modern web browser popup windows.
     */
    POPUP,

    /**
     * Mobile-optimized dialog designed for modern smartphones, such as Android and iPhone.
     */
    TOUCH
}
