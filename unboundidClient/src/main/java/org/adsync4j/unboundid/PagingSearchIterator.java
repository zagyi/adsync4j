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
package org.adsync4j.unboundid;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import org.adsync4j.api.LdapClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.unboundid.ldap.sdk.controls.SimplePagedResultsControl.PAGED_RESULTS_OID;

/**
 * Iterator returning a single page of {@link com.unboundid.ldap.sdk.SearchResultEntry} elements on each iteration.
 * Clients are expected to build a search request that includes a {@link SimplePagedResultsControl}, and to make an initial
 * call to {@link LDAPInterface#search(com.unboundid.ldap.sdk.SearchRequest)} with it. The iterator can then be constructed
 * with the {@link SearchResult} returned to this the initial search invocation.
 * <p/>
 * Subsequent searches are executed by the iterator when {@link PagingSearchIterator#next next()} is called (specifying
 * the same page size as that of the initial search request).
 */
public class PagingSearchIterator implements Iterator<List<SearchResultEntry>> {

    private final static Logger LOG = LoggerFactory.getLogger(PagingSearchIterator.class);

    private final LDAPInterface _connection;
    private final SearchRequest _searchRequest;
    private final int _pageSize;

    @Nullable
    ASN1OctetString _pagingCookie = null;
    List<SearchResultEntry> _firstPage;

    /**
     * @param connection    The connection on which the search request is to be executed.
     * @param searchRequest The search request containing a {@link SimplePagedResultsControl}.
     * @param firstResult   The result of the initial search.
     */
    public PagingSearchIterator(LDAPInterface connection, SearchRequest searchRequest, SearchResult firstResult) {
        _connection = connection;
        _searchRequest = searchRequest;
        _pageSize = getPageSize(searchRequest);
        _firstPage = firstResult.getSearchEntries();
        _pagingCookie = getPagingCookieForNextIteration(firstResult);
        LOG.debug("Instance created with an initial search result that indicates there will {}be more pages.",
                _pagingCookie == null ? "NO " : "");
    }

    /**
     * @param searchRequest A {@link SearchRequest} that contains the {@link SimplePagedResultsControl} control object.
     * @return The page size specified in the {@link SimplePagedResultsControl} control object.
     */
    private static int getPageSize(SearchRequest searchRequest) {
        SimplePagedResultsControl pagingControl = (SimplePagedResultsControl) searchRequest.getControl(PAGED_RESULTS_OID);

        if (pagingControl == null) {
            throw new IllegalArgumentException("The search request must contain a SimplePagedResultsControl control object.");
        }

        return pagingControl.getSize();
    }

    @Override
    public boolean hasNext() {
        boolean isFirstPageAlreadyReturned = _firstPage == null;
        if (isFirstPageAlreadyReturned) {
            // the server indicates the last page by not including a paging cookie in the response (see SimplePagedResultsControl)
            boolean didServerReturnAPagingCookie = _pagingCookie != null && _pagingCookie.getValueLength() > 0;
            return didServerReturnAPagingCookie;
        } else {
            return !_firstPage.isEmpty();
        }
    }

    @Override
    public List<SearchResultEntry> next() {
        if (hasNext()) {
            if (_firstPage == null) {
                return fetchNextPage();
            } else {
                return getAndReleaseFirstPage();
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Simply returns the reference to the first page of search results stored in {@code _firstPage},
     * but also nulls out that reference in order to avoid retaining those entries in memory longer than necessary.
     *
     * @return The search results referenced by {@code _firstPage}.
     */
    private List<SearchResultEntry> getAndReleaseFirstPage() {
        List<SearchResultEntry> firstPage = _firstPage;
        _firstPage = null;
        return firstPage;

    }

    /**
     * Performs a search operation to fetch the next page of results. Uses the cached paging cookie for the request,
     * and updates it with the paging cookie received in the response.
     *
     * @return The next page of results.
     */
    private List<SearchResultEntry> fetchNextPage() {
        _searchRequest.replaceControl(new SimplePagedResultsControl(_pageSize, _pagingCookie));
        try {
            LOG.debug("Fetching subsequent result page.");
            SearchResult searchResult = _connection.search(_searchRequest);
            _pagingCookie = getPagingCookieForNextIteration(searchResult);
            LOG.debug("Search result page received, response indicates it's {} page.",
                    _pagingCookie == null ? "the FINAL" : "an intermediate");
            return searchResult.getSearchEntries();
        } catch (LDAPSearchException e) {
            throw new LdapClientException(e);
        }
    }

    /**
     * Extracts the paging cookie from the {@link SearchResult}.
     *
     * @param searchResult The {@link SearchResult} to extract the paging cookie from.
     * @return The paging cookie contained in the {@link SimplePagedResultsControl} of the {@link SearchResult} if any,
     *         {@code null} otherwise.
     */
    @Nullable
    /*package*/ static ASN1OctetString getPagingCookieForNextIteration(@Nonnull SearchResult searchResult) {
        try {
            SimplePagedResultsControl pagedCtrlResponse = SimplePagedResultsControl.get(searchResult);
            ASN1OctetString pagingCookie = pagedCtrlResponse == null ? null : pagedCtrlResponse.getCookie();
            return pagingCookie == null ? null :
                   pagingCookie.getValueLength() == 0 ? null : pagingCookie;
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
