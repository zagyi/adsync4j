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
package org.adsync4j.testutils.ldap
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import org.junit.After
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.adsync4j.testutils.IOUtils.getResourceAsStreamOrFail
import static org.hamcrest.Matchers.is
import static spock.util.matcher.HamcrestSupport.that

@ContextConfiguration(locations = 'classpath:org/adsync4j/testutils/ldap/embedded-ldap-server-test-context.xml')
class EmbeddedUnboundIDLdapServerSpec extends Specification {

    static final def ROOT_DN = 'dc=test,dc=com'
    static final def USER_NAME = 'lali'

    @Autowired
    EmbeddedUnboundIDLdapServer embeddedLdapServerCreatedBySpring

    def embeddedLdapServer

    @After
    public void tearDown() {
        embeddedLdapServer.shutDown()
    }

    def 'perform search against embedded server set up on localhost'() {
        given:
        def connection = getConnectionToTheEmbeddedLdapServer(port, useSpringProvidedServer)

        def searchResult = searchUser(connection, USER_NAME)
        def sn = searchResult.searchEntries[0].getAttribute('sn').value

        expect:
        that searchResult.entryCount, is(1)
        that sn, is(USER_NAME)

        where:
        port  | useSpringProvidedServer
        33389 | false
        null  | false
        null  | true
    }

    def getConnectionToTheEmbeddedLdapServer(port, useSpringProvidedServer) {
        if (useSpringProvidedServer) {
            embeddedLdapServer = embeddedLdapServerCreatedBySpring
        } else {
            embeddedLdapServer = createEmbeddedServer(port)
        }
        new LDAPConnection('localhost', embeddedLdapServer.port)
    }

    def createEmbeddedServer(port) {
        def ldif = getResourceAsStreamOrFail('data/all-in-one.ldif')
        def server =
            new EmbeddedUnboundIDLdapServer()
                .setRootDN(ROOT_DN)
                .setLdifs([ldif])
        if (port) server.setPort(port)
        server.init()
    }

    SearchResult searchUser(LDAPConnection conn, def username) {
        conn.search("ou=users,$ROOT_DN", SearchScope.SUB, "(uid=$username)")
    }
}
