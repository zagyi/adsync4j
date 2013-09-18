package org.adsync4j.test

import org.adsync4j.LdapClientException
import org.adsync4j.testutils.ldap.EmbeddedUnboundIDLdapServer
import org.adsync4j.unboundid.PagingLdapConnection
import org.adsync4j.unboundid.PagingLdapConnectionImpl
import org.adsync4j.unboundid.PagingUnboundIDConnectionFactory

class EmbeddedUnboundIDServerConnectionFactory implements PagingUnboundIDConnectionFactory {

    private final EmbeddedUnboundIDLdapServer embeddedLdapServer

    @Lazy PagingLdapConnection connection = PagingLdapConnectionImpl.wrap(embeddedLdapServer.connection)

    EmbeddedUnboundIDServerConnectionFactory(EmbeddedUnboundIDLdapServer embeddedLdapServer) {
        this.embeddedLdapServer = embeddedLdapServer
    }

    @Override
    PagingLdapConnection createConnection() throws LdapClientException {
        connection
    }

    @Override
    PagingLdapConnection ensureConnection(PagingLdapConnection connection) throws LdapClientException {
        if (!embeddedLdapServer.connection.isConnected()) {
            embeddedLdapServer.connection.reconnect()
        }
        connection
    }

    @Override
    void closeConnection(PagingLdapConnection connection) {
        embeddedLdapServer.connection.close()
    }
}
