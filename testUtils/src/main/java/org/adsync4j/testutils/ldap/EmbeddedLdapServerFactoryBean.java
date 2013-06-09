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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;

public class EmbeddedLdapServerFactoryBean implements FactoryBean<EmbeddedUnboundIDLdapServer> {

    private Integer _port;
    private List<String> _rootDNs = new ArrayList<>();
    private List<Resource[]> _schemas = new ArrayList<>();
    private List<Resource[]> _ldifs = new ArrayList<>();
    private Map<String, String> _bindCredentials;
    private boolean _includeStandardSchema;

    @Override
    public EmbeddedUnboundIDLdapServer getObject() throws Exception {
        EmbeddedUnboundIDLdapServer server = createEmbeddedUnboundIDLdapServer();

        for (Resource[] resources : _schemas) {
            for (Resource resource : resources) {
                try {
                    server.addSchema(resource.getFile());
                } catch (IOException e) {
                    server.addSchema(resourceToInputStream(resource));
                }

            }
        }

        server.setIncludeStandardSchema(_includeStandardSchema)
              .setRootDNs(_rootDNs)
              .setLdifs(resourceArrayListToInputStreamList(_ldifs));

        if (_port != null) { server.setPort(_port); }
        if (_bindCredentials != null) {
            server.setBindCredentials(_bindCredentials);
        }

        return server.init();
    }

    protected EmbeddedUnboundIDLdapServer createEmbeddedUnboundIDLdapServer() {
        return new EmbeddedUnboundIDLdapServer();
    }

    @Override
    public Class<?> getObjectType() {
        return EmbeddedUnboundIDLdapServer.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Iterable<InputStream> resourceArrayListToInputStreamList(List<Resource[]> ldifs) {
        return Iterables.concat(
                Lists.transform(ldifs, new Function<Resource[], List<InputStream>>() {
                    @Override
                    public List<InputStream> apply(Resource[] resources) {
                        return resourceArrayToInputStreamList(resources);
                    }
                }));
    }

    private static List<InputStream> resourceArrayToInputStreamList(Resource[] resources) {
        return Lists.transform(
                Arrays.asList(resources),
                new Function<Resource, InputStream>() {
                    @Override
                    public InputStream apply(@Nullable Resource resource) {
                        return resourceToInputStream(resource);
                    }
                });
    }

    private static InputStream resourceToInputStream(Resource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    public void setIncludeStandardSchema(boolean includeStandardSchema) {
        _includeStandardSchema = includeStandardSchema;
    }

    public void setRootDN(String rootDN) {
        _rootDNs = Lists.newArrayList(rootDN);
    }

    public void setRootDNs(List<String> rootDNs) {
        _rootDNs = rootDNs;
    }

    public void setSchemas(List<Resource[]> schema) {
        _schemas = schema;
    }

    public void setPort(Integer port) {
        _port = port;
    }

    public void setLdifs(List<Resource[]> ldifs) {
        _ldifs = ldifs;
    }

    public void setBindCredentials(Map<String, String> bindCredentials) {
        _bindCredentials = bindCredentials;
    }
}
