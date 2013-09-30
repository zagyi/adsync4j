package org.adsync4j.unboundid

import org.adsync4j.api.LdapClientException
import org.adsync4j.testutils.ldap.EmbeddedUnboundIDLdapServer

class EmbeddedUnboundIDServerConnectionFactory implements PagingUnboundIDConnectionFactory {

    private final EmbeddedUnboundIDLdapServer embeddedLdapServer

    @Lazy PagingLdapConnection connection = new PagingLdapConnectionTestImpl(embeddedLdapServer.connection)

    EmbeddedUnboundIDServerConnectionFactory(EmbeddedUnboundIDLdapServer embeddedLdapServer) {
        this.embeddedLdapServer = embeddedLdapServer
    }

    @Override
    PagingLdapConnection createConnection() throws LdapClientException {
        connection
    }
}
