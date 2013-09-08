package org.adsync4j;

public abstract class FullSyncRequiredException extends RuntimeException {
    protected FullSyncRequiredException() {
    }

    protected FullSyncRequiredException(String message) {
        super(message);
    }
}
