package org.adsync4j.testutils.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;

import java.util.Map;

public class InMemoryDirectoryServerConfigWithRootDSEAttributes extends InMemoryDirectoryServerConfig {

    private Map<String, String> _rootDSEAttributes;

    public InMemoryDirectoryServerConfigWithRootDSEAttributes(String... baseDNs) throws LDAPException {
        super(baseDNs);
    }

    public InMemoryDirectoryServerConfigWithRootDSEAttributes(DN... baseDNs) throws LDAPException {
        super(baseDNs);
    }

    public InMemoryDirectoryServerConfigWithRootDSEAttributes(InMemoryDirectoryServerConfig cfg) {
        super(cfg);
    }

    public void setRootDSEAttributes(Map<String, String> rootDSEAttributes) {
        _rootDSEAttributes = rootDSEAttributes;
    }

    public Map<String, String> getRootDSEAttributes() {
        return _rootDSEAttributes;
    }
}
