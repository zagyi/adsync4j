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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.adsync4j.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Objects.equal;
import static org.adsync4j.UUIDUtils.bytesToUUID;
import static org.adsync4j.impl.ActiveDirectorySyncServiceImpl.ActiveDirectoryAttribute.*;
import static java.util.Arrays.asList;

//TODO: add logging statements
public class ActiveDirectorySyncServiceImpl<T_ATTRIBUTE> implements ActiveDirectorySyncService<T_ATTRIBUTE> {

    //private final static Logger LOG = LoggerFactory.getLogger(ActiveDirectorySyncServiceImpl.class);

    protected static final String DELETED_OBJECTS_PATTERN = ":CN=Deleted Objects,";
    protected static final String DELETED_OBJECTS_CONTAINER_FORMULA = "<WKGUID=%s,%s>";

    protected final LdapClient<T_ATTRIBUTE> _ldapClient;
    protected final DomainControllerAffiliation _dcAffiliation;

    protected final List<String> _incrementalSyncAttributes;
    protected final LdapAttributeResolver<T_ATTRIBUTE> _attributeResolver;

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
            LdapClient<T_ATTRIBUTE> ldapClient,
            DomainControllerAffiliation affiliation) throws LdapClientException
    {
        _dcAffiliation = new ImmutableDomainControllerAffiliation(affiliation);
        _ldapClient = ldapClient;
        _attributeResolver = _ldapClient.getAttributeResolver();

        _incrementalSyncAttributes = ImmutableList.<String>builder()
                                                  .add(USN_CREATED.key())
                                                  .addAll(affiliation.getAttributesToSync())
                                                  .build();
    }

    @Override
    public boolean isIncrementalSyncPossible() throws LdapClientException {
        UUID expectedInvocationId = _dcAffiliation.getInvocationId();
        Long lastSeenHighestCommittedUSN = _dcAffiliation.getHighestCommittedUSN();
        if (expectedInvocationId == null || lastSeenHighestCommittedUSN == null) {
            return false;
        } else {
            UUID actualInvocationId = retrieveInvocationId();
            return equal(actualInvocationId, expectedInvocationId);
        }
    }

    @Override
    public long fullSync(EntryProcessor<T_ATTRIBUTE> entryProcessor) throws LdapClientException {
        long remoteHighestCommittedUSN = retrieveRemoteHighestCommittedUSN();
        String filter = getFilterWithUpperBoundUSN(_dcAffiliation.getSearchFilter(), remoteHighestCommittedUSN);
        Iterable<T_ATTRIBUTE[]> searchResult = _ldapClient.search(
                _dcAffiliation.getSyncBaseDN(), filter, _dcAffiliation.getAttributesToSync());

        for (T_ATTRIBUTE[] attributes : searchResult) {
            entryProcessor.processNew(Arrays.asList(attributes));
        }

        return remoteHighestCommittedUSN;
    }

    @Override
    public long incrementalSync(EntryProcessor<T_ATTRIBUTE> entryProcessor) throws LdapClientException {
        long upperBoundUSN = retrieveRemoteHighestCommittedUSN();
        queryChangedAndNewEntries(entryProcessor, upperBoundUSN);
        queryDeletedEntries(entryProcessor, upperBoundUSN);
        return upperBoundUSN;
    }

    protected long queryChangedAndNewEntries(
            EntryProcessor<T_ATTRIBUTE> entryProcessor, long upperBoundUSN) throws LdapClientException
    {
        String filter = getFilterWithLowerAndUpperBoundUSN(_dcAffiliation.getSearchFilter(), upperBoundUSN);
        Iterable<T_ATTRIBUTE[]> searchResult = _ldapClient.search(
                _dcAffiliation.getSyncBaseDN(), filter, _incrementalSyncAttributes);

        for (T_ATTRIBUTE[] entry : searchResult) {
            feedEntryProcessor(entryProcessor, entry);
        }

        return upperBoundUSN;
    }

    private void feedEntryProcessor(EntryProcessor<T_ATTRIBUTE> entryProcessor, T_ATTRIBUTE[] entry) {
        List<T_ATTRIBUTE> entryWithoutUsnCreatedAttribute = asList(entry).subList(1, _incrementalSyncAttributes.size());
        if (isNewEntry(entry)) {
            entryProcessor.processNew(entryWithoutUsnCreatedAttribute);
        } else {
            entryProcessor.processChanged(entryWithoutUsnCreatedAttribute);
        }
    }

    private boolean isNewEntry(T_ATTRIBUTE[] entry) {
        T_ATTRIBUTE usnCreatedAttribute = entry[0];
        Long usnCreated = _attributeResolver.getAsLong(usnCreatedAttribute);
        return
                usnCreated != null &&
                usnCreated > _dcAffiliation.getHighestCommittedUSN();
    }

    protected void queryDeletedEntries(
            EntryProcessor<T_ATTRIBUTE> entryProcessor, long upperBoundUSN) throws LdapClientException
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
        T_ATTRIBUTE dsServiceDNAttribute = _ldapClient.getRootDSEAttribute(DS_SERVICE_NAME.key());
        String dsServiceDN = _attributeResolver.getAsString(dsServiceDNAttribute);

        LdapClientException.throwIfNull(dsServiceDN,
                "Invalid %s attribute encountered: %s", DS_SERVICE_NAME.key(), String.valueOf(dsServiceDNAttribute));

        T_ATTRIBUTE invocationIdAttribute = _ldapClient.getEntryAttribute(dsServiceDN, INVOCATION_ID.key());

        UUID invocationId = bytesToUUID(_attributeResolver.getAsByteArray(invocationIdAttribute));
        LdapClientException.throwIfNull(invocationId,
                "Invalid Update Sequence Number encountered: %s.", String.valueOf(invocationIdAttribute));

        return invocationId;
    }

    protected long retrieveRemoteHighestCommittedUSN() throws LdapClientException {
        T_ATTRIBUTE hcusnAttribute = _ldapClient.getRootDSEAttribute(HIGHEST_COMMITTED_USN.key());
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
        T_ATTRIBUTE wellKnownObjectsAttribute =
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

    public LdapAttributeResolver<T_ATTRIBUTE> getAttributeResolver() {
        return _attributeResolver;
    }
}
