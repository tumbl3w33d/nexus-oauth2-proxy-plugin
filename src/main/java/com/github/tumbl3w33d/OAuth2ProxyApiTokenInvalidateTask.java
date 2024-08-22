package com.github.tumbl3w33d;

import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.h2.OAuth2ProxyLoginRecordStore;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;
import com.github.tumbl3w33d.users.db.OAuth2ProxyLoginRecord;

@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class OAuth2ProxyApiTokenInvalidateTask extends TaskSupport implements Cancelable {

   private final OAuth2ProxyLoginRecordStore loginRecordStore;

   private final OAuth2ProxyUserManager userManager;

   @Inject
   public OAuth2ProxyApiTokenInvalidateTask(@Named OAuth2ProxyLoginRecordStore loginRecordStore,
         final List<UserManager> userManagers, final OAuth2ProxyUserManager userManager) {

      this.loginRecordStore = loginRecordStore;
      this.userManager = userManager;
   }

   private void resetApiToken(String userId) {
      try {
         userManager.changePassword(userId, OAuth2ProxyRealm.generateSecureRandomString(32));
         log.debug("API token reset for user {} succeeded", userId);
      } catch (UserNotFoundException e) {
         log.error("Unable to reset API token of user {} - {}", userId, e);
      }
   }

   @Override
   protected Void execute() throws Exception {

      Set<OAuth2ProxyLoginRecord> loginRecords = loginRecordStore.getAllLoginRecords();

      if (loginRecords.isEmpty()) {
         log.debug("No login records found, nothing to do");
         return null;
      }

      for (OAuth2ProxyLoginRecord loginRecord : loginRecords) {
         String userId = loginRecord.getId();
         Timestamp lastLoginDate = loginRecord.getLastLogin();

         Instant lastLoginInstant = lastLoginDate.toInstant();
         Instant nowInstant = Instant.now();

         log.debug("Last known login for {} was {}", userId,
               OAuth2ProxyRealm.formatDateString(lastLoginDate));

         long timePassed = ChronoUnit.DAYS.between(lastLoginInstant, nowInstant);

         int configuredDuration = getConfiguration()
               .getInteger(OAuth2ProxyApiTokenInvalidateTaskDescriptor.CONFIG_EXPIRY, 1);

         log.debug("Time passed since login: {} - configured maximum: {}", timePassed,
               configuredDuration);

         if (timePassed >= configuredDuration) {
            resetApiToken(userId);
            log.info("Reset api token of user {} because they did not show up for a while",
                  userId);
         }
      }

      return null;
   }

   @Override
   public String getMessage() {
      return "Invalidate OAuth2 Proxy API tokens of users who did not show up for a while";
   }

}
