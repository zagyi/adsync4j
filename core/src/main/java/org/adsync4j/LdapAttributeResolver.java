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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface LdapAttributeResolver<LDAP_ATTRIBUTE> {

    @Nullable
    String getAsString(LDAP_ATTRIBUTE attribute);

    @Nullable
    Long getAsLong(LDAP_ATTRIBUTE attribute);

    @Nullable
    byte[] getAsByteArray(LDAP_ATTRIBUTE attribute);

    @Nonnull
    List<String> getAsStringList(LDAP_ATTRIBUTE attribute);
}
