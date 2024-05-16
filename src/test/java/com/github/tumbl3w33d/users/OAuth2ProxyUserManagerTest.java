package com.github.tumbl3w33d.users;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import com.github.tumbl3w33d.users.db.OAuth2ProxyUserStore;
import com.google.common.collect.ImmutableSet;

public class OAuth2ProxyUserManagerTest {
    @Test
    void testAddUser() {

    }

    @Test
    void testChangePassword() {

    }

    @Test
    void testCreateUserObject() {

    }

    @Test
    void testDeleteUser() {

    }

    @Test
    void testGetApiToken() {

    }

    @Test
    void testGetUser() {

    }

    @Test
    void testGetUserWithRoleIds() {

    }

    @Test
    void testListUserIds() {

    }

    @Test
    void testListUsers() {

    }

    @Test
    void testSearchUsers() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user", "test.user@example.com");
        Mockito.when(userStore.getAllUsers()).thenReturn(ImmutableSet.of(exampleUser));
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        Set<User> searchResult = userManager.searchUsers(new UserSearchCriteria("test.user"));
        assertTrue(searchResult.size() == 1);
        assertTrue(searchResult.stream().anyMatch(user -> user.getUserId().equals("test.user")));

        searchResult = userManager.searchUsers(new UserSearchCriteria("foo.bar"));
        assertTrue(searchResult.isEmpty());

        UserSearchCriteria criteria = new UserSearchCriteria();
        criteria.setEmail("foo.bar@example.com");
        searchResult = userManager.searchUsers(criteria);
        assertTrue(searchResult.isEmpty());

        Mockito.when(userStore.getAllUsers()).thenReturn(ImmutableSet.of());
        searchResult = userManager.searchUsers(new UserSearchCriteria("test.user"));
        assertTrue(searchResult.isEmpty());
    }

    @Test
    void testUpdateUser() {

    }
}
