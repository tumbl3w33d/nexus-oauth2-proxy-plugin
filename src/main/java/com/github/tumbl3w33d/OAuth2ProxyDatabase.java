package com.github.tumbl3w33d;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseManager;

public class OAuth2ProxyDatabase {

    private OAuth2ProxyDatabase() {
    }

    public static final String NAME = "oauth2-proxy";

    @FeatureFlag(name = ORIENT_ENABLED)
    @Named(NAME)
    @Singleton
    public static class ProviderImpl
            implements Provider<DatabaseInstance> {
        private final DatabaseManager databaseManager;

        @Inject
        public ProviderImpl(final DatabaseManager databaseManager) {
            this.databaseManager = checkNotNull(databaseManager);
        }

        @Override
        public DatabaseInstance get() {
            return databaseManager.instance(NAME);
        }
    }
}