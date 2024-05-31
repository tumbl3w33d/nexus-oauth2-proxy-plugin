package com.github.tumbl3w33d.users.db;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.users.IncompleteOAuth2ProxyUserDataException;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Named
@Singleton
public class OAuth2ProxyUserEntityAdapter extends IterableEntityAdapter<OAuth2ProxyUser> {

    static final String DB_USER_CLASS = (new OClassNameBuilder()).type("user").build();
    static final String DB_USER_PROP_NAME = "name";
    private static final String DB_USER_PROP_TOKEN = "apiToken";
    private static final String DB_USER_PROP_EMAIL = "email";
    private static final String DB_USER_PROP_GROUPS = "groups";

    private static final String I_NAME = (new OIndexNameBuilder())
            .type(DB_USER_CLASS)
            .property(DB_USER_PROP_NAME)
            .build();

    private static final String I_EMAIL = (new OIndexNameBuilder())
            .type(DB_USER_CLASS)
            .property(DB_USER_PROP_EMAIL)
            .build();

    public OAuth2ProxyUserEntityAdapter() {
        super(DB_USER_CLASS);
        log.debug("initializing OAuth2ProxyEntityAdapter");
    }

    @Override
    public void register(ODatabaseDocumentTx db) {
        super.register(db);
        log.debug("register called for OAuth2ProxyEntityAdapter");
    }

    @Override
    protected void defineType(OClass type) {
        type.createProperty(DB_USER_PROP_NAME, OType.STRING).setNotNull(true);
        type.createProperty(DB_USER_PROP_EMAIL, OType.STRING).setNotNull(true);
        type.createProperty(DB_USER_PROP_TOKEN, OType.STRING);

        type.createIndex(I_NAME, OClass.INDEX_TYPE.UNIQUE, new String[] { DB_USER_PROP_NAME });
        type.createIndex(I_EMAIL, OClass.INDEX_TYPE.UNIQUE, new String[] { DB_USER_PROP_EMAIL });
    }

    @Override
    protected OAuth2ProxyUser newEntity() {
        return new OAuth2ProxyUser();
    }

    @Override
    protected void readFields(ODocument document, OAuth2ProxyUser entity) throws Exception {
        Object nameProp = document.field(DB_USER_PROP_NAME, OType.STRING);
        Object mailProp = document.field(DB_USER_PROP_EMAIL, OType.STRING);
        Object tokenProp = document.field(DB_USER_PROP_TOKEN, OType.STRING);
        Object groupsProp = document.field(DB_USER_PROP_GROUPS, OType.STRING);

        if (nameProp == null || mailProp == null) {
            throw new IncompleteOAuth2ProxyUserDataException(
                    "db doc of user " + entity.getPreferredUsername() + " has no name or mail");
        }

        entity.setPreferredUsername((String) nameProp);
        entity.setEmail((String) mailProp);

        if (tokenProp != null) {
            entity.setApiToken((String) tokenProp);
            log.debug("api token is set for user {} db doc when reading fields", (String) nameProp);
        } else {
            log.debug("user {} has no api token yet", entity.getPreferredUsername());
        }

        if (groupsProp != null) {
            entity.setGroups((String) groupsProp);
        } else {
            log.debug("user {} has no groups yet", entity.getPreferredUsername());
        }
    }

    @Override
    protected void writeFields(ODocument document, OAuth2ProxyUser entity) throws Exception {

        String name = entity.getPreferredUsername();
        if (name == null || name.isEmpty()) {
            throw new IncompleteOAuth2ProxyUserDataException("rejecting attempt to write user without name");
        }
        document.field(DB_USER_PROP_NAME, name);

        String email = entity.getEmail();
        if (email == null || email.isEmpty()) {
            throw new IncompleteOAuth2ProxyUserDataException("rejecting attempt to write user without mail");
        }
        document.field(DB_USER_PROP_EMAIL, email);

        String apiToken = entity.getApiToken();
        if (apiToken == null || apiToken.isEmpty()) {
            throw new IncompleteOAuth2ProxyUserDataException("rejecting attempt to write user without apiToken");
        }
        document.field(DB_USER_PROP_TOKEN, apiToken);

        String groupString = entity.getGroups().stream().map(RoleIdentifier::getRoleId)
                .collect(Collectors.joining(","));

        log.debug("storing comma-delimited groups of user {}: {}", name, groupString);

        document.field(DB_USER_PROP_GROUPS, groupString);
    }

