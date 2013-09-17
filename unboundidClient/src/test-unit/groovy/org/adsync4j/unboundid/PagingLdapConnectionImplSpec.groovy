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

import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.ResultCode
import spock.lang.Specification

import static org.adsync4j.unboundid.UnboundIDTestHelper.*

class PagingLdapConnectionImplSpec extends Specification {

    LDAPInterface nonPagingConnection = Mock(LDAPInterface)

    def 'should delegate LDAPInterface methods'() {
        given:
        PagingLdapConnection pagingConnection = PagingLdapConnectionImpl.wrap(nonPagingConnection)

        when:
        // checking just a few methods
        pagingConnection.search(DUMMY_SEARCH_REQUEST)
        pagingConnection.delete('foo')
        pagingConnection.getEntry('foo')

        then:
        1 * nonPagingConnection.search(DUMMY_SEARCH_REQUEST)
        1 * nonPagingConnection.delete('foo')
        1 * nonPagingConnection.getEntry('foo')
    }

    def 'paging search returns all pages '() {
        given:
        List pages = [['page1/1', 'page1/2'], ['page2/1']]
        PagingLdapConnection pagingConnection = buildPagingLdapConnection(pages)

        when:
        List entries = pagingConnection.search(DUMMY_SEARCH_REQUEST, PAGE_SIZE).collect()

        then:
        entries as List == pages.flatten()
    }

    def 'delegate exceptions from the underlying non-paging connection'() {
        given:
        // emulating a time-out on getEntry()
        nonPagingConnection.getEntry(_) >> { throw new LDAPException(ResultCode.TIMEOUT) }
        PagingLdapConnection pagingConnection = PagingLdapConnectionImpl.wrap(nonPagingConnection)

        when:
        pagingConnection.getEntry('foo')

        then:
        thrown LDAPException
    }

    PagingLdapConnection buildPagingLdapConnection(List pages) {
        def listOfSearchEntryLists = createPagedSearchResults(pages)

        // first invocation should not have a paging cookie
        if (listOfSearchEntryLists.size() > 0) {
            1 * nonPagingConnection.search({ searchRequestWithInitialPagingCookie(it) }) >> listOfSearchEntryLists[0]
            listOfSearchEntryLists.remove(0)
        }

        // unsigned right shift operator ('>>>') causes the mock take the right hand side
        // expression as a _list_ of responses to be returned upon subsequent invocations
        listOfSearchEntryLists.size() *
                nonPagingConnection.search({ searchRequestWithPagingCookie(it, PAGING_COOKIE) }) >>> listOfSearchEntryLists

        // interactions other than those explicitly specified in feature methods will be reported as errors
        0 * nonPagingConnection.search(_)

        PagingLdapConnectionImpl.wrap(nonPagingConnection)
    }
}
