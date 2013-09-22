package org.adsync4j.unboundid;

import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;

import java.util.Collection;
import java.util.List;

/**
 * Abstract base class for classes decorating an {@link LDAPConnection}.
 */
abstract public class AbstractUnboundIDLdapConnectionDecorator implements UnboundIDLdapConnection {

    private final LDAPInterface _delegateConnection;

    public AbstractUnboundIDLdapConnectionDecorator(LDAPConnection delegateConnection) {
        _delegateConnection = delegateConnection;
    }

    /**
     * Made available only for unit testing purposes.
     */
    /*package*/ AbstractUnboundIDLdapConnectionDecorator(LDAPInterface delegateConnection) {
        _delegateConnection = delegateConnection;
    }

    // region delegating methods of LDAPConnection exposed by UnboundIDLdapConnection
    @Override
    public void close() {
        if (_delegateConnection instanceof LDAPConnection) {
            ((LDAPConnection) _delegateConnection).close();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void reconnect() throws LDAPException {
        if (_delegateConnection instanceof LDAPConnection) {
            ((LDAPConnection) _delegateConnection).reconnect();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isConnected() {
        if (_delegateConnection instanceof LDAPConnection) {
            return ((LDAPConnection) _delegateConnection).isConnected();
        } else {
            throw new UnsupportedOperationException();
        }
    }
    // endregion

    // region delegating methods of LDAPInterface
    @Override
    public RootDSE getRootDSE() throws LDAPException {return _delegateConnection.getRootDSE();}

    @Override
    public Schema getSchema() throws LDAPException {return _delegateConnection.getSchema();}

    @Override
    public Schema getSchema(String entryDN) throws LDAPException {return _delegateConnection.getSchema(entryDN);}

    @Override
    public SearchResultEntry getEntry(String dn) throws LDAPException {return _delegateConnection.getEntry(dn);}

    @Override
    public SearchResultEntry getEntry(String dn, String... attributes) throws LDAPException {
        return _delegateConnection.getEntry(dn, attributes);
    }

    @Override
    public LDAPResult add(String dn, Attribute... attributes) throws LDAPException {
        return _delegateConnection.add(dn, attributes);
    }

    @Override
    public LDAPResult add(String dn, Collection<Attribute> attributes) throws LDAPException {
        return _delegateConnection.add(dn, attributes);
    }

    @Override
    public LDAPResult add(Entry entry) throws LDAPException {return _delegateConnection.add(entry);}

    @Override
    public LDAPResult add(String... ldifLines) throws LDIFException, LDAPException {return _delegateConnection.add(ldifLines);}

    @Override
    public LDAPResult add(AddRequest addRequest) throws LDAPException {return _delegateConnection.add(addRequest);}

    @Override
    public LDAPResult add(ReadOnlyAddRequest addRequest) throws LDAPException {return _delegateConnection.add(addRequest);}

    @Override
    public CompareResult compare(String dn, String attributeName, String assertionValue) throws LDAPException {
        return _delegateConnection.compare(dn, attributeName, assertionValue);
    }

    @Override
    public CompareResult compare(CompareRequest compareRequest) throws LDAPException {
        return _delegateConnection.compare(compareRequest);
    }

    @Override
    public CompareResult compare(ReadOnlyCompareRequest compareRequest) throws LDAPException {
        return _delegateConnection.compare(compareRequest);
    }

    @Override
    public LDAPResult delete(String dn) throws LDAPException {return _delegateConnection.delete(dn);}

    @Override
    public LDAPResult delete(DeleteRequest deleteRequest) throws LDAPException {return _delegateConnection.delete(deleteRequest);}

    @Override
    public LDAPResult delete(ReadOnlyDeleteRequest deleteRequest) throws LDAPException {
        return _delegateConnection.delete(deleteRequest);
    }

    @Override
    public LDAPResult modify(String dn, Modification mod) throws LDAPException {return _delegateConnection.modify(dn, mod);}

    @Override
    public LDAPResult modify(String dn, Modification... mods) throws LDAPException {return _delegateConnection.modify(dn, mods);}

    @Override
    public LDAPResult modify(String dn, List<Modification> mods) throws LDAPException {
        return _delegateConnection.modify(dn, mods);
    }

    @Override
    public LDAPResult modify(String... ldifModificationLines) throws LDIFException, LDAPException {
        return _delegateConnection.modify(ldifModificationLines);
    }

    @Override
    public LDAPResult modify(ModifyRequest modifyRequest) throws LDAPException {return _delegateConnection.modify(modifyRequest);}

    @Override
    public LDAPResult modify(ReadOnlyModifyRequest modifyRequest) throws LDAPException {
        return _delegateConnection.modify(modifyRequest);
    }

    @Override
    public LDAPResult modifyDN(String dn, String newRDN, boolean deleteOldRDN) throws LDAPException {
        return _delegateConnection.modifyDN(dn, newRDN, deleteOldRDN);
    }

    @Override
    public LDAPResult modifyDN(String dn, String newRDN, boolean deleteOldRDN, String newSuperiorDN) throws LDAPException {
        return _delegateConnection.modifyDN(dn, newRDN, deleteOldRDN, newSuperiorDN);
    }

    @Override
    public LDAPResult modifyDN(ModifyDNRequest modifyDNRequest) throws LDAPException {
        return _delegateConnection.modifyDN(modifyDNRequest);
    }

    @Override
    public LDAPResult modifyDN(ReadOnlyModifyDNRequest modifyDNRequest) throws LDAPException {
        return _delegateConnection.modifyDN(modifyDNRequest);
    }

    @Override
    public SearchResult search(
            String baseDN, SearchScope scope, String filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(baseDN, scope, filter, attributes);}

    @Override
    public SearchResult search(
            String baseDN, SearchScope scope, Filter filter,
            String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(baseDN, scope, filter, attributes);}

    @Override
    public SearchResult search(
            SearchResultListener searchResultListener, String baseDN,
            SearchScope scope, String filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(searchResultListener, baseDN, scope, filter, attributes);}

    @Override
    public SearchResult search(
            SearchResultListener searchResultListener, String baseDN,
            SearchScope scope, Filter filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(searchResultListener, baseDN, scope, filter, attributes);}

    @Override
    public SearchResult search(
            String baseDN, SearchScope scope, DereferencePolicy derefPolicy,
            int sizeLimit, int timeLimit, boolean typesOnly, String filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(baseDN, scope, derefPolicy, sizeLimit, timeLimit, typesOnly, filter, attributes);}

    @Override
    public SearchResult search(
            String baseDN, SearchScope scope, DereferencePolicy derefPolicy,
            int sizeLimit, int timeLimit, boolean typesOnly, Filter filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.search(baseDN, scope, derefPolicy, sizeLimit, timeLimit, typesOnly, filter, attributes);}

    @Override
    public SearchResult search(
            SearchResultListener searchResultListener, String baseDN,
            SearchScope scope, DereferencePolicy derefPolicy, int sizeLimit,
            int timeLimit, boolean typesOnly, String filter, String... attributes) throws LDAPSearchException
    {
        return _delegateConnection
                .search(searchResultListener, baseDN, scope, derefPolicy, sizeLimit, timeLimit, typesOnly, filter, attributes);
    }

    @Override
    public SearchResult search(
            SearchResultListener searchResultListener, String baseDN,
            SearchScope scope, DereferencePolicy derefPolicy, int sizeLimit,
            int timeLimit, boolean typesOnly, Filter filter, String... attributes) throws LDAPSearchException
    {
        return _delegateConnection
                .search(searchResultListener, baseDN, scope, derefPolicy, sizeLimit, timeLimit, typesOnly, filter, attributes);
    }

    @Override
    public SearchResult search(SearchRequest searchRequest) throws LDAPSearchException {
        return _delegateConnection.search(searchRequest);
    }

    @Override
    public SearchResult search(ReadOnlySearchRequest searchRequest) throws LDAPSearchException {
        return _delegateConnection.search(searchRequest);
    }

    @Override
    public SearchResultEntry searchForEntry(
            String baseDN, SearchScope scope, String filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.searchForEntry(baseDN, scope, filter, attributes);}

    @Override
    public SearchResultEntry searchForEntry(
            String baseDN, SearchScope scope, Filter filter,
            String... attributes) throws LDAPSearchException
    {return _delegateConnection.searchForEntry(baseDN, scope, filter, attributes);}

    @Override
    public SearchResultEntry searchForEntry(
            String baseDN, SearchScope scope, DereferencePolicy derefPolicy,
            int timeLimit, boolean typesOnly, String filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.searchForEntry(baseDN, scope, derefPolicy, timeLimit, typesOnly, filter, attributes);}

    @Override
    public SearchResultEntry searchForEntry(
            String baseDN, SearchScope scope, DereferencePolicy derefPolicy,
            int timeLimit, boolean typesOnly, Filter filter, String... attributes) throws LDAPSearchException
    {return _delegateConnection.searchForEntry(baseDN, scope, derefPolicy, timeLimit, typesOnly, filter, attributes);}

    @Override
    public SearchResultEntry searchForEntry(SearchRequest searchRequest) throws LDAPSearchException {
        return _delegateConnection.searchForEntry(searchRequest);
    }

    @Override
    public SearchResultEntry searchForEntry(ReadOnlySearchRequest searchRequest) throws LDAPSearchException {
        return _delegateConnection.searchForEntry(searchRequest);
    }
    // endregion
}
