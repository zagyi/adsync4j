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
package org.adsync4j;

import java.util.List;
import java.util.UUID;

public interface DomainControllerAffiliation extends LdapConnectionDetails {

    /**
     * @return DN of the node designating the subtree which is the scope of the sync operations (e.g. {@code
     *         CN=Users,DC=example,DC=com}).
     */
    String getSyncBaseDN();

    String getSearchFilter();

    String getSearchDeletedObjectsFilter();

    List<String> getAttributesToSync();

    UUID getInvocationId();

    Long getHighestCommittedUSN();

    void setInvocationId(UUID uuid);

    void setHighestCommittedUSN(Long hcusn);
}
