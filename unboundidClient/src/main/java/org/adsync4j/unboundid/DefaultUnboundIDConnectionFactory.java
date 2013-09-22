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
import org.adsync4j.api.LdapClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A simple implementation of {@link PagingUnboundIDConnectionFactory} that creates an unsecured connection with the provided
 * parameters.
 */
@NotThreadSafe
public class DefaultUnboundIDConnectionFactory implements PagingUnboundIDConnectionFactory {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultUnboundIDConnectionFactory.class);

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
     * {@link PagingLdapConnection} implementation that adds the paging search operation.
     */
    @Override
    public PagingLdapConnection createConnection() throws LdapClientException {
        try {
            checkArgument("ldap".equals(_protocol),
                    "This connection factory supports only the creation of unsecured ldap:// connections.");
            LOG.debug("Opening LDAP connection to ldap://{}:{}, and binding with user: {}", _host, _port, _bindUser);

            LDAPConnection connection = new LDAPConnection(_ldapConnectionOptions, _host, _port, _bindUser, _bindPassword);
            return new PagingLdapConnectionImpl(connection);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }
}
