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
import java.util.UUID;

/**
 * Interface that defines all operations an LDAP client implementation needs to provide, so that it can be used by
 * {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl ActiveDirectorySyncServiceImpl}.
 * <p/>
 * Implementations are free to use any LDAP SDK available for Java to implement this interface. Each of these SDKs define a
 * specific type to represent an LDAP attribute (e.g. it's {@code javax.naming.directory.Attribute} in case of JNDI,
 * or {@code com.unboundid.ldap.sdk.Attribute} in case of the UnboundID LDAP SDK, etc). However,
 * callers of this interface can not possibly be prepared to deal with all the different attribute types. In order to be
 * able to leave the decision to the implementations which LDAP SDK to use, some indirection had to be introduced here.
 * Therefore a type parameter is defined for the SDK specific attribute type, and implementations will have to provide an
 * accompanying class that can resolve the SDK specific attribute to well known types like String, Long,
 * etc. (see {@link LdapClient#getAttributeResolver()}).
 * <p/>
 * Once the synchronization operation is finished, the service will call {@link LdapClient#closeConnection closeConnection()}
 * to release any resources associated with the connection. Implementations must make sure that the connection is automatically
 * re-opened if any of the interface's methods is invoked after a call to {@link LdapClient#closeConnection closeConnection()}.
 *
 * @param <LDAP_ATTRIBUTE> The LDAP attribute type defined in the SDK with the help of which this interface was implemented.
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
     * Retrieves some metadata from the Active Directory server by reading an attribute of its root entry (root DSE).
     *
     * @param attributeName Name of the attribute contained in the root DSE.
     * @return Value of the root DSE attribute.
     * @throws org.adsync4j.api.LdapClientException
     *          if the root DSE cannot be retrieved or the specified attribute does not exist.
     */
    @Nonnull
    LDAP_ATTRIBUTE getRootDSEAttribute(String attributeName) throws LdapClientException;

    /**
     * Retrieves an attribute of a specific entry.
     *
     * @param entryDN   Distinguished Name of the entry to read.
     * @param attribute Name of the attribute contained in the specified entry.
     * @return Value of the attribute.
     * @throws LdapClientException if either the specified entry or its attribute is not found.
     */
    @Nonnull
    LDAP_ATTRIBUTE getEntryAttribute(String entryDN, String attribute) throws LdapClientException;

    /**
     * Performs a search operation with the given arguments.
     * <p/>
     * <b>Important!</b> Implementations must make sure that the attribute values in the returned attribute arrays have the same
     * number of attributes <i>and</i> in the same order as the input attribute name list (insert {@code null} value in case
     * an attribute is not present in an entry).
     *
     * @param searchBaseDN The scope of the search operation will be the sub-tree starting from the node designated by the
     *                     Distinguished Name given here.
     * @param filter       LDAP filter expression to use when performing the search.
     * @param attributes   List of attribute names to retrieve in the result set.
     * @return A number of attribute arrays that the caller can iterate trough. Each array represents one entry in the directory.
     *         The length of returned arrays and the order of the attribute values in it is the same as that of the input
     *         list of attribute names.
     * @throws LdapClientException in case the search operation failed for any reason.
     */
    @Nonnull
    Iterable<LDAP_ATTRIBUTE[]> search(String searchBaseDN, String filter, Iterable<String> attributes) throws LdapClientException;

    /**
     * Performs a search operation that retrieves the {@code objectGUID} attribute of deleted entries (tombstones) which satisfy
     * the given filter expression.
     * <p/>
     * <b>Important!</b> Implementations must make sure that  the search request contains the request control with the OID
     * defined in {@link LdapClient#SHOW_DELETED_CONTROL_OID}.
     * <p/>
     * Implementations can use the helper method {@link org.adsync4j.impl.UUIDUtils#bytesToUUID(byte[]) UUIDUtils.bytesToUUID()}
     * to correctly decode the {@code objectGUID} value (a 16-byte long byte array) as a {@link UUID} object.
     *
     * @param rootDN Root DN of the directory's domain (e.g. {@code DC=example,DC=com})
     * @param filter LDAP filter expression to use when searching for deleted objects.
     * @return The deleted objects' unique identifiers as {@link UUID} objects which the caller can iterate through.
     * @throws LdapClientException in case the search operation failed for any reason.
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
