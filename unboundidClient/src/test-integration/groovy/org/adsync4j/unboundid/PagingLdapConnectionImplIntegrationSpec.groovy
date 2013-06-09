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
package org.adsync4j.unboundid
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import org.adsync4j.testutils.ldap.EmbeddedUnboundIDLdapServer
import spock.lang.Specification

import static org.adsync4j.testutils.TestUtils.getResourceAsStreamOrFail

class PagingLdapConnectionImplIntegrationSpec extends Specification {

    static final def ROOT_DN = 'dc=example,dc=com'

    PagingLdapConnection pagingConnection

    static def createEmbeddedLdapServer(ldifLocation) {
        def ldif = getResourceAsStreamOrFail(ldifLocation)
        new EmbeddedUnboundIDLdapServer()
                .setRootDN(ROOT_DN)
                .addLdif(ldif)
                .init()
    }

    static def createPagingConnection(EmbeddedUnboundIDLdapServer embeddedLdapServer) {
        LDAPConnection conn = new LDAPConnection('localhost', embeddedLdapServer.port)
        PagingLdapConnectionImpl.wrap(conn)
    }

    def 'retrieve 5 users in pages of 2'() {
        given:
        def embeddedLdapServer = createEmbeddedLdapServer('five-users.ldif')
        pagingConnection = createPagingConnection(embeddedLdapServer)

        SearchRequest searchRequest = new SearchRequest(
                "ou=users,$ROOT_DN",
                SearchScope.SUB,
                'objectClass=inetOrgPerson',
                'sn')

        when:
        def results = pagingConnection.search(searchRequest, 2).collect { resultEntry ->
            resultEntry.getAttributeValue('sn')
        } as Set

        then:
        results.size() == 5
        results == ['user1', 'user2', 'user3', 'user4', 'user5'] as Set
    }
}
