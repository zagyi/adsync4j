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

import com.google.common.collect.Iterables;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class that executes {@link SearchRequest}s with paging while shielding the caller from the complexity of managing the
 * {@link SimplePagedResultsControl paging search control object} and the paging cookie. The client code stays completely
 * unaware of the fact that search results are fetched by pages in multiple subsequent steps.
 * <p/>
 * Doing a paging search then becomes as simple as:
 * <pre>
 * PagingSearchExecutor pagingSearchExecutor = new PagingSearchExecutor(getConnection(), searchRequest, _pageSize);
 * for (SearchResultEntry entry : pagingSearchExecutor.search()) {
 *     // process the entry
 * }
 * </pre>
 */
@ThreadSafe
class PagingLdapConnectionImpl implements PagingLdapSearcher, InvocationHandler {
    private final LDAPInterface _delegateConnection;

    /**
     * Creates a search executor that applies paging when retrieving search results.
     *
     * @param connection The connection on which the search request is to be executed.
     */
    public static PagingLdapConnection wrap(LDAPInterface connection) {
        return (PagingLdapConnection) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{PagingLdapConnection.class},
                new PagingLdapConnectionImpl(connection));
    }

    public PagingLdapConnectionImpl(LDAPInterface delegateConnection) {
        _delegateConnection = delegateConnection;
    }

    /**
     * Executes the search request applying paging behind the scenes. The return type cannot be a more specific collection
     * type, because the total number of entries matching the search request is not known in advance.
     *
     * @return An {@link Iterable} of {@link SearchResultEntry} items through which callers can iterate over the entire
     *         result set without caring about the fact that entries are fetched by pages behind the scenes.
     */
    @Override
    public Iterable<SearchResultEntry> search(final SearchRequest searchRequest, final int pageSize) throws LDAPException {
        searchRequest.setControls(new SimplePagedResultsControl(pageSize, null));
        final SearchResult firstPage = _delegateConnection.search(searchRequest);

        Iterable<List<SearchResultEntry>> pages =
                new Iterable<List<SearchResultEntry>>() {
                    @Override
                    public Iterator<List<SearchResultEntry>> iterator() {
                        return new PagingSearchIterator(_delegateConnection, searchRequest, firstPage);
                    }
                };
        return Iterables.concat(pages);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("search")
            && args.length == 2
            && args[0] instanceof SearchRequest
            && args[1] instanceof Integer) {
            return search((SearchRequest) args[0], (int) args[1]);
        } else {
            try {
                return method.invoke(_delegateConnection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
