package org.adsync4j.api;

/**
 * Abstract class that is the base of more specific exceptions indicating error conditions during incremental synchronization.
 */
public abstract class FullSyncRequiredException extends LdapClientException {

    protected FullSyncRequiredException() {
    }

    protected FullSyncRequiredException(String message) {
        super(message);
    }
}
