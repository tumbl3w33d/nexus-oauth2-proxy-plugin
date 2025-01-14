package com.github.tumbl3w33d.h2;

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.UnitOfWork;

public class OAuth2ProxyStores {

    public static DataSession<?> openSession(DataSessionSupplier supplier) {
        return supplier.openSession(DEFAULT_DATASTORE_NAME);
    }

    private static DataSession<?> thisSession() {
        return UnitOfWork.currentSession();
    }

    private static <T extends DataAccess> T dao(final Class<T> daoClass) {
        return thisSession().access(daoClass);
    }

    public static OAuth2ProxyUserDAO userDAO() {
        return dao(OAuth2ProxyUserDAO.class);
    }

    public static OAuth2ProxyRoleDAO roleDAO() {
        return dao(OAuth2ProxyRoleDAO.class);
    }

    public static OAuth2ProxyLoginRecordDAO loginRecordDAO() {
        return dao(OAuth2ProxyLoginRecordDAO.class);
    }

    public static OAuth2ProxyTokenInfoDAO tokenInfoDAO() {
        return dao(OAuth2ProxyTokenInfoDAO.class);
    }
}
