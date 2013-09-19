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
import com.unboundid.ldap.sdk.LDAPInterface;
import org.adsync4j.api.LdapClientException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A simple implementation of {@link PagingUnboundIDConnectionFactory} that creates an unsecured connection with the provided
 * parameters.
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

    /**
     * Creates an {@link LDAPConnection} based on the information passed at creation time, and wraps it in a
     * {@link PagingLdapConnectionImpl} that implements the paging search operation.
     */
    @Override
    public PagingLdapConnection createConnection() throws LdapClientException {
        try {
            checkArgument("ldap".equals(_protocol),
                    "This connection factory supports only the creation of unsecured ldap:// connections.");
            LDAPConnection connection = new LDAPConnection(_ldapConnectionOptions, _host, _port, _bindUser, _bindPassword);
            return PagingLdapConnectionImpl.wrap(connection);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    @Override
    public void closeConnection(PagingLdapConnection connection) {
        getLdapConnection(connection).close();
    }

    @Override
    public PagingLdapConnection ensureConnection(PagingLdapConnection connection) throws LdapClientException {
        LDAPConnection ldapConnection = getLdapConnection(connection);
        if (!ldapConnection.isConnected()) {
            try {
                ldapConnection.reconnect();
            } catch (LDAPException e) {
                throw new LdapClientException(e);
            }
        }
        return connection;
    }

    /**
     * Since {@link LDAPInterface} hides the implementation's {@link LDAPConnection#close() close()} and {@link
     * LDAPConnection#reconnect() reconnect()} methods (but why?), we need this clumsy method to get hold on the underlying
     * implementation.
     */
    private LDAPConnection getLdapConnection(PagingLdapConnection connection) {
        if (connection instanceof PagingLdapConnectionImpl) {
            LDAPInterface delegateConnection = ((PagingLdapConnectionImpl) connection).getDelegateConnection();
            if (delegateConnection instanceof LDAPConnection) {
                return ((LDAPConnection) delegateConnection);
            }
        }
        throw new IllegalArgumentException(
                "Provided collection must be of type PagingLdapConnectionImpl wrapping an LDAPConnection.");
    }

}
