package com.github.tumbl3w33d.users.db;

import java.io.Serializable;
import java.sql.Timestamp;

import org.joda.time.Instant;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.HasStringId;

public class OAuth2ProxyTokenInfo extends AbstractEntity implements Serializable, HasStringId {

    private static final long serialVersionUID = -2052302452536776751L;

    private String userId;
    private Timestamp tokenCreation;

    public Timestamp getTokenCreation() {
        return tokenCreation;
    }

    public void setTokenCreation(Timestamp tokenCreation) {
        this.tokenCreation = tokenCreation;
    }

    @Override
    public String getId() {
        return this.userId;
    }

    @Override
    public void setId(String id) {
        this.userId = id;
    }

    public static OAuth2ProxyTokenInfo of(String userId) {
        OAuth2ProxyTokenInfo newRecord = new OAuth2ProxyTokenInfo();
        newRecord.setId(userId);
        newRecord.setTokenCreation(new Timestamp(Instant.now().getMillis()));
        return newRecord;
    }

}
