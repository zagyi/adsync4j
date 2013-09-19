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
import org.adsync4j.api.LdapClientException
import spock.lang.Specification

import static org.adsync4j.unboundid.UnboundIDTestHelper.*

class PagingSearchIteratorSpec extends Specification {

    LDAPInterface connection = Mock(LDAPInterface)

    def 'first iteration does not perform a search'() {
        // first page is always fetched by the time the PagingSearchIterator is created, therefore no communication is
        // done on the connection when all the search results fit on the first (already fetched) page

        given:
        def firstAndOnlyPage = ['page1/1', 'page1/2']
        def psi = buildPSI([firstAndOnlyPage])

        when:
        def actualFirstPage = psi.next()

        then:
        actualFirstPage == firstAndOnlyPage
        0 * connection._
    }

    def 'hasNext() returns false for the last page'() {
        given:
        PagingSearchIterator psi = buildPSI([['page1:entry1', 'page1:entry2'], ['page2:entry1']])

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
        4                 | 2
        5                 | 2
        1                 | 8
        7                 | 7
    }

    def 'cannot create iterator without paging control'() {
        given:
        def searchRequestWithoutPagingControl = new SearchRequest('', SearchScope.BASE, 'foo=bar')

        when:
        new PagingSearchIterator(null, searchRequestWithoutPagingControl, createSearchResult([], true))

        then:
        thrown IllegalArgumentException
    }

    def 'fails when calling next() after getting the last page'() {
        given:
        PagingSearchIterator psi = buildPSI([['page1:entry1', 'page1:entry2']])

        when:
        psi.next()
        psi.next()

        then:
        thrown NoSuchElementException
    }

    def 'propagates UnboundID-specific exception wrapped in LdapClientException'() {
        given:
        // passing a 'firstResult' to the iterator which indicates that there is more pages to fetch
        def psi = new PagingSearchIterator(connection, DUMMY_SEARCH_REQUEST, createSearchResult(['page1:entry1'], false))
        // a timeout exception during search
        1 * connection.search((SearchRequest) _) >> { throw new LDAPSearchException(ResultCode.TIMEOUT, '') }

        when:
        psi.collect()

        then:
        thrown LdapClientException
    }

    def 'check results of getPagingCookieForNextIteration'() {
        given:
        Control[] controls = pagingCtrl ? [pagingCtrl] : []
        def searchResult = new SearchResult(-1, null, null, null, null, 0, 0, controls)
        def psi = new PagingSearchIterator(null, DUMMY_SEARCH_REQUEST, createSearchResult(['page1:entry1'], true))

        expect:
        def cookie = psi.getPagingCookieForNextIteration(searchResult)
        if (cookie) {
            expectedCookieLength == cookie.valueLength
        } else {
            expectedCookieLength == null
        }

        where:
        pagingCtrl                                                      | expectedCookieLength
        new SimplePagedResultsControl(PAGE_SIZE, PAGING_COOKIE)         | PAGING_COOKIE.valueLength
        new SimplePagedResultsControl(PAGE_SIZE, new ASN1OctetString()) | 0
        new SimplePagedResultsControl(PAGE_SIZE, null)                  | 0
        null                                                            | null
    }

    PagingSearchIterator buildPSI(List pages = [[]]) {
        List<SearchResult> pagedResults = createPagedSearchResults(pages)
        SearchResult firstResult = pagedResults.remove(0)

        // unsigned right shift operator ('>>>') causes the mock take the right hand side
        // expression as a _list_ of responses to be returned in subsequent calls
        pagedResults.size() *
                connection.search({ searchRequestWithPagingCookie(it, PAGING_COOKIE) }) >>> pagedResults

        // further interactions with the connection (other than those explicitly specified in feature methods)
        // will be reported as errors
        0 * connection.search(_)

        new PagingSearchIterator(connection, DUMMY_SEARCH_REQUEST, firstResult)
    }
}
