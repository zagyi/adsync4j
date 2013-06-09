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

import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPInterface
import org.adsync4j.testutils.IOUtils
import org.adsync4j.testutils.ldap.EmbeddedUnboundIDLdapServer
import org.adsync4j.unboundid.PagingLdapConnectionImpl
import org.adsync4j.unboundid.PagingUnboundIDConnectionFactory
import org.adsync4j.unboundid.UnboundIDLdapClient
import org.junit.Before
import org.junit.Test

class UnboundIDLdapClientIntegrationTest {

    static final def OBJECT_CLASS = 'objectClass'

    static UnboundIDLdapClient client
    static Map<String, String> config

    @Before
    public void setup() {
        if (!client) {
            loadConfig()
            def server = createEmbeddedServer()
            def connection = createConnection(server)
            client = createClient(connection)
        }
    }

    @Test
    public void testGetRootDSEAttribute() {
        def rootDSEObjectClassList = client.getRootDSEAttribute(OBJECT_CLASS).values as List
        assert rootDSEObjectClassList.containsAll('top', 'ds-root-dse')
    }

    @Test
    public void testGetEntryAttribute() {
        // given
        def dn = config['entryDn']
        def attributeName = config['attributeName']
        def expectedValue = config['attributeValue']

        // when
        def actualValue = client.getEntryAttribute(dn, attributeName).value

        // then
        assert actualValue == expectedValue
    }

    @Test
    public void testSearch() {
        // given
        def base = config['searchBase']
        def filter = config['searchFilter']
        def attributeName = config['attributeName']
        def expectedValues = [config['attributeValue']] as Set

        // when
        def attributeArrayList = client.search(base, filter, [attributeName])
        def actualValues = attributeArrayList.collect { Attribute[] attributes -> attributes[0].value } as Set

        // then
        assert actualValues == expectedValues
    }

    @Test
    public void testSearchDeleted() {
        // given
        def base = config['searchBase']
        def filter = config['searchFilter']
        def expectedId = UUID.fromString(config['objectGuidExpectedValue'])
        def expectedIdSet = [expectedId] as Set

        // when
        def deletedUserIdSet = client.searchDeleted(base, filter).collect() as Set

        // then
        assert deletedUserIdSet == expectedIdSet
    }

    static def loadConfig() {
        def resource = IOUtils.getResourceAsStreamOrFail('in-memory-ldap.properties')
        def props = new Properties()
        props.load(resource)
        config = props as Map<String, String>
    }

    static def createEmbeddedServer() {
        new EmbeddedUnboundIDLdapServer()
                .setRootDN(config['rootDN'])
                .includeStandardSchema()
                .addSchema(IOUtils.getResourceAsStreamOrFail('ldap.schema'))
                .setLdifs([IOUtils.getResourceAsStreamOrFail('users.ldif')])
                .init()
    }

    static def createConnection(EmbeddedUnboundIDLdapServer server) {
        PagingLdapConnectionImpl.wrap(new LDAPConnection('localhost', server.port))
    }

    static def createClient(LDAPInterface connection) {
        new UnboundIDLdapClient({ connection } as PagingUnboundIDConnectionFactory)
    }
}
