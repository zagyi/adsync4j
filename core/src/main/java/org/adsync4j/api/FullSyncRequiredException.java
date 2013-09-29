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

/**
 * Abstract base class for concrete exceptions representing specific error conditions during incremental synchronization.
 */
public abstract class FullSyncRequiredException extends LdapClientException {

    protected FullSyncRequiredException() {
    }

    protected FullSyncRequiredException(String message) {
        super(message);
    }
}
