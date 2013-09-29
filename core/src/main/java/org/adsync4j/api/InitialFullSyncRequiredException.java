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
 * Exception thrown on incremental synchronization in case the {@link org.adsync4j.spi.DomainControllerAffiliation affiliation
 * record} does not contain all the necessary information required to carry out that operation (both the
 * {@link org.adsync4j.spi.DomainControllerAffiliation#getInvocationId() invocationId} and the
 * {@link org.adsync4j.spi.DomainControllerAffiliation#getHighestCommittedUSN() highest committed USN} is required).
 * The absence of these properties indicates that the initial full synchronization has not been done yet,
 * so clients will have to invoke {@link ActiveDirectorySyncService#fullSync} first, before attempting an incremental
 * synchronization.
 */
public class InitialFullSyncRequiredException extends FullSyncRequiredException {
}
