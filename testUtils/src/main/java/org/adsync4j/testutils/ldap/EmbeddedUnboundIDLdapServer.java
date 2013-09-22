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
package org.adsync4j.testutils.ldap;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.LDAPSDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static java.util.Objects.requireNonNull;

/**
 * Utility class that makes it easy to start an embedded LDAP server. It's basically a lightweight wrapper around UnboundID's
 * {@link com.unboundid.ldap.listener.InMemoryDirectoryServer}.
 */
@NotThreadSafe
public class EmbeddedUnboundIDLdapServer {

    private final static Logger LOG = LoggerFactory.getLogger(EmbeddedUnboundIDLdapServer.class);
    private final static boolean CLOSE_EXISTING_CONNECTIONS_ON_SHUTDOWN = true;
    private final static boolean CLEAR_BEFORE_LDIF_IMPORT = false;

    @Nullable
    private Integer _port;
    private List<String> _rootDNs = new ArrayList<>();
    private List<SchemaFileSupplier> _schemas = new ArrayList<>();
    private List<InputStream> _ldifs = new ArrayList<>();
    private Map<String, String> _bindCredentials = new HashMap<>();
    private boolean _includeStandardSchema = false;

    private boolean _initialized;
    private InMemoryDirectoryServer _server;


    @PostConstruct
    public EmbeddedUnboundIDLdapServer init() {
        assertUninitialized();

        try {
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(
                    _rootDNs.toArray(new String[_rootDNs.size()]));

            initSchema(config);
            initPort(config);
            initBindCredentials(config);

            _server = createInMemoryDirectoryServer(config);
            _server.startListening();

            loadLdifs(_server);

            LOG.debug("LDAP Server is up and listening on: {}", getAddress());

            _initialized = true;
        } catch (LDAPSDKException | IOException e) {
            throw propagate(e);
        }

        return this;
    }

    protected InMemoryDirectoryServer createInMemoryDirectoryServer(InMemoryDirectoryServerConfig config) throws LDAPException {
        return new InMemoryDirectoryServer(config);
    }

    @PreDestroy
    public void shutDown() {
        if (_initialized) {
            _server.shutDown(CLOSE_EXISTING_CONNECTIONS_ON_SHUTDOWN);
        } else {
            LOG.warn("shutDown() called on an uninitialized instance.");
        }
    }

    public LDAPConnection getConnection() throws LDAPException {
        return _server.getConnection();
    }

    private void initSchema(InMemoryDirectoryServerConfig config) throws LDAPSDKException, IOException {
        if (_schemas.size() > 0) {
            config.setSchema(Schema.mergeSchemas(getSchemasInArray()));
        }
    }

    private Schema[] getSchemasInArray() throws LDAPSDKException, IOException {
        Schema[] schemaArray;
        int i = 0;

        if (_includeStandardSchema) {
            schemaArray = new Schema[_schemas.size() + 1];
            schemaArray[i++] = Schema.getDefaultStandardSchema();
        } else {
            schemaArray = new Schema[_schemas.size()];
        }

        for (SchemaFileSupplier schemaFileSupplier : _schemas) {
            File schemaFile = schemaFileSupplier.getFile();
            try {
                Schema schema = Schema.getSchema(schemaFile);
                schemaArray[i++] = schema;
                if (schemaFileSupplier.isBackedByTempFile()) {
                    schemaFile.deleteOnExit();
                }
            } catch (LDIFException e) {
                throw new RuntimeException("Could not load schema file: " + schemaFile.getAbsolutePath(), e);
            }
        }

        return schemaArray;
    }

    private void initPort(InMemoryDirectoryServerConfig config) {
        if (_port != null) {
            try {
                InMemoryListenerConfig listenerConfig =
                        new InMemoryListenerConfig("default listener", null, _port, null, null, null);
                config.setListenerConfigs(listenerConfig);
                LOG.debug("Successfully configured listener on port: {}", _port);
            } catch (LDAPException e) {
                throw propagate(e);
            }
        }
    }

    private void initBindCredentials(InMemoryDirectoryServerConfig config) {
        for (Map.Entry<String, String> bindCredentialEntry : _bindCredentials.entrySet()) {
            String user = bindCredentialEntry.getKey();
            String pwd = bindCredentialEntry.getValue();
            try {
                config.addAdditionalBindCredentials(user, pwd);
            } catch (LDAPException e) {
                throw propagate(e);
            }
        }
    }

    private void loadLdifs(InMemoryDirectoryServer ds) {
        try {
            for (InputStream ldif : _ldifs) {
                LDIFReader ldifReader = new LDIFReader(ldif);
                ds.importFromLDIF(CLEAR_BEFORE_LDIF_IMPORT, ldifReader);
            }
        } catch (LDAPException e) {
            throw propagate(e);
        }
    }


    private int obtainPort() {
        int port = _server.getListenPort();
        boolean isListenerActive = port != -1;

        checkState(isListenerActive, "Listener has not yet been actived.");

        _port = port;
        return port;
    }

