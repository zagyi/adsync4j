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

import com.google.common.collect.ImmutableSet;
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
import java.util.Set;

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

    private static final Set<Method> PAGING_LDAP_SEARCHER_METHODS =
            ImmutableSet.copyOf(PagingLdapSearcher.class.getDeclaredMethods());

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

    @Override
    public LDAPInterface getDelegateConnection() {
        return _delegateConnection;
    }

    /**
     * Invocation handler that dispatches method calls to the delegate connection,
     * unless it's a call to {@link PagingLdapSearcher} that is dispatched to {@code this}.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (PAGING_LDAP_SEARCHER_METHODS.contains(method)) {
            return invokeThrowingUnwrappedException(this, method, args);
        } else {
            return invokeThrowingUnwrappedException(_delegateConnection, method, args);
        }
    }

    /**
     * Indirect method calls (done through reflection) may throw a {@link InvocationTargetException} that wraps the original
     * exception which might have been thrown by the indirectly invoked method. Invocation handlers ({@link
     * InvocationHandler#invoke InvocationHandler.invoke()}) of a dynamic {@link Proxy} must unwrap the original exception before
     * rethrowing it, because carelessly rethrowing the wrapper {@link InvocationTargetException} will result in a
     * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}. That's because the {@link
     * InvocationTargetException} is a checked exception which is normally not declared in the throws clause of the proxied
     * methods.
     * <p/>
     * This helper method performs an indirect method call, and re-throws the <b>unwrapped</b> original exception if the
     * invocation of the target method failed.
     *
     * @param object The object to dispatch the method call to.
     * @param method The method to invoke on the object.
     * @param args   Arguments used for the method call.
     * @return The result of the indirect method invocation.
     * @throws Throwable Any exception that the target method might have thrown.
     */
    private static Object invokeThrowingUnwrappedException(Object object, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
