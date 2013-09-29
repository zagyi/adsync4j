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
import org.adsync4j.spi.DCARepository;
import org.adsync4j.spi.DomainControllerAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple connection factory that creates an unsecured connection with the parameters stored in a
 * {@link DomainControllerAffiliation} record.
 */
@NotThreadSafe
public class DefaultUnboundIDConnectionFactory<DCA_KEY> implements PagingUnboundIDConnectionFactory {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultUnboundIDConnectionFactory.class);
    private final static Pattern URL_PATTERN = Pattern.compile("ldap://(.+):(\\d+)");

    private final DCA_KEY _dcaKey;
    private final DCARepository<DCA_KEY, ?> _affiliationRepository;

    String _bindUser;
    String _bindPassword;

    String _host;
    int _port = -1;

    @Nullable
    private LDAPConnectionOptions _ldapConnectionOptions;

    /**
     * Creates a connection factory that uses the URL stored in the {@link DomainControllerAffiliation} record loaded from the
     * provided repository using the specified key. The user credentials in the DCA will be ignored in favor of the values
     * given in the arguments. This constructor is recommended when saving user credentials into the DCA repository is not
     * desirable.
     *
     * @param dcaKey                Identification key of the DCA record containing the server's URL.
     * @param affiliationRepository The repository that contains the DCA record specified by the {@code dcaKey}.
     * @param bindUser              The user name to authenticate with.
     * @param bindPassword          The password to use on authentication.
     */
    public DefaultUnboundIDConnectionFactory(
            DCA_KEY dcaKey, DCARepository<DCA_KEY, ?> affiliationRepository, String bindUser, String bindPassword)
    {
        _dcaKey = dcaKey;
        _affiliationRepository = affiliationRepository;
        _bindUser = bindUser;
        _bindPassword = bindPassword;
    }

    /**
     * Creates a connection factory that uses the connection parameters stored in the {@link DomainControllerAffiliation}
     * record loaded from the provided repository using the specified key. This constructor can be used when saving user
     * credentials into the DCA repository is not an issue.
     *
     * @param dcaKey                Identification key of the DCA record storing the server's URL and the user credentials.
     * @param affiliationRepository The repository that contains the DCA record specified by the {@code dcaKey}.
     */
    public DefaultUnboundIDConnectionFactory(DCA_KEY dcaKey, DCARepository<DCA_KEY, ?> affiliationRepository) {
        this(dcaKey, affiliationRepository, null, null);
    }

    /**
     * Creates an {@link LDAPConnection} and wraps it in a {@link PagingLdapConnection} implementation that adds the paging
     * search operation.
     */
    @Override
    public PagingLdapConnection createConnection() throws LdapClientException {
        loadDCA();

        try {
            LOG.debug("Opening LDAP connection to ldap://{}:{}, and binding with user: {}", _host, _port, _bindUser);
            LDAPConnection connection = new LDAPConnection(_ldapConnectionOptions, _host, _port, _bindUser, _bindPassword);
            return new PagingLdapConnectionImpl(connection);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    /*package*/ void loadDCA() {
        DomainControllerAffiliation dca = _affiliationRepository.load(_dcaKey);
        parseUrl(dca.getUrl());
        _bindUser = _bindUser == null ? dca.getBindUser() : _bindUser;
        _bindPassword = _bindPassword == null ? dca.getBindPassword() : _bindPassword;
    }

    private void parseUrl(String url) throws LdapClientException {
        Matcher urlPatternMatcher = URL_PATTERN.matcher(url);

        if (urlPatternMatcher.matches() && urlPatternMatcher.groupCount() == 2) {
            _host = urlPatternMatcher.group(1);
            try {
                _port = Integer.parseInt(urlPatternMatcher.group(2));
            } catch (NumberFormatException ignored) {
            }
        }

        if (_host == null || _port == -1 || _port > 0xffff) {
            throw new LdapClientException("Could not parse '" + url + "' as an LDAP URL (ldap://<host>:<port>).");
        }
    }

    public void setLdapConnectionOptions(@Nullable LDAPConnectionOptions ldapConnectionOptions) {
        _ldapConnectionOptions = ldapConnectionOptions;
    }
}
