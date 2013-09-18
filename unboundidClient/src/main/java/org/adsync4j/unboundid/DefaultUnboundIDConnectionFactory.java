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

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import org.adsync4j.LdapClientException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default connection factory that creates an unsecured connection with the given user and password.
 */
@NotThreadSafe
public class DefaultUnboundIDConnectionFactory implements PagingUnboundIDConnectionFactory {

    private final String _protocol;
    private final String _host;
    private final int _port;
    private final String _bindUser;
    private final String _bindPassword;

    @Nullable
    private final LDAPConnectionOptions _ldapConnectionOptions;

    private PagingLdapConnection _connection;

    public DefaultUnboundIDConnectionFactory(String protocol, String host, int port, String bindUser, String bindPassword) {
        this(protocol, host, port, bindUser, bindPassword, null);
    }

    public DefaultUnboundIDConnectionFactory(
            String protocol, String host, int port, String bindUser, String bindPassword,
            LDAPConnectionOptions connectionOptions)
    {
        _protocol = protocol;
        _host = host;
        _port = port;
        _bindUser = bindUser;
        _bindPassword = bindPassword;
        _ldapConnectionOptions = connectionOptions;
    }

    @Override
    public PagingLdapConnection getConnection() throws LdapClientException {
        if (_connection == null) {
            _connection = createConnection();
        }
        return _connection;
    }

    private PagingLdapConnection createConnection() {
        try {
            checkArgument("ldap".equals(_protocol),
                    "This connection factory supports only the creation of unsecured ldap:// connections.");
            LDAPConnection connection = new LDAPConnection(_ldapConnectionOptions, _host, _port, _bindUser, _bindPassword);
            return PagingLdapConnectionImpl.wrap(connection);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }
}
