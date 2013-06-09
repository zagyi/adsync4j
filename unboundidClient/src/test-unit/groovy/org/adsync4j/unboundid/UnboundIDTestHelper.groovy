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
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl

import static com.unboundid.ldap.sdk.controls.SimplePagedResultsControl.PAGED_RESULTS_OID

class UnboundIDTestHelper {
    final static def PAGE_SIZE = -1
    final static def PAGING_COOKIE_VALUE = 'foo'
    final static def PAGING_COOKIE = new ASN1OctetString(PAGING_COOKIE_VALUE)

    final static SearchRequest dummySearchRequest = new SearchRequest('', SearchScope.BASE, 'foo=bar')

    static def wrapSearchEntryListsInResultObjects(List listOfSearchEntryLists) {
        if (!listOfSearchEntryLists) {
            listOfSearchEntryLists = [[]]
        }

        def i = listOfSearchEntryLists.size()
        listOfSearchEntryLists.collect { List searchEntryList ->
            createSearchResult(results: searchEntryList, isLastPage: --i == 0)
        }
    }

    static SearchResult createSearchResult(Map args) {
        List results = args.results
        def pagingCtrl = createPagedResultsControl(args)
        new SearchResult(-1, null, null, null, null, results, null, results.size(), -1, pagingCtrl)
    }

    static SimplePagedResultsControl[] createPagedResultsControl(Map args) {
        [new SimplePagedResultsControl(-1, args.isLastPage ? null : PAGING_COOKIE)]
    }

    static def hasMatchingPageCookie(SearchRequest searchRequest, expectedCookie) {
        SimplePagedResultsControl pagingControl = searchRequest.getControl(PAGED_RESULTS_OID) as SimplePagedResultsControl
        assert pagingControl
        return expectedCookie ?
            pagingControl.cookie == expectedCookie :
            pagingControl.cookie.valueLength == 0
    }
}