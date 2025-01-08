package com.github.tumbl3w33d;

import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.h2.OAuth2ProxyLoginRecordStore;
import com.github.tumbl3w33d.h2.OAuth2ProxyTokenInfoStore;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;
import com.github.tumbl3w33d.users.db.OAuth2ProxyLoginRecord;
import com.github.tumbl3w33d.users.db.OAuth2ProxyTokenInfo;

@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class OAuth2ProxyApiTokenInvalidateTask extends TaskSupport implements Cancelable {

    private final OAuth2ProxyLoginRecordStore loginRecordStore;
    private final OAuth2ProxyTokenInfoStore tokenInfoStore;
    private final OAuth2ProxyUserManager userManager;
    private final SecuritySystem securitySystem;
    private final EmailManager mailManager;

    @Inject
    public OAuth2ProxyApiTokenInvalidateTask(@Named OAuth2ProxyLoginRecordStore loginRecordStore,
            @Named OAuth2ProxyTokenInfoStore tokenInfoStore, @Named OAuth2ProxyUserManager userManager,
            SecuritySystem securitySystem, EmailManager mailManager) {
        this.loginRecordStore = loginRecordStore;
        this.tokenInfoStore = tokenInfoStore;
        this.userManager = userManager;
        this.securitySystem = securitySystem;
        this.mailManager = mailManager;
    }

    @Override
    protected Void execute() throws Exception {
        Map<String, OAuth2ProxyLoginRecord> loginRecords = loginRecordStore.getAllLoginRecords();
        Map<String, OAuth2ProxyTokenInfo> tokenInfos = tokenInfoStore.getAllTokenInfos();

        if (loginRecords.isEmpty() && tokenInfos.isEmpty()) {
            log.debug("No records found, nothing to do");
            return null;
        }

        int configuredIdleExpiration = getConfiguration().getInteger(
                OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_IDLE_EXPIRY,
                OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_IDLE_EXPIRY_DEFAULT);

        int configuredMaxTokenAge = getConfiguration().getInteger(
                OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_AGE,
                OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_AGE_DEFAULT);

        boolean notify = getConfiguration().getBoolean(OAuth2ProxyApiTokenInvalidateTaskDescriptor.NOTIFY,
                OAuth2ProxyApiTokenInvalidateTaskDescriptor.NOTIFY_DEFAULT);

        Set<String> userIds = new HashSet<>(loginRecords.size());
        userIds.addAll(loginRecords.keySet());
        userIds.addAll(tokenInfos.keySet());
        for (String userId : userIds) {
            if ("admin".equals(userId)) {
                // never reset the admin "token" as it would overwrite the password, possibly locking people out of nexus
                // when the task would run before OIDC setup is completed
                continue;
            }

            if (isUserIdleTimeExpired(userId, loginRecords.get(userId), configuredIdleExpiration)
                    || isTokenLifespanExpired(userId, tokenInfos.get(userId), configuredMaxTokenAge)) {
                resetApiToken(userId, notify);
                log.info("API token of user {} has been reset", userId);
            }

        }
        return null;
    }

    private boolean isUserIdleTimeExpired(String userId, OAuth2ProxyLoginRecord loginRecord, int configuredIdleTime) {
        if (configuredIdleTime <= 0) {
            return false;
        }

        Timestamp lastLoginDate = loginRecord.getLastLogin();
        log.debug("Last known login for {} was {}", userId, OAuth2ProxyRealm.formatDateString(lastLoginDate));
        long timePassed = ChronoUnit.DAYS.between(lastLoginDate.toInstant(), Instant.now());
        log.debug("Time passed since login: {} - configured maximum: {}", timePassed, configuredIdleTime);
        if (timePassed >= configuredIdleTime) {
            log.debug("Idle time expired for {}", userId);
            return true;
        }
        return false;
    }

    private boolean isTokenLifespanExpired(String userId, OAuth2ProxyTokenInfo tokenInfo, int configuredMaxTokenAge) {
        if (configuredMaxTokenAge <= 0) {
            return false;
        }

        Timestamp tokenCreationDate = tokenInfo.getTokenCreation();
        log.debug("API token for {} was created at {}", userId, OAuth2ProxyRealm.formatDateString(tokenCreationDate));
        long timePassed = ChronoUnit.DAYS.between(tokenCreationDate.toInstant(), Instant.now());
        log.debug("Time passed since token creation: {} - configured maximum: {}", timePassed, configuredMaxTokenAge);
        if (timePassed >= configuredMaxTokenAge) {
            log.debug("Token lifespan expired for user {}", userId);
            return true;
        }
        return false;
    }

    @Override
    public String getMessage() {
        return "Invalidate OAuth2 Proxy API tokens of users who did not show up for a while";
    }

    private void resetApiToken(String userId, boolean notify) {
        try {
            securitySystem.changePassword(userId, OAuth2ProxyRealm.generateSecureRandomString(32));
            log.debug("API token reset for user {} succeeded", userId);
            if (notify) {
                sendMail(userId);
            }
        } catch (UserNotFoundException e) {
            log.error("Unable to reset API token of user {}", userId);
            log.debug("Unable to reset API token of user {}", userId, e);
        }
    }

    private void sendMail(String userId) throws UserNotFoundException {
        if (mailManager.getConfiguration().isEnabled()) {
            User user = userManager.getUser(userId);
            String to = user.getEmailAddress();
            try {
                SimpleEmail mail = new SimpleEmail();
                mail.addTo(to);
                if (BaseUrlHolder.isSet()) {
                    mail.setMsg("Your OAuth2 Proxy API Token on " + BaseUrlHolder.get()
                            + " has been invalidated because of inactivity or expired token lifespan");
                } else {
                    mail.setMsg(
                            "Your OAuth2 Proxy API Token has been invalidated because of inactivity or expired token lifespan");
                }
                mailManager.send(mail);
            } catch (EmailException e) {
                log.warn("Failed to send notification email about oauth2 API token reset to user " + user.getName());
                log.debug("Failed to send notification email", e);
            }
        } else {
            log.warn("Sending token invalidation notifications is enabled, but no mail server is configured in Nexus");
        }
    }

}
