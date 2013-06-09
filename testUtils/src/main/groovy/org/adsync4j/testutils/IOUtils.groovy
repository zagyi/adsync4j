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
package org.adsync4j.testutils

import org.adsync4j.testutils.ldap.EmbeddedUnboundIDLdapServer
import org.codehaus.groovy.reflection.ReflectionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IOUtils {

    private final static Logger LOG = LoggerFactory.getLogger(EmbeddedUnboundIDLdapServer)

    static InputStream getResourceAsStreamOrFail(resourceLocation) {
        def clazz

        if (ReflectionUtils.isCallingClassReflectionAvailable()) {
             clazz = ReflectionUtils.callingClass
        } else {
            clazz = getClass()
            LOG.warn 'Could not determine class of caller --> can only resolve resources specified with full package path.'
        }

        def resourceStream = clazz.getResourceAsStream(resourceLocation)
        assert resourceStream, "couldn't find resource: $resourceLocation"
        resourceStream
    }
}