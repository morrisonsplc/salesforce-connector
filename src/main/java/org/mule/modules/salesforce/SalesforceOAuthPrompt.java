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
