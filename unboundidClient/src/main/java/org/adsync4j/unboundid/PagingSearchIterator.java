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

import com.google.common.base.Preconditions;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import org.adsync4j.LdapClientException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator returning a single page (lists) of {@link com.unboundid.ldap.sdk.SearchResultEntry} elements on each iteration.
 * It repeatedly executes the LDAP query with a limit for the number of entries to be fetched in a single page given at
 * construction time.
 */
public class PagingSearchIterator implements Iterator<List<SearchResultEntry>> {

    private final static XLogger LOG = XLoggerFactory.getXLogger(PagingSearchIterator.class);

    private final LDAPInterface _connection;
    private final SearchRequest _searchRequest;
    private final int _pageSize;

    boolean _isLastPageFetched;
    ASN1OctetString _pagingCookie = null;

    int _numOfPagesServed = 0;
    int _numOfPagesFetched = 0;
    List<SearchResultEntry> _currentPage = null;

    /**
     * @param connection    The connection on which the search request is to be executed.
     * @param searchRequest The search request (without paging controls).
     * @param pageSize      Maximum number of entries to be fetched in a single page (in one iteration).
     */
    public PagingSearchIterator(LDAPInterface connection, SearchRequest searchRequest, int pageSize) {
        _connection = connection;
        _searchRequest = searchRequest;
        _pageSize = pageSize;
    }

    @Override
    public boolean hasNext() {
        if (_numOfPagesFetched == 0) {
            fetchNextPage();
        }

        boolean isCurrentPageEmpty = _currentPage == null || _currentPage.isEmpty();

        return _numOfPagesServed == 0
                          ? !isCurrentPageEmpty
                          : !_isLastPageFetched;
    }

    @Override
    public List<SearchResultEntry> next() {
        if (hasNext()) {
            if (shouldFetchNextPage()) {
                fetchNextPage();
            }

            ++_numOfPagesServed;
            return _currentPage;
        }

        throw new NoSuchElementException();
    }

    /*package*/ List<SearchResultEntry> fetchNextPage() {
        Preconditions.checkState(
                _numOfPagesFetched - _numOfPagesServed < 1,
                "Attempted to fetch the next page (#%d) while the previous one has not been consumed yet.",
                _numOfPagesFetched
        );

        _searchRequest.replaceControl(new SimplePagedResultsControl(_pageSize, _pagingCookie));

        SearchResult searchResult;
        try {
            LOG.trace("instance: {}, page: #{}, with cookie: {}",
                    this.hashCode(),
                    _numOfPagesFetched,
                    _pagingCookie != null && _pagingCookie.getValueLength() > 0);

            searchResult = _connection.search(_searchRequest);
            _pagingCookie = getPagingCookieForNextIteration(searchResult);
            ++_numOfPagesFetched;
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }

        _isLastPageFetched = _pagingCookie == null || _pagingCookie.getValueLength() == 0;
        _currentPage = searchResult.getSearchEntries();

        if (_numOfPagesFetched > 1 && (_currentPage == null || _currentPage.isEmpty())) {
            LOG.warn("Ldap paged search returned null or empty result list when fetching the next page, " +
                     "which normally should never happen. SearchResult object returned: {}",
                    searchResult);
        }

        return _currentPage;
    }

    /*package*/ ASN1OctetString getPagingCookieForNextIteration(@Nonnull SearchResult searchResult) throws LDAPException {
        SimplePagedResultsControl pagedCtrlResponse = SimplePagedResultsControl.get(searchResult);
        return pagedCtrlResponse == null
               ? null
               : pagedCtrlResponse.getCookie();
    }

    private boolean shouldFetchNextPage() {
        assert _numOfPagesServed <= _numOfPagesFetched;
        return _numOfPagesServed == _numOfPagesFetched;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
