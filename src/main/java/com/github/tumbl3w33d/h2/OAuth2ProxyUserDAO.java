package com.github.tumbl3w33d.h2;

import org.apache.ibatis.annotations.Param;
import org.sonatype.nexus.datastore.api.IdentifiedDataAccess;

import com.github.tumbl3w33d.users.db.OAuth2ProxyUser;

public interface OAuth2ProxyUserDAO extends IdentifiedDataAccess<OAuth2ProxyUser> {

        public void updateApiToken(@Param("preferredUsername") String preferredUsername,
                        @Param("apiToken") String apiToken);

        public void updateGroups(@Param("preferredUsername") String preferredUsername,
                        @Param("groupString") String groupString);
}
