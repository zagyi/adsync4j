package org.adsync4j.api;

/**
 * Exception thrown on an incremental synchronization request if the {@link org.adsync4j.spi.DomainControllerAffiliation affiliation record}
 * does not contain enough information to carry out that operation (both an {@link org.adsync4j.spi.DomainControllerAffiliation#getInvocationId()
 * invocationId} and the {@link org.adsync4j.spi.DomainControllerAffiliation#getHighestCommittedUSN() highest committed USN} is required).
 * In the absence of these properties, an initial full synchronization will have to be performed first.
 */
public class InitialFullSyncRequiredException extends FullSyncRequiredException {
}
