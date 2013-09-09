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

import org.codehaus.groovy.reflection.ReflectionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestUtils {
    private final static Logger LOG = LoggerFactory.getLogger(TestUtils)

    /**
     *
     * @param resourceReference Reference to a resource either as a full-blown URL, or just classpath reference
     *                          like "/com/package/resource.xml".
     * @return A URL object pointing to the referenced resource.
     */
    static URL getResourceURL(def resourceReference) {
        URL resource = getClass().getResource(resourceReference)

        if (!resource) {
            resource = new URL(resourceReference)
        }

        assert resource, "could not resolve $resourceReference to a classpath resource, and it doesn't seem to be a URL"

        resource
    }

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

    static byte[] uuidToBytArray(UUID uuid) {
        byte[] bytes = new byte[16]

        long msb = uuid.getMostSignificantBits()
        bytes[6] = (byte) (msb & 0xff)
        bytes[7] = (byte) (msb >> 8 & 0xff)
        bytes[4] = (byte) (msb >> 16 & 0xff)
        bytes[5] = (byte) (msb >> 24 & 0xff)
        bytes[0] = (byte) (msb >> 32 & 0xff)
        bytes[1] = (byte) (msb >> 40 & 0xff)
        bytes[2] = (byte) (msb >> 48 & 0xff)
        bytes[3] = (byte) (msb >> 56 & 0xff)

        long lsb = uuid.getLeastSignificantBits()
        bytes[15] = (byte) (lsb & 0xff)
        bytes[14] = (byte) (lsb >> 8 & 0xff)
        bytes[13] = (byte) (lsb >> 16 & 0xff)
        bytes[12] = (byte) (lsb >> 24 & 0xff)
        bytes[11] = (byte) (lsb >> 32 & 0xff)
        bytes[10] = (byte) (lsb >> 40 & 0xff)
        bytes[9] = (byte) (lsb >> 48 & 0xff)
        bytes[8] = (byte) (lsb >> 56 & 0xff)

        return bytes;
    }
}
