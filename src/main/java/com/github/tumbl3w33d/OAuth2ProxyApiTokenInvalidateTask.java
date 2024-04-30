package com.github.tumbl3w33d;

import static com.github.tumbl3w33d.OAuth2ProxyRealm.CLASS_USER_LOGIN;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.FIELD_LAST_LOGIN;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.FIELD_USER_ID;
import static com.github.tumbl3w33d.OAuth2ProxyRealm.formatDateString;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

import java.time.LocalDate;
import java.time.ZoneId;
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

    @Inject
    public OAuth2ProxyApiTokenInvalidateTask(@Named(OAuth2ProxyDatabase.NAME) DatabaseInstance databaseInstance) {
        this.databaseInstance = databaseInstance;

        OAuth2ProxyRealm.ensureUserLoginTimestampSchema(databaseInstance, log);
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
                    Date lastLoginDate = userLogin.field(FIELD_LAST_LOGIN);
                    LocalDate lastLoginLocalDate = lastLoginDate.toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    LocalDate nowLocalDate = LocalDate.now();
                    logger.info("Last known login for {} was {}", userLogin.field(FIELD_USER_ID),
                            formatDateString(lastLoginDate));
                    // if > 30d passed, reset user password
                    /*
                     * if (!lastLoginLocalDate.equals(nowLocalDate)) {
                     * // TODO
                     * }
                     */
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
