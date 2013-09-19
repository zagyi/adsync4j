package org.adsync4j.unboundid;

import com.unboundid.ldap.sdk.Attribute;
import org.adsync4j.spi.LdapAttributeResolver;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

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
