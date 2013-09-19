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

import com.unboundid.ldap.sdk.*
import org.adsync4j.api.LdapClientException
import spock.lang.Specification

import static org.adsync4j.spi.LdapClient.OBJECT_GUID
import static org.adsync4j.spi.LdapClient.SHOW_DELETED_CONTROL_OID
import static org.adsync4j.testutils.TestUtils.uuidToBytArray

class UnboundIDLdapClientSpec extends Specification {

    static final def PAGE_SIZE = 17
    static final def BASE_DN = 'rootDN'
    static final def FILTER = 'iAm=aFilter'

    static final def ATTRIBUTE_NAME = 'attributeName'
    static final def ATTRIBUTE_VALUE = 'value'
    static final def ATTRIBUTE = new Attribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE)
    static final def ENTRY_DN = 'entryDN'
    static final def EMPTY_ENTRY = new SearchResultEntry('', [] as Attribute[])
    static final def ENTRY = new SearchResultEntry('', [ATTRIBUTE])

    PagingLdapConnection connection = Mock(PagingLdapConnection)
    UnboundIDLdapClient client = new UnboundIDLdapClient({ connection } as PagingUnboundIDConnectionFactory)

    def 'setup'() {
        client.pageSize = PAGE_SIZE
    }

    def 'getRootDSEAttribute() returns the value of a rootDSE attribute'() {
        given:
        1 * connection.getRootDSE() >> new RootDSE(ENTRY)
        allowNoFurtherInteractions()

        when:
        def attribute = client.getRootDSEAttribute(ATTRIBUTE_NAME)

        then:
        attribute.value == 'value'
    }

    def 'getRootDSEAttribute() fails on missing rootDSE entry or attribute'() {
        given:
        1 * connection.getRootDSE() >> rootDSE
        allowNoFurtherInteractions()

        when:
        client.getRootDSEAttribute(ATTRIBUTE_NAME)

        then:
        thrown LdapClientException

        where:
        rootDSE                  | _
        null                     | _
        new RootDSE(EMPTY_ENTRY) | _
    }

    def 'getEntryAttribute() returns the value of an entry attribute'() {
        given:
        1 * connection.getEntry(ENTRY_DN, ATTRIBUTE_NAME) >> ENTRY
        allowNoFurtherInteractions()

        when:
        def actualAttribute = client.getEntryAttribute(ENTRY_DN, ATTRIBUTE_NAME)

        then:
        actualAttribute == ATTRIBUTE
    }

    def 'getEntryAttribute() fails on missing entry'() {
        given:
        1 * connection.getEntry(ENTRY_DN, ATTRIBUTE_NAME) >> entry
        allowNoFurtherInteractions()

        when:
        client.getEntryAttribute(ENTRY_DN, ATTRIBUTE_NAME)

        then:
        thrown LdapClientException

        where:
        entry       | _
        null        | _
        EMPTY_ENTRY | _
    }

    def 'search() submits the correct search request'() {
        given:
        def attributes = ['attribute1', 'attribute2']
        SearchRequest capturedRequest
        allowNoFurtherInteractions()

        when:
        client.search(BASE_DN, FILTER, attributes).collect()

        then:
        1 * connection.search({ capturedRequest = it }, PAGE_SIZE) >> []

        capturedRequest.baseDN == BASE_DN
        capturedRequest.filter.toString() == FILTER
        capturedRequest.scope == SearchScope.SUB
        capturedRequest.attributes == attributes
    }

    def 'search() returns attributes in the same order as requested'() {
        given:
        def requestedAttributes = ['attribute1', 'attribute2']
        def attribute1 = new Attribute('attribute1', 'value1')
        def attribute2 = new Attribute('attribute2', 'value2')
        def resultEntries = [
                new SearchResultEntry('', [attribute2, attribute1] as Attribute[]),
                new SearchResultEntry('', [attribute1] as Attribute[]),
                new SearchResultEntry('', [attribute2] as Attribute[]),
                new SearchResultEntry('', [] as Attribute[]),
        ]

        1 * connection.search(* _) >> resultEntries
        allowNoFurtherInteractions()

        when:
        def entries = client.search(BASE_DN, FILTER, requestedAttributes).collect()

        then:
        entries.size() == resultEntries.size()
        entries[0] == [attribute1, attribute2]
        entries[1] == [attribute1, null]
        entries[2] == [null, attribute2]
        entries[3] == [null, null]
    }

    def 'searchDeleted() submits the correct search request'() {
        given:
        SearchRequest capturedRequest

        when:
        client.searchDeleted(BASE_DN, FILTER).collect()

        then:
        1 * connection.search({ capturedRequest = it }, PAGE_SIZE) >> []
        capturedRequest.baseDN == BASE_DN
        capturedRequest.scope == SearchScope.SUB
        capturedRequest.filter.toString() == FILTER
        capturedRequest.attributes == [OBJECT_GUID]
        capturedRequest.controls.any { Control ctrl ->
            ctrl.OID == SHOW_DELETED_CONTROL_OID }

        interaction { allowNoFurtherInteractions() }
    }

    def 'searchDeleted() returns UUIDs'() {
        given:
        def id = new UUID(1, 1)
        def attribute = new Attribute('id', uuidToBytArray(id))
        def entry = new SearchResultEntry('', [attribute])

        when:
        def ids = client.searchDeleted(BASE_DN, FILTER).collect()

        then:
        1 * connection.search(* _) >> [entry]
        ids == [id]
        interaction { allowNoFurtherInteractions() }
    }

    def 'searchDeleted() should not throw exceptions when UUID of deleted object cannot be parsed'() {
        given:
        1 * connection.search(* _) >> [ENTRY]
        allowNoFurtherInteractions()

        when:
        def ids = client.searchDeleted(BASE_DN, FILTER).collect()

        then:
        ids == [null]
    }

    def 'all methods propagate ldap exception'() {
        given:
        def client = new UnboundIDLdapClient({ connection } as PagingUnboundIDConnectionFactory)
        connection._ >> { throw new LDAPException(ResultCode.TIMEOUT) }

        when:
        client.getRootDSEAttribute(null)
        then:
        thrown LdapClientException

        when:
        client.getEntryAttribute(null, null)
        then:
        thrown LdapClientException

        when:
        client.search('', '', [])
        then:
        thrown LdapClientException

        when:
        client.searchDeleted('', '')
        then:
        thrown LdapClientException
    }

    def 'resolve long attribute'() {
        given:
        def expectedLong = 1234
        def attribute = new Attribute('', Long.toString(expectedLong))

        expect:
        expectedLong == client.attributeResolver.getAsLong(attribute)
    }

    def 'resolve String attribute'() {
        given:
        def expectedString = 'foo'
        def attribute = new Attribute('', expectedString)

        expect:
        expectedString == client.attributeResolver.getAsString(attribute)
    }

    def 'resolve String list attribute'() {
        given:
        def expectedStringList = ['foo', 'bar']
        def attribute = new Attribute('', expectedStringList)

        expect:
        expectedStringList == client.attributeResolver.getAsStringList(attribute)
    }

    def allowNoFurtherInteractions() {
        0 * connection._ // forbid interactions other than those defined in a spec method
    }
}
