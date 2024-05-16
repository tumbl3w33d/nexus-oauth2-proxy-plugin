package com.github.tumbl3w33d.users.db;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.security.role.NoSuchRoleException;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Named
@Singleton
public class OAuth2ProxyRoleEntityAdapter extends IterableEntityAdapter<OAuth2ProxyRole> {

    static final String DB_ROLE_CLASS = (new OClassNameBuilder()).type("role").build();
    static final String DB_ROLE_PROP_NAME = "name";

    private static final String I_NAME = (new OIndexNameBuilder())
            .type(DB_ROLE_CLASS)
            .property(DB_ROLE_PROP_NAME)
            .build();

    public OAuth2ProxyRoleEntityAdapter() {
        super(DB_ROLE_CLASS);
        log.debug("initializing OAuth2ProxyRoleEntityAdapter");
    }

    @Override
    public void register(ODatabaseDocumentTx db) {
        super.register(db);
        log.debug("register called for OAuth2ProxyRoleEntityAdapter");
    }

    @Override
    protected void defineType(OClass type) {
        type.createProperty(DB_ROLE_PROP_NAME, OType.STRING).setNotNull(true);

        type.createIndex(I_NAME, OClass.INDEX_TYPE.UNIQUE, new String[] { DB_ROLE_PROP_NAME });
    }

    @Override
    protected OAuth2ProxyRole newEntity() {
        return new OAuth2ProxyRole();
    }

    @Override
    protected void readFields(ODocument document, OAuth2ProxyRole entity) throws Exception {
        entity.setName((String) document.field(DB_ROLE_PROP_NAME, OType.STRING));
    }

    @Override
    protected void writeFields(ODocument document, OAuth2ProxyRole entity) throws Exception {
        document.field(DB_ROLE_PROP_NAME, entity.getName());
    }

    @Override
    public Iterable<OAuth2ProxyRole> browse(ODatabaseDocumentTx db) {
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>("SELECT FROM " + DB_ROLE_CLASS);
        List<ODocument> results = db.query(query);
        return convertToIterable(results);
    }

    @Override
    public ODocument addEntity(ODatabaseDocumentTx db, OAuth2ProxyRole entity) {
        ODocument doc = new ODocument(DB_ROLE_CLASS);

        try {
            writeFields(doc, entity);
        } catch (Exception e) {
            log.error("unable to write role data - {}", e);
        }

        try {
            db.save(doc);
        } catch (Exception e) {
            log.error("unable to save role doc: {}", e);
        }

        return doc;
    }

    @Override
    public void deleteEntity(ODatabaseDocumentTx db, OAuth2ProxyRole entity) {

        String query = "SELECT FROM " + DB_ROLE_CLASS + " WHERE " + DB_ROLE_PROP_NAME + " = ?";

        List<ODocument> results = db.query(new OSQLSynchQuery<>(query), entity.getName());

        if (results != null && !results.isEmpty()) {
            ODocument doc = results.get(0);
            db.delete(doc);
        }
    }

    protected ODocument getEntityByName(ODatabaseDocumentTx db, String name) {
        String query = "SELECT FROM " + DB_ROLE_CLASS + " WHERE " + DB_ROLE_PROP_NAME + " = ?";

        List<ODocument> results = db.query(new OSQLSynchQuery<>(query), name);

        if (results != null && !results.isEmpty()) {
            if (results.size() > 1) {
                log.warn("found multiple db records for {}", name);
            }
            return results.get(0);
        }
        throw new NoSuchRoleException(name);
    }

    protected OAuth2ProxyRole convertToOAuth2ProxyRole(ODocument document) {
        OAuth2ProxyRole role = new OAuth2ProxyRole();

        try {
            readFields(document, role);
        } catch (Exception e) {
            log.error("failed to map role data from db to entity - {}", e);
        }

        return role;
    }

    public Iterable<OAuth2ProxyRole> convertToIterable(List<ODocument> documents) {
        return ImmutableList.copyOf(
                documents.stream()
                        .map(this::convertToOAuth2ProxyRole)
                        .collect(Collectors.toList()));

    }

}
