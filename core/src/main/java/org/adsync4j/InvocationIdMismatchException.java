package org.adsync4j;

import java.util.UUID;

import static java.lang.String.format;

public class InvocationIdMismatchException extends FullSyncRequiredException {
    public InvocationIdMismatchException(UUID expectedInvocationId, UUID actualInvocationId) {
        super(format(
                "Expected Invocation ID is %s, but retrieved ID is %s",
                expectedInvocationId.toString(), actualInvocationId.toString()));
    }
}
