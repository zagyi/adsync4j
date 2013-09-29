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

/**
 * Interface exposing methods of {@link com.unboundid.ldap.sdk.LDAPConnection LDAPConnection} that {@link UnboundIDLdapClient}
 * needs, but that are not exposed by {@link LDAPInterface}.
 */
public interface UnboundIDLdapConnection extends LDAPInterface {

    /**
     * Unbinds from the server and closes the connection to the LDAP server.
     */
    void close();

    /**
     * Indicates whether this connection is currently established.
     *
     * @return {@code true} if this connection is currently established, or
     *         {@code false} if it is not.
     */
    boolean isConnected();

    /**
     * Attempts to re-establish a connection to the server and re-authenticate if appropriate.
     *
     * @throws LDAPException If a problem occurs while attempting to re-connect or re-authenticate.
     */
    void reconnect() throws LDAPException;
}
