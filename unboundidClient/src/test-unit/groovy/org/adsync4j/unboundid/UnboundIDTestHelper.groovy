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

import com.unboundid.asn1.ASN1OctetString
import com.unboundid.ldap.sdk.Control
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl

import static com.unboundid.ldap.sdk.controls.SimplePagedResultsControl.PAGED_RESULTS_OID

/**
 * Utility class that helps building {@link SearchResult} objects emulating responses to a paged search request.
 */
class UnboundIDTestHelper {
    final static def PAGE_SIZE = -1
    final static def PAGING_COOKIE_VALUE = 'foo'
    final static def PAGING_COOKIE = new ASN1OctetString(PAGING_COOKIE_VALUE)

    final static SearchRequest DUMMY_SEARCH_REQUEST = new SearchRequest('', SearchScope.BASE, 'foo=bar')
    final static SimplePagedResultsControl PAGING_CONTROL_FOR_REQUEST = new SimplePagedResultsControl(PAGE_SIZE)

    static {
        DUMMY_SEARCH_REQUEST.addControl(PAGING_CONTROL_FOR_REQUEST)
    }

    /**
     *
     * @param listOfSearchEntryLists A list of search result entry lists (each list representing a page of search results).
     * @return A list of {@link SearchResult}s enriched with a properly configured {@link SimplePagedResultsControl} (which
     * indicates if a result set is intermediate or final in the series).
     */
    static List<SearchResult> createPagedSearchResults(List<List<?>> listOfSearchEntryLists) {
        if (!listOfSearchEntryLists) {
            listOfSearchEntryLists = [[]]
        }

        def i = listOfSearchEntryLists.size()
        listOfSearchEntryLists.collect { List<?> searchEntryList ->
            boolean isLastPage = --i == 0
            createSearchResult(searchEntryList, isLastPage)
        }
    }

    /**
     *
     * @param searchEntryList List of entries to include in the {@link SearchResult}.
     * @param isLastPage Determines whether to include a paging cookie in the {@link SearchResult} which indicates to the
     * client if it's an intermediate or a final result in a series of paged results.
     * @return A {@link SearchResult} object wrapping the provided entries and containing a {@link SimplePagedResultsControl}
     * according to the {@code isLastPage} argument.
     */
    static SearchResult createSearchResult(List<?> searchEntryList, boolean isLastPage) {
        Control[] resultControls = [createPagedResultsControl(isLastPage)]
        new SearchResult(-1, null, null, null, null,
                searchEntryList as List<SearchResultEntry>, null, searchEntryList.size(), -1, resultControls)
    }

    /**
     * A paged search result indicates if there is more pages to fetch by setting a paging cookie in a {@link
     * SimplePagedResultsControl} and adding that to the response object.
     * <p/>
     * This method helps emulating intermediate and final search result objects by creating a {@link
     * SimplePagedResultsControl} with or without a paging cookie according to the {@code isLastPage} argument.
     */
    static SimplePagedResultsControl createPagedResultsControl(boolean isLastPage) {
        new SimplePagedResultsControl(PAGE_SIZE, isLastPage ? null : PAGING_COOKIE)
    }

    /**
     * Asserts that the provided {@link SearchRequest} contains a {@link SimplePagedResultsControl} with the expected cookie
     * value.
     *
     * @param searchRequest The {@link SearchRequest} to examine.
     * @param expectedCookie The cookie that must be held by the request's {@link SimplePagedResultsControl}.
     * @return True if the {@link SimplePagedResultsControl} is present in the request and it holds the specified cookie value,
     * false otherwise.
     */
    static def searchRequestWithPagingCookie(SearchRequest searchRequest, ASN1OctetString expectedCookie) {
        SimplePagedResultsControl pagingControl = searchRequest.getControl(PAGED_RESULTS_OID) as SimplePagedResultsControl
        assert pagingControl
        return expectedCookie ?
            pagingControl.cookie == expectedCookie :
            pagingControl.cookie.valueLength == 0
    }

    /**
     * Asserts that the provided {@link SearchRequest} contains a {@link SimplePagedResultsControl} with an "initial" cookie
     * value (en empty string).
     *
     * @param searchRequest The {@link SearchRequest} to examine.
     * @return True if the {@link SimplePagedResultsControl} is present in the request and it holds an empty cookie,
     * false otherwise.
     */
    static def searchRequestWithInitialPagingCookie(SearchRequest searchRequest) {
        SimplePagedResultsControl pagingControl = searchRequest.getControl(PAGED_RESULTS_OID) as SimplePagedResultsControl
        assert pagingControl
        return pagingControl.cookie.valueLength == 0
    }
}