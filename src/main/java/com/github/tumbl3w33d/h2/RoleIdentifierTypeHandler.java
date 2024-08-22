package com.github.tumbl3w33d.h2;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.sonatype.nexus.security.role.RoleIdentifier;

import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;

public class RoleIdentifierTypeHandler extends BaseTypeHandler<Set<RoleIdentifier>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<RoleIdentifier> parameter, JdbcType jdbcType)
            throws SQLException {
        String groupString = parameter.stream()
                .map(RoleIdentifier::getRoleId)
                .sorted()
                .collect(Collectors.joining(","));
        ps.setString(i, groupString);
    }

    private Set<RoleIdentifier> groupStringToSet(String groupString) {
        return Arrays.stream(groupString.split(","))
                .map(roleId -> new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, roleId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RoleIdentifier> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String groupString = rs.getString(columnName);
        return groupStringToSet(groupString);
    }

    @Override
    public Set<RoleIdentifier> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String groupString = rs.getString(columnIndex);
        return groupStringToSet(groupString);
    }

    @Override
    public Set<RoleIdentifier> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String groupString = cs.getString(columnIndex);
        return groupStringToSet(groupString);
    }
}