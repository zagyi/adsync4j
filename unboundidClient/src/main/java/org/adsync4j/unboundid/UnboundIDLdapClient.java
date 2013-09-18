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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.unboundid.ldap.sdk.*;
import org.adsync4j.LdapAttributeResolver;
import org.adsync4j.LdapClient;
import org.adsync4j.LdapClientException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.google.common.collect.Iterables.toArray;
import static org.adsync4j.UUIDUtils.bytesToUUID;

/**
 * This implementation of the {@link LdapClient} interface uses the UnboundID LDAP SDK to communicate with Active Directory.
 * The LDAP connection used by this class ensures that all search operations are paged without any further effort from the
 * client's side.
 */
public class UnboundIDLdapClient implements LdapClient<Attribute> {

    private final static XLogger LOG = XLoggerFactory.getXLogger(UnboundIDLdapClient.class);

    private final PagingUnboundIDConnectionFactory _connectionFactory;

    private int _pageSize = DEFAULT_PAGE_SIZE;
    private PagingLdapConnection _connection;

    public UnboundIDLdapClient(PagingUnboundIDConnectionFactory connectionFactory) {
        _connectionFactory = connectionFactory;
    }

    public void setPageSize(int pageSize) {
        _pageSize = pageSize;
    }

    @Nonnull
    @Override
    public Attribute getRootDSEAttribute(String attribute) throws LdapClientException {
        try {
            RootDSE rootDSE = getConnection().getRootDSE();
            LdapClientException.throwIfNull(rootDSE, "Root DSE not available.");

            Attribute rootDSEAttribute = rootDSE.getAttribute(attribute);
            LdapClientException.throwIfNull(rootDSEAttribute, "Could not retrieve attribute '%s' of the root DSE.", attribute);
            LOG.trace("Retrieved Root DSE attribute: {} = {}", attribute, rootDSEAttribute);

            return rootDSEAttribute;
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    @Nonnull
    @Override
    public Attribute getEntryAttribute(String entryDN, String attributeName) throws LdapClientException {
        try {
            SearchResultEntry entry = getConnection().getEntry(entryDN, attributeName);
            LdapClientException.throwIfNull(entry, "Missing entry '%s'", entryDN);
            Attribute attribute = entry.getAttribute(attributeName);
            LdapClientException.throwIfNull(attribute,
                    "Expected attribute '%s' on entry '%s' is missing.", entryDN, attributeName);
            return attribute;
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    @Nonnull
    @Override
    public Iterable<Attribute[]> search(
            String searchBaseDN, String filter, Iterable<String> attributes) throws LdapClientException
    {
        try {
            String[] attributeArray = toArray(attributes, String.class);

            SearchRequest searchRequest = new SearchRequest(
                    searchBaseDN,
                    SearchScope.SUB,
                    filter,
                    attributeArray);

            Iterable<SearchResultEntry> searchResult = getConnection().search(searchRequest, _pageSize);

            return resultEntriesToAttributeArrays(searchResult, attributeArray);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    /**
     * Transforms the provided series of search result entries into series of {@link Attribute} arrays that is guaranteed to
     * contain attribute values in the same number and order as the second argument of attribute names (the attribute array may
     * contain {@code null} values).
     *
     * @param searchResult A number of {@link SearchResultEntry} objects to transform.
     * @param attributes   Name of the attributes. Determines the number and order of attributes in the output.
     * @return A series of {@link Attribute} arrays, each array representing one search result entry.
     */
    private Iterable<Attribute[]> resultEntriesToAttributeArrays(
            Iterable<SearchResultEntry> searchResult, final String[] attributes)
    {
        return Iterables.transform(searchResult,
                new Function<SearchResultEntry, Attribute[]>() {
                    @Override
                    public Attribute[] apply(SearchResultEntry resultEntry) {
                        return ensureAttributeOrder(resultEntry, attributes);
                    }
                });
    }

    /**
     * Extracts {@link Attribute}s from the provided {@link SearchResultEntry} and returns them in the same order as the
     * attribute names are specified in the second argument. Some of the {@link Attribute} references may be null in the
     * returned array.
     */
    private Attribute[] ensureAttributeOrder(SearchResultEntry resultEntry, String[] attributes) {
        Attribute[] result = new Attribute[attributes.length];
        int i = 0;
        for (String attributeName : attributes) {
            Attribute attribute = resultEntry.getAttribute(attributeName);
            result[i++] = attribute;
        }
        return result;
    }

    @Nonnull
    @Override
    public Iterable<UUID> searchDeleted(String rootDN, String filter) throws LdapClientException {
        try {
            SearchRequest searchRequest = new SearchRequest(rootDN, SearchScope.SUB, filter, OBJECT_GUID);
            searchRequest.addControl(new Control(SHOW_DELETED_CONTROL_OID));

            Iterable<SearchResultEntry> searchResult = getConnection().search(searchRequest, _pageSize);

            return resultEntriesToUUIDs(searchResult);
        } catch (LDAPException e) {
            throw new LdapClientException(e);
        }
    }

    /**
     * Transforms the provided series of {@link SearchResultEntry} objects into a series of {@link UUID} objects. This method
     * assumed that the first attribute of each entry is a 16-byte long byte array (the {@code objectGUID} attribute)
     * representing the ID of the entry.
     */
    private Iterable<UUID> resultEntriesToUUIDs(Iterable<SearchResultEntry> searchResult) {
        return Iterables.transform(searchResult,
                new Function<SearchResultEntry, UUID>() {
                    @Override
                    public UUID apply(SearchResultEntry resultEntry) {
                        Attribute objectGuidAttribute = resultEntry.getAttributes().iterator().next();
                        UUID uuid = bytesToUUID(objectGuidAttribute.getValueByteArray());
                        if (uuid == null) {
                            LOG.error("Deleted object's objectGUID is expected to be a UUID encoded in 16 bytes, " +
                                      "but got: '{}'", objectGuidAttribute.getValue());
                        }
                        return uuid;
                    }
                });
    }

    @Nonnull
    @Override
    public LdapAttributeResolver<Attribute> getAttributeResolver() {
        return UnboundIdAttributeResolver.INSTANCE;
    }

    @Override
    public void closeConnection() {
        if (_connection != null) {
            _connectionFactory.closeConnection(_connection);
        }
    }

    private PagingLdapConnection getConnection() {
        if (_connection == null) {
            _connection = _connectionFactory.createConnection();
        }
        return _connectionFactory.ensureConnection(_connection);
    }
}
