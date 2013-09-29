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
package org.adsync4j.unboundid;

import com.unboundid.ldap.sdk.Attribute;
import org.adsync4j.spi.LdapAttributeResolver;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * {@link LdapAttributeResolver} implementation interpreting the {@link Attribute} type defined by the UnboundID LDAP SDK.
 * Implemented as an enum, in order to ensure it's a singleton.
 */
public enum UnboundIdAttributeResolver implements LdapAttributeResolver<Attribute> {

    INSTANCE;

    @Override
    public String getAsString(Attribute attribute) {
        return attribute.getValue();
    }

    @Override
    public Long getAsLong(Attribute attribute) {
        return attribute.getValueAsLong();
    }

    @Override
    public byte[] getAsByteArray(Attribute attribute) {
        return attribute.getValueByteArray();
    }

    @Nonnull
    @Override
    public List<String> getAsStringList(Attribute attribute) {
        return Arrays.asList(attribute.getValues());
    }
}
