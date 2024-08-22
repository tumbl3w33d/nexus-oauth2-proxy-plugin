package com.github.tumbl3w33d.users;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import com.github.tumbl3w33d.h2.OAuth2ProxyUserStore;
import com.github.tumbl3w33d.users.db.OAuth2ProxyUser;
import com.google.common.collect.ImmutableSet;

public class OAuth2ProxyUserManagerTest {

    private OAuth2ProxyUserManager userManager;

    @Test
    void testAddUser() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");

        userManager = getTestUserManager(userStore);
        userManager.addUser(exampleUser, "secret123");

        ArgumentCaptor<OAuth2ProxyUser> userCaptor = ArgumentCaptor.forClass(OAuth2ProxyUser.class);
        verify(userStore).addUser(userCaptor.capture());

        OAuth2ProxyUser capturedUser = userCaptor.getValue();

        assertEquals("secret123", capturedUser.getApiToken());
    }

    @Test
    void testChangePassword() throws UserNotFoundException {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);

        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
        Mockito.when(userStore.getUser(any())).thenReturn(Optional.of(exampleUser));
        userManager = getTestUserManager(userStore);
        userManager.changePassword(exampleUser.getUserId(), "secret123");

     ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
     ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
     verify(userStore).updateUserApiToken(userIdCaptor.capture(),
             passwordCaptor.capture());

     String capturedUserId = userIdCaptor.getValue();
     String capturedPassword = passwordCaptor.getValue();

     assertEquals(exampleUser.getUserId(), capturedUserId);
     assertEquals("secret123", capturedPassword);
 }

    @Test
    void testCreateUserObject() {
        User user = OAuth2ProxyUserManager.createUserObject("test.user", "test.user@example.com");
        assertNotNull(user);
        assertEquals("test.user", user.getUserId());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals("test.user@example.com", user.getEmailAddress());
        assertEquals(UserStatus.active, user.getStatus());
        assertEquals(OAuth2ProxyUserManager.SOURCE, user.getSource());
    }

    @Test
    void testDeleteUser() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);

        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
        Mockito.when(userStore.getUser(any())).thenReturn(Optional.of(exampleUser));
        userManager = getTestUserManager(userStore);
        assertDoesNotThrow(() -> userManager.deleteUser(exampleUser.getUserId()));

        ArgumentCaptor<OAuth2ProxyUser> userCaptor = ArgumentCaptor.forClass(OAuth2ProxyUser.class);
        verify(userStore).deleteUser(userCaptor.capture());

        OAuth2ProxyUser capturedUser = userCaptor.getValue();

        assertEquals(OAuth2ProxyUser.of(exampleUser), capturedUser);
    }

    @Test
    void testGetApiToken() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        Mockito.when(userStore.getApiToken("test.user")).thenReturn(Optional.of("secret123"));

        userManager = getTestUserManager(userStore);
        Optional<String> token = userManager.getApiToken("test.user");

        ArgumentCaptor<String> principal = ArgumentCaptor.forClass(String.class);
        verify(userStore).getApiToken(principal.capture());
        String capturedPrincipal = principal.getValue();

        assertEquals("test.user", capturedPrincipal);
        assertEquals(Optional.of("secret123"), token);
    }

    @Test
    void testGetUser() throws UserNotFoundException {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
        Mockito.when(userStore.getUser(anyString())).thenReturn(Optional.of(exampleUser));
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        assertDoesNotThrow(() -> {
            User user = userManager.getUser(exampleUser.getUserId());
            assertNotNull(user);
            assertEquals(exampleUser.getUserId(), user.getUserId());
        });
    }

    @Test
    void testGetUserForNonExisting() throws UserNotFoundException {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
        Mockito.when(userStore.getUser(anyString())).thenReturn(Optional.empty());
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        assertThrows(UserNotFoundException.class,
                () -> userManager.getUser(exampleUser.getUserId()));
    }

    @Test
    void testGetUserWithRoleIds() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
        Mockito.when(userStore.getUser(anyString())).thenReturn(Optional.of(exampleUser));
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        assertDoesNotThrow(() -> {
            User user = userManager.getUser(exampleUser.getUserId(),
                    ImmutableSet.of("foo", "bar"));
            assertNotNull(user);
            assertEquals(exampleUser.getUserId(), user.getUserId());
        });
    }

    @Test
    void testListUserIds() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser1 = OAuth2ProxyUserManager.createUserObject("test.user1",
                "test.user1@example.com");
        User exampleUser2 = OAuth2ProxyUserManager.createUserObject("test.user2",
                "test.user2@example.com");
        ImmutableSet<User> testUsers = ImmutableSet.of(exampleUser1, exampleUser2);
        Mockito.when(userStore.getAllUsers()).thenReturn(testUsers);
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        assertEquals(ImmutableSet.of("test.user1", "test.user2"),
                userManager.listUserIds());
    }

    @Test
    void testListUsers() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser1 = OAuth2ProxyUserManager.createUserObject("test.user1",
                "test.user1@example.com");
        User exampleUser2 = OAuth2ProxyUserManager.createUserObject("test.user2",
                "test.user2@example.com");
        ImmutableSet<User> testUsers = ImmutableSet.of(exampleUser1, exampleUser2);
        Mockito.when(userStore.getAllUsers()).thenReturn(testUsers);
        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);

        assertEquals(testUsers, userManager.listUsers());
    }

    @Test
    void testSearchUsers() {
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");
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
        OAuth2ProxyUserStore userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        User exampleUser = OAuth2ProxyUserManager.createUserObject("test.user",
                "test.user@example.com");

        userManager = getTestUserManager(userStore);
        assertDoesNotThrow(() -> {
            userManager.updateUser(exampleUser);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userStore).updateUser(userCaptor.capture());

            User capturedUser = userCaptor.getValue();

            assertEquals(exampleUser, capturedUser);
        });
    }

    @Test
    void testSupportsWrite() {
        assertTrue(getTestUserManager(null).supportsWrite());
    }

    @Test
    void testgetAuthenticationRealmName() {
        assertEquals(OAuth2ProxyUserManager.AUTHENTICATING_REALM,
                getTestUserManager(null).getAuthenticationRealmName());
    }

    @Test
    void testGetSource() {
        assertEquals(OAuth2ProxyUserManager.SOURCE, getTestUserManager(null).getSource());
    }

    private OAuth2ProxyUserManager getTestUserManager(OAuth2ProxyUserStore userStore) {

        if (userStore == null) {
            userStore = Mockito.mock(OAuth2ProxyUserStore.class);
        }

        OAuth2ProxyUserManager userManager = new OAuth2ProxyUserManager(userStore);
        return userManager;
    }
}
