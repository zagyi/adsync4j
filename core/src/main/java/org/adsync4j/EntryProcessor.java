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

public interface EntryProcessor<LDAP_ATTRIBUTE> {

    void processNew(List<LDAP_ATTRIBUTE> entry);

    void processChanged(List<LDAP_ATTRIBUTE> entry);

    void processDeleted(UUID entryId);
}
