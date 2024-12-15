package com.github.tumbl3w33d.users.db;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.HasStringId;

public class OAuth2ProxyLoginRecord extends AbstractEntity implements Serializable, HasStringId {

    private static final long serialVersionUID = 2397868513451L;

    private String userId;
    private Timestamp lastLogin;

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String getId() {
        return this.userId;
    }

    @Override
    public void setId(String id) {
        this.userId = id;
    }

    public static OAuth2ProxyLoginRecord of(String userId) {
        OAuth2ProxyLoginRecord newRecord = new OAuth2ProxyLoginRecord();
        newRecord.setId(userId);
        newRecord.setLastLogin(new Timestamp(new Date().getTime()));
        return newRecord;
    }

}
