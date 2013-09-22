package org.adsync4j.unboundid;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;

/**
 * Interface exposing methods of {@link com.unboundid.ldap.sdk.LDAPConnection LDAPConnection} that {@link UnboundIDLdapClient}
 * needs, but that are not exposed by {@link LDAPInterface}.
 */
public interface UnboundIDLdapConnection extends LDAPInterface {

    /**
     * Unbinds from the server and closes the connection to the LDAP server.
     */
    void close();

    /**
     * Indicates whether this connection is currently established.
     *
     * @return {@code true} if this connection is currently established, or
     *         {@code false} if it is not.
     */
    public boolean isConnected();

    /**
     * Attempts to re-establish a connection to the server and re-authenticate if appropriate.
     *
     * @throws LDAPException If a problem occurs while attempting to re-connect or re-authenticate.
     */
    public void reconnect() throws LDAPException;
}
