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
 ******************************************************************************/
package org.adsync4j.spi;

import org.adsync4j.api.LdapClientException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Interface defining lower level LDAP operations used by
 * {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl ActiveDirectorySyncServiceImpl}.
 * <p/>
 * Implementations are free to use any LDAP SDK available for Java to implement this interface. Clients will use the
 * {@link LdapAttributeResolver} returned by {@link LdapClient#getAttributeResolver()} to convert values of the LDAP SDK specific
 * attribute type to values of well known types like String, Long, etc.
 * <p/>
 * Implementations have to make sure that the connection is automatically re-opened if necessary.
 *
 * @param <LDAP_ATTRIBUTE> The LDAP attribute type defined in the SDK used to implement this interface.
 */
public interface LdapClient<LDAP_ATTRIBUTE> {

    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * ID of the LDAP request control used to retrieve tombstones (remnants of deleted entries).
     */
    public static final String SHOW_DELETED_CONTROL_OID = "1.2.840.113556.1.4.417";

    /**
     * Name of the attribute that uniquely identifies entries in Active Directory. Should be used when implementing {@link
     * LdapClient#searchDeleted(String, String)}.
     */
    public static final String OBJECT_GUID = "objectGUID";

    /**
     * Retrieves an attribute of the directory's root DSE.
     *
     * @param attributeName Name of one of the attributes of the directory's root DSE.
     * @return Value of the root DSE attribute.
     * @throws org.adsync4j.api.LdapClientException
     *          if the root DSE cannot be retrieved or the specified attribute does not exist.
     */
    @Nonnull
    LDAP_ATTRIBUTE getRootDSEAttribute(String attributeName) throws LdapClientException;

    /**
     * Retrieves an attribute of a specific entry.
     *
     * @param entryDN   Distinguished name of the entry to read.
     * @param attribute Name of an attribute of the specified entry.
     * @return Value of the attribute.
     * @throws LdapClientException if either the specified entry or its attribute is not found.
     */
    @Nonnull
    LDAP_ATTRIBUTE getEntryAttribute(String entryDN, String attribute) throws LdapClientException;

    /**
     * Performs a search operation with the given arguments.
     * <p/>
     * <b>Important! Implementers must make sure that:</b>
     * <ul>
     * <li>the returned attribute arrays contain <i>the same number of attributes in the same order</i> as specified by the
     * {@code attributes} input argument (insert {@code null} values if an attribute is not present on an entry)</li>
     * <li><i>every</i> directory entry that satisfies the search criteria is returned (use paging to avoid hitting the result
     * size limit of the server!)</li>
     * </ul>
     *
     * @param searchBaseDN The scope of the search operation will be the sub-tree starting from the node designated by the
     *                     Distinguished Name given here.
     * @param filter       LDAP filter expression to use when performing the search.
     * @param attributes   List of attribute names to retrieve in the result set.
     * @return A number of attribute arrays each of which represents one directory entry satisfying the search criteria. Each
     *         individual value in the returned arrays corresponds to the attribute in the same position in the {@code
     *         attributes} input argument.
     * @throws LdapClientException in case the LDAP communication failed for some reason.
     */
    @Nonnull
    Iterable<LDAP_ATTRIBUTE[]> search(String searchBaseDN, String filter, List<String> attributes) throws LdapClientException;

    /**
     * Performs a search operation that retrieves the {@code objectGUID} attribute of deleted entries (tombstones) which satisfy
     * the given search criteria.
     * <p/>
     * <b>Important! Implementers must make sure that:</b>
     * <ul>
     * <li>the search request contains the "show deleted" request control (see {@link LdapClient#SHOW_DELETED_CONTROL_OID})</li>
     * <li>the {@code objectGUID} attribute is correctly resolved to a {@link UUID} object (see {@link
     * org.adsync4j.impl.UUIDUtils#bytesToUUID(byte[]) UUIDUtils.bytesToUUID()})</li>
     * <li><i>every</i> directory entry that satisfies the search criteria is returned (use paging to avoid hitting the result
     * size limit of the server!)</li>
     * </ul>
     *
     * @param rootDN Root DN of the directory's domain (e.g. {@code DC=example,DC=com})
     * @param filter LDAP filter expression to use when searching for deleted objects.
     * @return The deleted objects' unique identifiers as {@link UUID} objects which the caller can iterate through.
     * @throws LdapClientException in case the LDAP communication failed for some reason.
     */
    @Nonnull
    Iterable<UUID> searchDeleted(String rootDN, String filter) throws LdapClientException;

    /**
     * @return An auxiliary function object that helps to interpret the LDAP attribute type specific to the LDAP SDK used by
     *         the implementation class.
     */
    @Nonnull
    LdapAttributeResolver<LDAP_ATTRIBUTE> getAttributeResolver();

    /**
     * Closes the underlying connection to the LDAP server.
     */
    void closeConnection();
}
