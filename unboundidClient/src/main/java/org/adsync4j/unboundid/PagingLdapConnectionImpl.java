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
 * Implementation of {@link PagingLdapSearcher} that can be used in two different ways:
 * <ul>
 * <li>Use the {@link PagingLdapConnectionImpl#PagingLdapConnectionImpl constructor} to create an instance that will only
 * offer {@link PagingLdapSearcher} methods. Recommended if you only want to quickly perform a paged LDAP search.</li>
 * <li>Use the {@link PagingLdapConnectionImpl#wrap static factory method} that wraps the provided connection and returns a
 * proxy implementing {@link PagingLdapConnection}, which means that you can use it as an enhanced drop-in replacement of
 * the provided connection which is now able to execute {@link PagingLdapSearcher#search paged search requests}.</li>
 * </ul>
 */
@ThreadSafe
public class PagingLdapConnectionImpl implements PagingLdapSearcher, InvocationHandler {
    private final LDAPInterface _delegateConnection;

    /**
     * Wraps the provided {@link LDAPInterface} in order to make {@link PagingLdapSearcher} methods available.
     *
     * @param connection The connection to be wrapped.
     * @return A proxy wrapping the provided connection adding {@link PagingLdapSearcher} methods to it.
     */
    public static PagingLdapConnection wrap(LDAPInterface connection) {
        return (PagingLdapConnection) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{PagingLdapConnection.class},
                new PagingLdapConnectionImpl(connection));
    }

    /**
     * Creates an instance that is able to execute paging search requests using the provided LDAP connection.
     *
     * @param delegateConnection The connection to delegate to.
     */
    public PagingLdapConnectionImpl(LDAPInterface delegateConnection) {
        _delegateConnection = delegateConnection;
    }

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

    /**
     * @return The wrapped connection.
     */
    /*package*/ LDAPInterface getDelegateConnection() {
        return _delegateConnection;
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
