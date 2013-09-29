/*******************************************************************************
 * ADSync4J (https://github.com/zagyi/adsync4j)
 *
 * Copyright (c) 2013 Balazs Zagyvai
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Balazs Zagyvai
 ******************************************************************************/
package org.adsync4j.api;

import java.util.UUID;

import static java.lang.String.format;

/**
 * Exception that might be thrown on incremental synchronization in case the database identifier of the domain controller
 * (called Invocation ID) does not match the ID stored in the {@link
 * org.adsync4j.spi.DomainControllerAffiliation#getInvocationId() affiliation record}.
 * <p/>
 * This happens when the domain controller's database is restored after a failure, which invalidates the highest committed USN
 * stored in the {@link org.adsync4j.spi.DomainControllerAffiliation#getHighestCommittedUSN() affiliation record}. Clients will
 * have to perform a full re-synchronization in response to this exception.
 */
public class InvocationIdMismatchException extends FullSyncRequiredException {

    public InvocationIdMismatchException(UUID expectedInvocationId, UUID actualInvocationId) {
        super(format(
                "Expected Invocation ID is %s, but retrieved ID is %s",
                expectedInvocationId.toString(), actualInvocationId.toString()));
    }
}