    private static class SchemaFileSupplier {
        private final File _file;
        private final boolean _isBackedByTempFile;

        public SchemaFileSupplier(File file) {
            _file = file;
            _isBackedByTempFile = false;
        }

        public SchemaFileSupplier(InputStream inputStream) {
            _file = dumpStreamToTempFile(inputStream);
            _isBackedByTempFile = true;
        }

        public File getFile() {
            return _file;
        }

        private boolean isBackedByTempFile() {
            return _isBackedByTempFile;
        }

        private static File dumpStreamToTempFile(InputStream inputStream) {
            try {
                File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                File schemaTempFile = File.createTempFile("unboundid-schema", "tmp", tmpDir);
                ByteStreams.copy(inputStream, Files.newOutputStreamSupplier(schemaTempFile));
                schemaTempFile.deleteOnExit();
                inputStream.close();
                return schemaTempFile;
            } catch (IOException e) {
                throw propagate(e);
            }
        }

    }

    private void assertUninitialized() {
        checkState(!_initialized, "Instance already initialized.");
    }

    //region ############## getters ##############
    public int getPort() {
        return _port == null ? obtainPort() : _port;
    }

    public String getAddress() {
        return "ldap://127.0.0.1:" + getPort();
    }

    public List<String> getRootDNs() {
        return _rootDNs;
    }

    public boolean isInitialized() {
        return _initialized;
    }
    //endregion

    //region ############## setters/adders ##############
    public EmbeddedUnboundIDLdapServer setPort(int port) {
        assertUninitialized();
        _port = port;
        return this;
    }

    public EmbeddedUnboundIDLdapServer setLdifs(Iterable<InputStream> ldifs) {
        assertUninitialized();
        requireNonNull(ldifs);
        _ldifs = Lists.newArrayList(ldifs);
        return this;
    }

    public EmbeddedUnboundIDLdapServer addLdif(InputStream ldif) {
        assertUninitialized();
        requireNonNull(ldif);
        _ldifs.add(ldif);
        return this;
    }

    public EmbeddedUnboundIDLdapServer setSchemaFiles(Iterable<File> schemaFiles) {
        assertUninitialized();
        requireNonNull(schemaFiles);
        _schemas = new ArrayList<>();
        for (File schemaFile : schemaFiles) {
            _schemas.add(new SchemaFileSupplier(schemaFile));
        }
        return this;
    }

    public EmbeddedUnboundIDLdapServer setSchemaStreams(Iterable<InputStream> schemasStreams) {
        assertUninitialized();
        requireNonNull(schemasStreams);
        _schemas = new ArrayList<>();
        for (InputStream schemasStream : schemasStreams) {
            _schemas.add(new SchemaFileSupplier(schemasStream));
        }
        return this;
    }

    public EmbeddedUnboundIDLdapServer addSchema(InputStream schema) {
        assertUninitialized();
        requireNonNull(schema);
        _schemas.add(new SchemaFileSupplier(schema));
        return this;
    }

    public EmbeddedUnboundIDLdapServer addSchema(File schema) {
        assertUninitialized();
        requireNonNull(schema);
        _schemas.add(new SchemaFileSupplier(schema));
        return this;
    }

    public EmbeddedUnboundIDLdapServer setBindCredentials(Map<String, String> bindCredentials) {
        assertUninitialized();
        requireNonNull(bindCredentials);
        _bindCredentials = bindCredentials;
        return this;
    }

    public EmbeddedUnboundIDLdapServer addBindCredentials(String bindUser, String bindPassword) {
        assertUninitialized();
        requireNonNull(bindUser);
        requireNonNull(bindPassword);
        _bindCredentials.put(bindUser, bindPassword);
        return this;
    }

    public EmbeddedUnboundIDLdapServer setRootDN(String rootDN) {
        assertUninitialized();
        requireNonNull(rootDN);
        _rootDNs = Lists.newArrayList(rootDN);
        return this;
    }

    public EmbeddedUnboundIDLdapServer setRootDNs(String firstRootDN, String... restRootDNs) {
        assertUninitialized();
        requireNonNull(firstRootDN);
        requireNonNull(restRootDNs);
        _rootDNs = Lists.newArrayList(Lists.asList(firstRootDN, restRootDNs));
        return this;
    }

    public EmbeddedUnboundIDLdapServer setRootDNs(List<String> rootDNs) {
        assertUninitialized();
        requireNonNull(rootDNs);
        _rootDNs = rootDNs;
        return this;
    }

    public EmbeddedUnboundIDLdapServer addRootDN(String rootDN) {
        assertUninitialized();
        requireNonNull(rootDN);
        _rootDNs.add(rootDN);
        return this;
    }

    public EmbeddedUnboundIDLdapServer setIncludeStandardSchema(boolean includeStandardSchema) {
        assertUninitialized();
        _includeStandardSchema = includeStandardSchema;
        return this;
    }

    public EmbeddedUnboundIDLdapServer includeStandardSchema() {
        assertUninitialized();
        _includeStandardSchema = true;
        return this;
    }
    //endregion
}
