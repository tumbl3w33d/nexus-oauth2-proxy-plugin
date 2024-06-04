package com.github.tumbl3w33d;

import static com.github.tumbl3w33d.OAuth2ProxyRealm.CLASS_USER_LOGIN;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.FIELD_LAST_LOGIN;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.FIELD_USER_ID;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.formatDateString;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.generateSecureRandomString;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class OAuth2ProxyApiTokenInvalidateTask
        extends TaskSupport
        implements Cancelable {

    private final DatabaseInstance databaseInstance;

    private final OAuth2ProxyUserManager userManager;

    @Inject
    public OAuth2ProxyApiTokenInvalidateTask(@Named(OAuth2ProxyDatabase.NAME) DatabaseInstance databaseInstance,
            final List<UserManager> userManagers, final OAuth2ProxyUserManager userManager) {

        this.databaseInstance = databaseInstance;
        this.userManager = userManager;

        OAuth2ProxyRealm.ensureUserLoginTimestampSchema(databaseInstance, log);
    }

    private void resetApiToken(String userId) {
        try {
            userManager.changePassword(userId, generateSecureRandomString(32));
            log.debug("API token reset for user {} succeeded", userId);
        } catch (UserNotFoundException e) {
            log.error("Unable to reset API token of user {} - {}", userId, e);
        }
    }

    @Override
    protected Void execute() throws Exception {
        try (ODatabaseDocumentTx db = databaseInstance.acquire()) {
            db.begin();

            List<ODocument> userLogins = db.query(new OSQLSynchQuery<ODocument>(
                    "select from " + CLASS_USER_LOGIN));

            if (userLogins.isEmpty()) {
                log.debug("Nothing to do");
            } else {
                for (ODocument userLogin : userLogins) {
                    String userId = userLogin.field(FIELD_USER_ID);
                    Date lastLoginDate = userLogin.field(FIELD_LAST_LOGIN);

                    Instant lastLoginInstant = lastLoginDate.toInstant();
                    Instant nowInstant = Instant.now();

                    log.debug("Last known login for {} was {}", userId,
                            formatDateString(lastLoginDate));

                    long timePassed = ChronoUnit.DAYS.between(lastLoginInstant, nowInstant);

                    int configuredDuration = getConfiguration()
                            .getInteger(OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_EXPIRY, 1);

                    log.debug("Time passed since login: {} - configured maximum: {}", timePassed,
                            configuredDuration);

                    if (timePassed >= configuredDuration) {
                        resetApiToken(userId);
                        log.info("Reset api token of user {} because they did not show up for a while", userId);
                    }
                }

            }
        } catch (Exception e) {
            log.error("Failed to retrieve login timestamps - {}", e);
        }
        return null;
    }

    @Override
    public String getMessage() {
        return "Invalidate OAuth2 Proxy API tokens of users who did not show up for a while";
    }
}
