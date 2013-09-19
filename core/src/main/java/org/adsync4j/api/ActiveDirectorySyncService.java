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
package org.adsync4j.api;

import org.adsync4j.spi.EntryProcessor;

/**
 * Main service interface of ADSync4J.
 * <p/>
 * Implementations have to consult a {@link org.adsync4j.spi.DomainControllerAffiliation DomainControllerAffiliation} record in
 * order to get the details based on which the synchronization operations are to be carried out.
 * <p/>
 * For more on the {@code LDAP_ATTRIBUTE} type parameter please refer to the documentation of {@link
 * org.adsync4j.spi.LdapClient LdapClient} and {@link org.adsync4j.spi.LdapAttributeResolver LdapAttributeResolver}.
 *
 * @param <LDAP_ATTRIBUTE> An LDAP SDK specific attribute type. Determined by the {@link org.adsync4j.spi.LdapClient LdapClient}
 *                         implementation in use.
 */
public interface ActiveDirectorySyncService<LDAP_ATTRIBUTE> {

    /**
     * Performs a full synchronization that retrieves all entries currently found in the synchronization scope in Active
     * Directory.<br>
     * Entries are reported to the caller by iteratively invoking {@link org.adsync4j.spi.EntryProcessor#processNew
     * processNew()} of the provided {@link org.adsync4j.spi.EntryProcessor}.
     *
     * @param entryProcessor {@link org.adsync4j.spi.EntryProcessor} implementation provided by the caller in order to receive
     *                       the synchronized
     *                       entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException if the LDAP client encounters a problem during synchronization.
     */
    long fullSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) throws LdapClientException;


    /**
     * Performs an incremental synchronization that only retrieves entries created/changed/deleted after the point of time
     * represented by the highest committed Update Sequence Number returned by the last synchronization.<br>
     * Entries are reported to the caller by iteratively invoking the corresponding methods of the provided
     * {@link EntryProcessor}.
     *
     * @param entryProcessor {@link EntryProcessor} implementation provided by the caller in order to receive the synchronized
     *                       entries.
     * @return The current highest committed Update Sequence Number on the server side that represents the point of time from
     *         which the next incremental synchronization will have to retrieve changes from Active Directory.
     * @throws LdapClientException if the LDAP client encounters a problem during synchronization.
     */
    long incrementalSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) throws LdapClientException;
}