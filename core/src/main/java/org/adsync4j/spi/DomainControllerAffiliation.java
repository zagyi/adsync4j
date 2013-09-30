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

import java.util.List;
import java.util.UUID;

/**
 * Interface for objects that store all the information required to maintain a synchronization relationship with a domain
 * controller. It's basically a recipe by which synchronization operations work.
 * <p/>
 * It defines properties:
 * <ul>
 * <li>on how to access and identify the domain controller,</li>
 * <li>on what the scope of synchronization is,</li>
 * <li>and a {@link DomainControllerAffiliation#getHighestCommittedUSN marker} designating the point of time until which all
 * entries and changes has already been retrieved from the domain controller.</li>
 * </ul>
 */
public interface DomainControllerAffiliation {

    /**
     * @return The URL of the domain controller.
     */
    String getUrl();

    /**
     * @return The user name to authenticate with.
     */
    String getBindUser();

    /**
     * @return The password to use on authentication.
     */
    String getBindPassword();

    /**
     * @return DN of the root entry of the Active Directory tree (e.g. {@code DC=example,DC=com}).
     */
    String getRootDN();

    /**
     * @return Distinguished name of the directory node designating the sub-tree which will be the scope of synchronization (e.g.
     *         {@code CN=Users,DC=example,DC=com}).
     */
    String getSyncBaseDN();

    /**
     * @return LDAP filter expression that determines which entries should be retrieved during synchronization.
     */
    String getSearchFilter();

    /**
     * @return LDAP filter expression to use when searching for deleted entries during an incremental synchronization.
     */
    String getSearchDeletedObjectsFilter();

    /**
     * @return List of attributes to fetch when retrieving objects from Active Directory.
     */
    List<String> getAttributesToSync();

    /**
     * Invocation ID is a GUID that identifies the database on the server side. Technically, it's an attribute of a directory
     * entry pointed to by the 'dsServiceName' attribute of the root DSE. The affiliation record needs to store this ID,
     * in order to be able to recognize if it changes on the server side (as a result of restoring the server-side database).
     *
     * @return The invocation ID of the domain controller.
     * @see org.adsync4j.api.InvocationIdMismatchException InvocationIdMismatchException
     */
    UUID getInvocationId();

    /**
     * Active Directory maintains a counter called Update Sequence Number (USN) which is increased on every transaction that
     * changes, creates or deletes a directory entry. The current value of the USN is recorded in the entry subject of the
     * current transaction, and in the {@code highestCommittedUSN} attribute of the root DSE as well.<br>
     * ADSync4J uses the USN to track changes made since the last synchronization, therefore the affiliation record has to
     * store this number.
     *
     * @return The highest committed USN retrieved from Active Directory on the last synchronization,
     *         or null if no synchronization has been performed using this affiliation record.
     */
    Long getHighestCommittedUSN();

    /**
     * Setter for the Invocation ID. See the corresponding {@link DomainControllerAffiliation#getInvocationId()
     * getter} for a detailed description on what the Invocation ID is. Called by the
     * {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl syncrhronization service} after it retrieves the server's
     * Invocation ID during a {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl#fullSync full synchronization}.
     *
     * @param uuid The Invocation ID to set.
     * @return This {@link DomainControllerAffiliation} instance (returned to allow chaining the setters).
     */
    DomainControllerAffiliation setInvocationId(UUID uuid);

    /**
     * Setter for the highest committed USN. See the corresponding {@link DomainControllerAffiliation#getHighestCommittedUSN()
     * getter} for a detailed description on what the highest committed USN is. Called by the
     * {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl syncrhronization service} after it retrieves the server's
     * current highest committed USN at the start of a {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl#fullSync full}
     * or {@link org.adsync4j.impl.ActiveDirectorySyncServiceImpl#incrementalSync incremental} synchronization.
     *
     * @param hcusn The highest committed USN to set.
     * @return This {@link DomainControllerAffiliation} instance (returned to allow chaining the setters).
     */
    DomainControllerAffiliation setHighestCommittedUSN(Long hcusn);
}
