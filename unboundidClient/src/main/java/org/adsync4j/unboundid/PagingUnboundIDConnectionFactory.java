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

import org.adsync4j.api.LdapClientException;

/**
 * Interface for factories capable of managing a {@link PagingLdapConnection}.
 */
public interface PagingUnboundIDConnectionFactory {

    PagingLdapConnection createConnection() throws LdapClientException;

    /**
     * Ensures that the provided connection is open (reconnects in case it's not).
     *
     * @param connection The connection to examine.
     */
    PagingLdapConnection ensureConnection(PagingLdapConnection connection) throws LdapClientException;

    /**
     * Closes the physical connection to the LDAP server.
     *
     * @param connection The connection to close.
     */
    void closeConnection(PagingLdapConnection connection);
}
