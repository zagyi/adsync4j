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
 *******************************************************************************/
package org.adsync4j.impl;

import com.google.common.collect.Lists;
import org.adsync4j.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static org.adsync4j.UUIDUtils.bytesToUUID;
import static org.adsync4j.impl.ActiveDirectorySyncServiceImpl.ActiveDirectoryAttribute.*;

@NotThreadSafe
public class ActiveDirectorySyncServiceImpl<DCA_KEY, LDAP_ATTRIBUTE> implements ActiveDirectorySyncService<LDAP_ATTRIBUTE> {

    //TODO: add logging statements
    //private final static Logger LOG = LoggerFactory.getLogger(ActiveDirectorySyncServiceImpl.class);

    protected static final String DELETED_OBJECTS_PATTERN = ":CN=Deleted Objects,";
    protected static final String DELETED_OBJECTS_CONTAINER_FORMULA = "<WKGUID=%s,%s>";

    protected final DCA_KEY _dcaKey;
    protected final SimpleRepository<DCA_KEY, DomainControllerAffiliation> _affiliationRepository;
    protected final LdapClient<LDAP_ATTRIBUTE> _ldapClient;
    protected final LdapAttributeResolver<LDAP_ATTRIBUTE> _attributeResolver;

    protected DomainControllerAffiliation _dcAffiliation;

