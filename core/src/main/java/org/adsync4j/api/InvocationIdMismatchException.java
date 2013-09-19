package org.adsync4j.api;

import java.util.UUID;

import static java.lang.String.format;

/**
 * Exception thrown when an incremental synchronization fails, because the database identifier of Active Directory (called
 * Invocation ID) does not match the ID stored in the {@link org.adsync4j.spi.DomainControllerAffiliation#getInvocationId() affiliation record}.
 * This happens when the database on the server side is restored from a backup.<br>
 * Since the highest committed USN stored in the {@link org.adsync4j.spi.DomainControllerAffiliation#getHighestCommittedUSN()
 * affiliation record} gets obsolete after a server-side database restore, a full synchronization will have to be performed,
 * in order to prevent inconsistencies.
 */
public class InvocationIdMismatchException extends FullSyncRequiredException {
    public InvocationIdMismatchException(UUID expectedInvocationId, UUID actualInvocationId) {
        super(format(
                "Expected Invocation ID is %s, but retrieved ID is %s",
                expectedInvocationId.toString(), actualInvocationId.toString()));
    }
}
