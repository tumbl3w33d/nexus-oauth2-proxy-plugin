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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class OAuth2ProxyApiTokenInvalidateTask
        extends TaskSupport
        implements Cancelable {

    private final Logger logger = LoggerFactory.getLogger(OAuth2ProxyApiTokenInvalidateTask.class.getName());

    private final DatabaseInstance databaseInstance;
    private final UserManager nexusAuthenticatingRealm;

    @Inject
    public OAuth2ProxyApiTokenInvalidateTask(@Named(OAuth2ProxyDatabase.NAME) DatabaseInstance databaseInstance,
            final List<UserManager> userManagers) {

        this.databaseInstance = databaseInstance;

        this.nexusAuthenticatingRealm = userManagers.stream()
                .filter(um -> um.getAuthenticationRealmName() == "NexusAuthenticatingRealm")
                .findFirst().get();

        OAuth2ProxyRealm.ensureUserLoginTimestampSchema(databaseInstance, log);
    }

    private void resetPassword(String userId) {
        try {
            nexusAuthenticatingRealm.changePassword(userId, generateSecureRandomString(32));
            logger.debug("Password reset for user {} succeeded", userId);
        } catch (UserNotFoundException e) {
            logger.error("Unable to reset password of user {} - {}", userId, e);
        }
    }

    @Override
    protected Void execute() throws Exception {
        try (ODatabaseDocumentTx db = databaseInstance.acquire()) {
            db.begin();

            List<ODocument> userLogins = db.query(new OSQLSynchQuery<ODocument>(
                    "select from " + CLASS_USER_LOGIN));

            if (userLogins.isEmpty()) {
                logger.debug("Nothing to do");
            } else {
                for (ODocument userLogin : userLogins) {
                    String userId = userLogin.field(FIELD_USER_ID);
                    Date lastLoginDate = userLogin.field(FIELD_LAST_LOGIN);

                    Instant lastLoginInstant = lastLoginDate.toInstant();
                    Instant nowInstant = Instant.now();

                    logger.debug("Last known login for {} was {}", userId,
                            formatDateString(lastLoginDate));

                    long timePassed = ChronoUnit.DAYS.between(lastLoginInstant, nowInstant);

                    int configuredDuration = getConfiguration()
                            .getInteger(OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_EXPIRY, 1);

                    logger.debug("Time passed since login: {} - configured maximum: {}", timePassed,
                            configuredDuration);

                    if (timePassed >= configuredDuration) {
                        resetPassword(userId);
                        logger.info(
                                "Reset api token of user {} because they did not login via OAuth2 Proxy for a while",
                                userId);
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Failed to retrieve login timestamps - {}", e);
        }
        return null;
    }

    @Override
    public String getMessage() {
        return "Invalidate OAuth2 Proxy API tokens of users who did not log in for a while";
    }
}
