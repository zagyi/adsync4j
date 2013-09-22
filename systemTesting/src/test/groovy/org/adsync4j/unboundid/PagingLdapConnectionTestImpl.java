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
 ***************************************************************************** */
package org.adsync4j.unboundid;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Implementation of {@link org.adsync4j.unboundid.PagingLdapConnection} that is only meant to be used by integration tests
 * executed against an embedded LDAP server. The necessity of this special test class stems from a bug in the UnboundID LDAP
 * SDK. The original implementation of {@link com.unboundid.ldap.sdk.LDAPConnection#reconnect()} will only succeed if there has
 * been no other reconnect request in the lase second, otherwise it throws an exception. I guess that's done to prevent
 * thrashing, but the logic is flawed, because it doesn't take an explicit close() request (between the two reconnect() within
 * 1 second) into consideration. Since integration tests do exactly that (close and re-open the LDAP connection in quick
 * succession) in order to execute a series of LDAP synchronization operations, we need some workaround. This class overrides
 * close() and reconnect() with an empty body to completely remove the problem. The other option would be to wait 1 sec in
 * reconnect(), but that would defeat the purpose of integration tests (which must mirror the production environment as much as
 * possible, but must be _quick_ in the first place, so that we can execute them frequently during development).
 */
@ThreadSafe
public class PagingLdapConnectionTestImpl extends PagingLdapConnectionImpl {

    public PagingLdapConnectionTestImpl(LDAPInterface delegateConnection) {
        super(delegateConnection);
    }

    /**
     * Connections won't be closed during integration tests.
     */
    @Override
    public void close() {
    }

    /**
     * As connections won't be closed during integration tests there is no need to reopen them...
     */
    @Override
    public void reconnect() throws LDAPException {
    }

    /**
     * Always true, by definition. :)
     */
    @Override
    public boolean isConnected() {
        return true;
    }
}
