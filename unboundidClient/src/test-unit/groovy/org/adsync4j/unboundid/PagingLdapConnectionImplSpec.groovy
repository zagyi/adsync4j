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

import com.unboundid.ldap.sdk.LDAPInterface
import spock.lang.Specification

class PagingLdapConnectionImplSpec extends Specification {

    LDAPInterface nonPagingConnection = Mock(LDAPInterface)

    def 'should delegate LDAPInterface methods'() {
        given:
        PagingLdapConnection pagingConnection = PagingLdapConnectionImpl.wrap(nonPagingConnection)

        when:
        // checking just a few methods
        pagingConnection.search(org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest)
        pagingConnection.delete('foo')
        pagingConnection.getEntry('foo')

        then:
        1 * nonPagingConnection.search(org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest)
        1 * nonPagingConnection.delete('foo')
        1 * nonPagingConnection.getEntry('foo')
    }

    def 'should not delegate PagingLdapSearcher methods'() {
        given:
        PagingLdapConnection pagingConnection = PagingLdapConnectionImpl.wrap(nonPagingConnection)

        when:
        pagingConnection.search(org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE)

        then:
        0 * this.nonPagingConnection._
    }

    def 'paging search returns all pages '() {
        given:
        List pages = [['page1/1', 'page1/2'], ['page2/1']]
        PagingLdapConnection pagingConnection = buildPagingLdapConnection(pages)

        when:
        List entries = pagingConnection.search(org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE).collect()

        then:
        entries as List == pages.flatten()
    }

    // TODO: refactor code argument matchers to assertions on captured arguments
    @SuppressWarnings("GroovyAssignabilityCheck")
    PagingLdapConnection buildPagingLdapConnection(List pages) {
        def listOfSearchEntryLists = org.adsync4j.unboundid.UnboundIDTestHelper.wrapSearchEntryListsInResultObjects(pages)

        // first invocation should not have a paging cookie
        if (listOfSearchEntryLists.size() > 0) {
            1 * nonPagingConnection.search({ org.adsync4j.unboundid.UnboundIDTestHelper.hasMatchingPageCookie(it, null) }) >> listOfSearchEntryLists[0]
            listOfSearchEntryLists.remove(0)
        }

        // unsigned right shift operator ('>>>') causes the mock take the right hand side
        // expression as a _list_ of responses to be returned upon subsequent invokations
        listOfSearchEntryLists.size() *
                nonPagingConnection.search({ org.adsync4j.unboundid.UnboundIDTestHelper.hasMatchingPageCookie(it, org.adsync4j.unboundid.UnboundIDTestHelper.PAGING_COOKIE) }) >>> listOfSearchEntryLists

        // interactions other than those explicitly specified in feature methods will be reported as errors
        0 * nonPagingConnection.search(_)

        PagingLdapConnectionImpl.wrap(nonPagingConnection)
    }
}
