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
package org.adsync4j.spi;

/**
 * Interface of a repository that is able to load and save {@link DomainControllerAffiliation} instances.
 * <p/>
 * <b>Important!</b>
 * Implementations must persist DCAs in the same physical database that stores the entries synchronized based on the DCA. This
 * is necessary to ensure that the DCA stays consistent with the synchronized entries in case the database is restored from a
 * backup. This consistency ensures that an incremental synchronization will be enough after a local database restore operation.
 *
 * @param <KEY> The type of the key used to identify the stored DCAs.
 * @param <DCA_IMPL> The implementation class of the {@link DomainControllerAffiliation} interface.
 */
public interface DCARepository<KEY, DCA_IMPL extends DomainControllerAffiliation> {
    DCA_IMPL load(KEY key);
    DCA_IMPL save(DCA_IMPL dca);
}
