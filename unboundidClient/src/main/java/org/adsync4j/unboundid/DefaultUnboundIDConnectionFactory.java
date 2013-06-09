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

import com.google.common.base.Preconditions;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import org.adsync4j.LdapClientException;
import org.adsync4j.LdapConnectionDetails;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Default connection factory that creates an unsecured connection with the given user and password.
 */
@NotThreadSafe
public class DefaultUnboundIDConnectionFactory implements PagingUnboundIDConnectionFactory {

    @Nullable
    private final LDAPConnectionOptions _ldapConnectionOptions;
    private final LdapConnectionDetails _ldapConnectionDetails;

    private PagingLdapConnection _connection;

    public DefaultUnboundIDConnectionFactory(LdapConnectionDetails _ldapConnectionDetails) {
        this(_ldapConnectionDetails, null);
    }

    public DefaultUnboundIDConnectionFactory(
            LdapConnectionDetails ldapConnectionDetails, @Nullable LDAPConnectionOptions ldapConnectionOptions)
    {
        _ldapConnectionDetails = ldapConnectionDetails;
        _ldapConnectionOptions = ldapConnectionOptions;
    }

    public PagingLdapConnection getConnection() throws LdapClientException {
        if (_connection == null) {
            _connection = createConnection();
        }
        return _connection;
    }

    private PagingLdapConnection createConnection() {
        try {

            Preconditions.checkArgument("ldap".equals(_ldapConnectionDetails.getProtocol()),
                    "This connection factory supports only the creation of unsecured ldap:// connections.");
            LDAPConnection connection = new LDAPConnection(_ldapConnectionOptions,
                    _ldapConnectionDetails.getHost(), _ldapConnectionDetails.getPort(),
                    _ldapConnectionDetails.getBindUser(), _ldapConnectionDetails.getBindPassword());
            return PagingLdapConnectionImpl.wrap(connection);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }
}
