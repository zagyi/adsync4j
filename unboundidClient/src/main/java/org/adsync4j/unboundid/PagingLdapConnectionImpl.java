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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link PagingLdapConnection} that is basically a decorator around {@link LDAPConnection} which can execute
 * paged search requests.
 */
@ThreadSafe
public class PagingLdapConnectionImpl extends AbstractUnboundIDLdapConnectionDecorator implements PagingLdapConnection {

    private final static Logger LOG = LoggerFactory.getLogger(PagingLdapConnectionImpl.class);

    /**
     * Creates an instance that is able to execute paging search requests using the provided LDAP connection.
     *
     * @param delegateConnection The connection to delegate to.
     */
    public PagingLdapConnectionImpl(UnboundIDLdapConnection delegateConnection) {
        super(delegateConnection);
    }

    /**
     * Made available only for unit testing purposes.
     */
    /*package*/ PagingLdapConnectionImpl(LDAPInterface delegateConnection) {
        super(delegateConnection);
    }

    @Override
    public Iterable<SearchResultEntry> search(final SearchRequest searchRequest, final int pageSize) throws LDAPException {
        searchRequest.replaceControl(new SimplePagedResultsControl(pageSize, null));

        LOG.debug("Requesting first page of results for search request: {}", searchRequest);
        final SearchResult firstPage = search(searchRequest);

        Iterable<List<SearchResultEntry>> pages =
                new Iterable<List<SearchResultEntry>>() {
                    @Override
                    public Iterator<List<SearchResultEntry>> iterator() {
                        return new PagingSearchIterator(PagingLdapConnectionImpl.this, searchRequest, firstPage);
                    }
                };
        return Iterables.concat(pages);
    }
}
