package com.github.tumbl3w33d.h2;

import static com.github.tumbl3w33d.h2.OAuth2ProxyStores.tokenInfoDAO;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.github.tumbl3w33d.users.db.OAuth2ProxyTokenInfo;

@Named("mybatis")
@Singleton
public class OAuth2ProxyTokenInfoStore extends StateGuardLifecycleSupport
        implements TransactionalStore<DataSession<?>> {

    private final DataSessionSupplier sessionSupplier;

    @Inject
    public OAuth2ProxyTokenInfoStore(final DataSessionSupplier sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public DataSession<?> openSession() {
        return OAuth2ProxyStores.openSession(sessionSupplier);
    }

    @Transactional
    public Map<String, OAuth2ProxyTokenInfo> getAllTokenInfos() {
        return Collections.unmodifiableMap(StreamSupport.stream(tokenInfoDAO().browse().spliterator(), false)
                .collect(Collectors.toMap(OAuth2ProxyTokenInfo::getId, Function.identity())));
    }

    @Transactional
    public Optional<OAuth2ProxyTokenInfo> getTokenInfo(String userId) {
        log.trace("call to getTokenInfo with userId {}", userId);

        try {
            return tokenInfoDAO().read(userId);
        } catch (Exception e) {
            log.error("unable to retrieve token record for {} - {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void createTokenInfo(String userId) {
        log.trace("call to createTokenInfo with userId {}", userId);
        try {
            tokenInfoDAO().create(OAuth2ProxyTokenInfo.of(userId));
        } catch (Exception e) {
            log.error("unable to create token record for {} - {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void updateTokenInfo(String userId) {
        tokenInfoDAO().update(OAuth2ProxyTokenInfo.of(userId));
    }

}
