package com.github.tumbl3w33d.h2;

import static com.github.tumbl3w33d.h2.OAuth2ProxyStores.loginRecordDAO;

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

import com.github.tumbl3w33d.users.db.OAuth2ProxyLoginRecord;

@Named("mybatis")
@Singleton
public class OAuth2ProxyLoginRecordStore extends StateGuardLifecycleSupport
        implements TransactionalStore<DataSession<?>> {

    private final DataSessionSupplier sessionSupplier;

    @Inject
    public OAuth2ProxyLoginRecordStore(final DataSessionSupplier sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public DataSession<?> openSession() {
        return OAuth2ProxyStores.openSession(sessionSupplier);
    }

    @Transactional
    public Map<String, OAuth2ProxyLoginRecord> getAllLoginRecords() {
        return Collections.unmodifiableMap(StreamSupport.stream(loginRecordDAO().browse().spliterator(), false)
                .collect(Collectors.toMap(OAuth2ProxyLoginRecord::getId, Function.identity())));
    }

    @Transactional
    public Optional<OAuth2ProxyLoginRecord> getLoginRecord(String userId) {
        log.trace("call to getLoginRecord with userId {}", userId);

        try {
            return loginRecordDAO().read(userId);
        } catch (Exception e) {
            log.error("unable to retrieve login record for {} - {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void createLoginRecord(String userId) {
        log.trace("call to createLoginRecord with userId {}", userId);
        try {
            loginRecordDAO().create(OAuth2ProxyLoginRecord.of(userId));
        } catch (Exception e) {
            log.error("unable to create login record for {} - {}", userId, e);
            throw e;
        }
    }

    @Transactional
    public void updateLoginRecord(String userId) {
        loginRecordDAO().update(OAuth2ProxyLoginRecord.of(userId));
    }

}