    @Override
    public Iterable<OAuth2ProxyUser> browse(ODatabaseDocumentTx db) {
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>("SELECT FROM " + DB_USER_CLASS);
        List<ODocument> results = db.query(query);
        return convertToIterable(results);
    }

    @Override
    public ODocument addEntity(ODatabaseDocumentTx db, OAuth2ProxyUser entity) {
        ODocument doc = new ODocument(DB_USER_CLASS);

        try {
            writeFields(doc, entity);
            log.debug("wrote fields of new user doc for {}", entity.getPreferredUsername());
        } catch (Exception e) {
            log.error("unable to write user data - {}", e);
            return null;
        }

        try {
            db.save(doc);
            log.debug("saved new user doc of {}", entity.getPreferredUsername());
        } catch (Exception e) {
            log.error("unable to save user doc: {}", e);
            return null;
        }

        return doc;
    }

    @Override
    public void deleteEntity(ODatabaseDocumentTx db, OAuth2ProxyUser entity) {

        String query = "SELECT FROM " + DB_USER_CLASS + " WHERE " + DB_USER_PROP_NAME + " = ?";

        List<ODocument> results = db.query(new OSQLSynchQuery<>(query), entity.getPreferredUsername());

        if (results != null && !results.isEmpty()) {
            ODocument doc = results.get(0);
            db.delete(doc);
        }
    }

    @Override
    public ODocument editEntity(ODatabaseDocumentTx db, OAuth2ProxyUser entity) {
        log.trace("call to editEntity of user {}", entity.getPreferredUsername());

        try {
            ODocument existingUser = getEntityByName(db, entity.getPreferredUsername());

            if (!existingUser.field(DB_USER_PROP_EMAIL).equals(entity.getEmail())) {
                existingUser.field(DB_USER_PROP_EMAIL, entity.getEmail());
            }

            if (existingUser.field(DB_USER_PROP_GROUPS) == null
                    || !existingUser.field(DB_USER_PROP_GROUPS).equals(entity.getGroupString())) {
                existingUser.field(DB_USER_PROP_GROUPS, entity.getGroupString());
            }
            if (isTokenMissingButInbound(entity, existingUser) || isTokenExistingButDifferent(entity, existingUser)) {
                log.debug("setting api token of {} to {}", entity.getPreferredUsername(),
                        entity.getApiToken() != null ? "<hidden>" : "null");
                existingUser.field(DB_USER_PROP_TOKEN, entity.getApiToken());
            }

            db.save(existingUser);

            return existingUser;

        } catch (UserNotFoundException e) {
            log.warn("could not find user {} in db for updating", entity.getPreferredUsername());
        } catch (Exception e) {
            log.error("unexpected failure when editing user entity - {}", e);
        }
        return null;
    }

    private boolean isTokenMissingButInbound(OAuth2ProxyUser entity, ODocument existingUser) {
        return existingUser.field(DB_USER_PROP_TOKEN) == null && entity.getApiToken() != null;
    }

    private boolean isTokenExistingButDifferent(OAuth2ProxyUser entity, ODocument existingUser) {
        return existingUser.field(DB_USER_PROP_TOKEN) != null
                && !existingUser.field(DB_USER_PROP_TOKEN).equals(entity.getApiToken());
    }

    private ODocument getEntityByName(ODatabaseDocumentTx db, String name) throws UserNotFoundException {
        String query = "SELECT FROM " + DB_USER_CLASS + " WHERE " + DB_USER_PROP_NAME + " = ?";

        List<ODocument> results = db.query(new OSQLSynchQuery<>(query), name);

        if (results != null && !results.isEmpty()) {
            if (results.size() > 1) {
                log.warn("found multiple db records for {}", name);
            }
            return results.get(0);
        }

        throw new UserNotFoundException(name);
    }

    protected OAuth2ProxyUser convertToOAuth2ProxyUser(ODocument document) {
        OAuth2ProxyUser user = new OAuth2ProxyUser();

        try {
            readFields(document, user);
        } catch (Exception e) {
            log.error("failed to map user data from db to entity - {}", e);
        }

        return user;
    }

    public Iterable<OAuth2ProxyUser> convertToIterable(List<ODocument> documents) {
        return ImmutableList.copyOf(
                documents.stream()
                        .map(this::convertToOAuth2ProxyUser)
                        .collect(Collectors.toList()));

    }

}
