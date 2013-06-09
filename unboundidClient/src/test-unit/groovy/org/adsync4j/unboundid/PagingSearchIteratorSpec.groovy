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

import com.google.common.collect.Lists
import com.unboundid.asn1.ASN1OctetString
import com.unboundid.ldap.sdk.*
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl
import org.adsync4j.LdapClientException
import spock.lang.Specification

class PagingSearchIteratorSpec extends Specification {

    LDAPInterface connection = Mock(LDAPInterface)

    def 'hasNext() invokes the initial search if it is called before next()'() {
        given:

        def psi = new PagingSearchIterator(connection, org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE)

        when:
        psi.hasNext()

        then:
        1 * connection.search(org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest) >> org.adsync4j.unboundid.UnboundIDTestHelper.createSearchResult(results: [], isLastPage: true)
    }

    def 'hasNext() returns false for the last page'() {
        given:
        PagingSearchIterator psi = buildPSI([['page1/1', 'page1/2'], ['page2/1']])

        expect:
        psi.hasNext()

        when:
        psi.next()
        then:
        psi.hasNext()

        when:
        psi.next()
        then:
        !psi.hasNext()
    }

    def 'hasNext() returns false if the first page is empty'() {
        given:
        def psi = buildPSI([[]])

        expect:
        !psi.hasNext()
    }

    def 'entries are fully fetched in arbitrary pageSize/totalNumberOfEntries combinations'() {
        given:
        List entries = [* 1..totalNumOfEntries] // == [1, 2, ..., n]
        def pages = Lists.partition(entries, pageSize)

        PagingSearchIterator psi = buildPSI(pages)

        when:
        def actualEntries = psi.iterator().collect()

        then:
        entries == actualEntries.flatten()

        where:
        totalNumOfEntries | pageSize
        5                 | 2
        1                 | 8
        7                 | 7
    }

    def 'fails when calling next() after getting last page'() {
        given:
        PagingSearchIterator psi = buildPSI([['page1/1', 'page1/2']])

        when:
        psi.next()
        psi.next()

        then:
        thrown NoSuchElementException
    }

    def 'fetchNextPage() fails when called twice before calling next()'() {
        given:
        PagingSearchIterator psi = buildPSI([['page1']])
        psi.hasNext();

        when:
        psi.fetchNextPage()

        then:
        thrown IllegalStateException
    }

    def 'propagates UnboundID-specific exception wrapped in LdapClientException'() {
        given:
        1 * connection.search((SearchRequest) _) >> { throw new LDAPSearchException(ResultCode.TIMEOUT, '') }
        def psi = new PagingSearchIterator(connection, org.adsync4j.unboundid.UnboundIDTestHelper.dummySearchRequest, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE)

        when:
        psi.hasNext()

        then:
        thrown LdapClientException
    }

    def 'checking post-conditions of fetchNextPage()'() {
        given:
        def pages = [['page1/1', 'page1/2']]

        PagingSearchIterator psi = buildPSI(pages)

        when:
        def actualPage = psi.fetchNextPage()

        then:
        actualPage == pages[0]
        psi._currentPage == actualPage
        psi._numOfPagesFetched == 1
        psi._isLastPageFetched
        psi._numOfPagesServed == 0
        psi._pagingCookie.valueLength == 0
    }

    def 'checking results of getPagingCookieForNextIteration'() {
        given:
        def searchResult = new SearchResult(-1, null, null, null, null, 0, 0, [pagingCtrl] as Control[])
        def psi = new PagingSearchIterator(null, null, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE)

        expect:
        psi.getPagingCookieForNextIteration(searchResult).valueLength == cookieLength

        where:
        pagingCtrl                                                      | cookieLength
        new SimplePagedResultsControl(org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE, org.adsync4j.unboundid.UnboundIDTestHelper.PAGING_COOKIE)         | org.adsync4j.unboundid.UnboundIDTestHelper.PAGING_COOKIE_VALUE.length()
        new SimplePagedResultsControl(org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE, new ASN1OctetString()) | 0
        new SimplePagedResultsControl(org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE, null)                  | 0
    }

    // TODO: refactor code argument matchers to assertions on captured arguments
    @SuppressWarnings("GroovyAssignabilityCheck")
    PagingSearchIterator buildPSI(List pages) {
        def listOfSearchEntryLists = org.adsync4j.unboundid.UnboundIDTestHelper.wrapSearchEntryListsInResultObjects(pages)

        // first invocation should not have a paging cookie
        if (listOfSearchEntryLists.size() > 0) {
            1 * connection.search({ org.adsync4j.unboundid.UnboundIDTestHelper.hasMatchingPageCookie(it, null) }) >> listOfSearchEntryLists[0]
            listOfSearchEntryLists.remove(0)
        }

        // unsigned right shift operator ('>>>') causes the mock take the right hand side
        // expression as a _list_ of responses to be returned in subsequent calls
        listOfSearchEntryLists.size() *
                connection.search({ org.adsync4j.unboundid.UnboundIDTestHelper.hasMatchingPageCookie(it, org.adsync4j.unboundid.UnboundIDTestHelper.PAGING_COOKIE) }) >>> listOfSearchEntryLists

        // the following lines ensure that any interaction with the below mentioned mocks
        // other than those explicitly specified in feature methods will be reported as errors
        0 * connection.search(_)

        new PagingSearchIterator(connection, UnboundIDTestHelper.dummySearchRequest, org.adsync4j.unboundid.UnboundIDTestHelper.PAGE_SIZE)
    }
}