    protected interface SyncOperation<LDAP_ATTRIBUTE> {
        void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor);
    }

    protected enum ActiveDirectoryAttribute {
        WELL_KNOWN_OBJECTS("wellKnownObjects"),
        INVOCATION_ID("invocationID"),
        DS_SERVICE_NAME("dsServiceName"),
        HIGHEST_COMMITTED_USN("highestCommittedUSN"),
        USN_CHANGED("uSNChanged"),
        USN_CREATED("uSNCreated");

        private final String _name;

        private ActiveDirectoryAttribute(String name) {
            _name = name;
        }

        public String key() {
            return _name;
        }

        public String toString() {
            return _name;
        }
    }

    public ActiveDirectorySyncServiceImpl(
            DCA_KEY dcaKey,
            SimpleRepository<DCA_KEY, DomainControllerAffiliation> affiliationRepository,
            LdapClient<LDAP_ATTRIBUTE> ldapClient)
            throws LdapClientException
    {
        _dcaKey = dcaKey;
        _affiliationRepository = affiliationRepository;
        _ldapClient = ldapClient;
        _attributeResolver = _ldapClient.getAttributeResolver();
    }

    @Override
    public long fullSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
        return doSync(entryProcessor, new SyncOperation<LDAP_ATTRIBUTE>() {
            @Override
            public void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
                String filter = getFilterWithUpperBoundUSN(_dcAffiliation.getSearchFilter(), remoteHighestCommittedUSN);

                Iterable<LDAP_ATTRIBUTE[]> searchResult = _ldapClient.search(
                        _dcAffiliation.getSyncBaseDN(), filter, _dcAffiliation.getAttributesToSync());

                for (LDAP_ATTRIBUTE[] entryAttributes : searchResult) {
                    entryProcessor.processNew(asList(entryAttributes));
                }

                _dcAffiliation.setInvocationId(retrieveInvocationId());
            }
        });
    }

    @Override
    public long incrementalSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
        return doSync(entryProcessor, new SyncOperation<LDAP_ATTRIBUTE>() {
            @Override
            public void execute(long remoteHighestCommittedUSN, EntryProcessor<LDAP_ATTRIBUTE> entryProcessor) {
                assertIncrementalSyncIsPossible();
                queryChangedAndNewEntries(entryProcessor, remoteHighestCommittedUSN);
                queryDeletedEntries(entryProcessor, remoteHighestCommittedUSN);
            }
        });
    }

    /**
     * Template method that implements a common frame of logic that has to be executed regardless of which specific sync
     * operation (full or incremental) is actually performed.
     *
     * @param entryProcessor
     * @param syncOperation
     * @return
     */
    private long doSync(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, SyncOperation<LDAP_ATTRIBUTE> syncOperation) {
        reloadAffiliation();
        long remoteHighestCommittedUSN = retrieveRemoteHighestCommittedUSN();

        syncOperation.execute(remoteHighestCommittedUSN, entryProcessor);

        _dcAffiliation.setHighestCommittedUSN(remoteHighestCommittedUSN);
        _affiliationRepository.save(_dcAffiliation);
        return remoteHighestCommittedUSN;
    }

    void reloadAffiliation() {
        _dcAffiliation = _affiliationRepository.load(_dcaKey);
        checkArgument(_dcAffiliation != null,
                "No Domain Controller Affiliation record is found in the repository with key: ", _dcaKey);
    }

    void assertIncrementalSyncIsPossible() {
        UUID expectedInvocationId = _dcAffiliation.getInvocationId();
        Long lastSeenHighestCommittedUSN = _dcAffiliation.getHighestCommittedUSN();

        boolean isAnyAffiliationDetailMissing = expectedInvocationId == null || lastSeenHighestCommittedUSN == null;

        if (isAnyAffiliationDetailMissing) {
            throw new InitialFullSyncRequiredException();
        }

        if (!equal(expectedInvocationId, retrieveInvocationId())) {
            throw new InvocationIdMismatchException();
        }
    }

    protected void queryChangedAndNewEntries(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, long upperBoundUSN) {
        String filter = getFilterWithLowerAndUpperBoundUSN(_dcAffiliation.getSearchFilter(), upperBoundUSN);
        Iterable<String> attributes = concat(asList(USN_CREATED.key()), _dcAffiliation.getAttributesToSync());

        Iterable<LDAP_ATTRIBUTE[]> searchResult = _ldapClient.search(_dcAffiliation.getSyncBaseDN(), filter, attributes);

        for (LDAP_ATTRIBUTE[] entry : searchResult) {
            feedEntryProcessor(entryProcessor, entry);
        }
    }

    private void feedEntryProcessor(EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, LDAP_ATTRIBUTE[] entry) {
        List<LDAP_ATTRIBUTE> entryWithoutUsnCreatedAttribute = asList(entry).subList(1, entry.length);
        if (isNewEntry(entry)) {
            entryProcessor.processNew(entryWithoutUsnCreatedAttribute);
        } else {
            entryProcessor.processChanged(entryWithoutUsnCreatedAttribute);
        }
    }

    private boolean isNewEntry(LDAP_ATTRIBUTE[] entry) {
        LDAP_ATTRIBUTE usnCreatedAttribute = entry[0];
        Long usnCreated = _attributeResolver.getAsLong(usnCreatedAttribute);
        return
                usnCreated != null &&
                usnCreated > _dcAffiliation.getHighestCommittedUSN();
    }

    protected void queryDeletedEntries(
            EntryProcessor<LDAP_ATTRIBUTE> entryProcessor, long upperBoundUSN) throws LdapClientException
    {
        String idOfDeletedObjectContainer = retrieveIdOfDeletedObjectContainer();

        String deletedObjectsContainer = String.format(
                DELETED_OBJECTS_CONTAINER_FORMULA, idOfDeletedObjectContainer, _dcAffiliation.getRootDN());

        String filter = getFilterWithLowerAndUpperBoundUSN(_dcAffiliation.getSearchDeletedObjectsFilter(), upperBoundUSN);

        Iterable<UUID> deletedObjectIds = _ldapClient.searchDeleted(deletedObjectsContainer, filter);

        for (UUID uuid : deletedObjectIds) {
            entryProcessor.processDeleted(uuid);
        }
    }

    protected UUID retrieveInvocationId() throws LdapClientException {
        LDAP_ATTRIBUTE dsServiceDNAttribute = _ldapClient.getRootDSEAttribute(DS_SERVICE_NAME.key());
        String dsServiceDN = _attributeResolver.getAsString(dsServiceDNAttribute);

        LdapClientException.throwIfNull(dsServiceDN,
                "Invalid %s attribute encountered: %s", DS_SERVICE_NAME.key(), String.valueOf(dsServiceDNAttribute));

        LDAP_ATTRIBUTE invocationIdAttribute = _ldapClient.getEntryAttribute(dsServiceDN, INVOCATION_ID.key());

        UUID invocationId = bytesToUUID(_attributeResolver.getAsByteArray(invocationIdAttribute));
        LdapClientException.throwIfNull(invocationId,
                "Invalid Update Sequence Number encountered: %s.", String.valueOf(invocationIdAttribute));

        return invocationId;
    }

    protected long retrieveRemoteHighestCommittedUSN() throws LdapClientException {
        LDAP_ATTRIBUTE hcusnAttribute = _ldapClient.getRootDSEAttribute(HIGHEST_COMMITTED_USN.key());
        Long hcusn = _attributeResolver.getAsLong(hcusnAttribute);
        LdapClientException.throwIfNull(hcusn,
                "Invalid Update Sequence Number encountered: %s.", String.valueOf(hcusnAttribute));
//      noinspection ConstantConditions
        return hcusn;
    }

    /**
     * Retrieves the GUID that identifies the {@code Deleted Objects Container} by inspecting the root entry
     * ({@link org.adsync4j.DomainControllerAffiliation#getRootDN()}) which is supposed to hold a number of values in its
     * {@code wellKnownObjects} attribute in the following format:
     * <p/>
     * {@code "B:32:18E2EA80684F11D2B9AA00C04F79F805:CN=<...>,DC=example,DC=com"}
     * <p/>
     * This method looks for the GUID in the 3rd field (considering ':' as field separator) of the record where {@code
     * CN=Deleted Objects}.
     *
     * @return The 3rd field of the record the 4th field of which starts with {@code "CN=Deleted Objects,"}
     *         (the record is split into fields on the ':' character).
     */
    protected String retrieveIdOfDeletedObjectContainer() throws LdapClientException {
        LDAP_ATTRIBUTE wellKnownObjectsAttribute =
                _ldapClient.getEntryAttribute(_dcAffiliation.getRootDN(), WELL_KNOWN_OBJECTS.key());

        List<String> wellKnownObjects = _attributeResolver.getAsStringList(wellKnownObjectsAttribute);

        for (String wellKnownObject : wellKnownObjects) {
            if (wellKnownObject.contains(DELETED_OBJECTS_PATTERN)) {
                return wellKnownObject.split(":")[2];
            }
        }
        throw new LdapClientException("Could not determine the GUID of the Deleted Objects container.");
    }

    protected String getFilterWithLowerAndUpperBoundUSN(String filter, long upperBoundUSN) {
        long lowerBoundUSN = _dcAffiliation.getHighestCommittedUSN();
        String lowerBoundUSNFilter = USN_CHANGED + ">=" + lowerBoundUSN;
        String upperBoundUSNFilter = USN_CHANGED + "<=" + upperBoundUSN;
        return and(filter, lowerBoundUSNFilter, upperBoundUSNFilter);
    }

    protected String getFilterWithUpperBoundUSN(String filter, long upperBoundUSN) {
        String usnUpperBoundFilter = USN_CHANGED + "<=" + upperBoundUSN;
        return and(filter, usnUpperBoundFilter);
    }

    protected static String and(String firstFilter, String secondFilter, String... otherFilters) {
        StringBuilder result = new StringBuilder("(&");
        for (String filter : Lists.asList(firstFilter, secondFilter, otherFilters)) {
            result.append(ensureWrappedInParenthesis(filter));
        }
        return result.append(')').toString();
    }

    private static StringBuilder ensureWrappedInParenthesis(String filter) {
        StringBuilder result = new StringBuilder(filter.length() + 2);
        if (filter.startsWith("(")) {
            return result.append(filter);
        } else {
            return result
                    .append('(')
                    .append(filter)
                    .append(')');
        }
    }

    public LdapAttributeResolver<LDAP_ATTRIBUTE> getAttributeResolver() {
        return _attributeResolver;
    }
}
