package com.github.tumbl3w33d.users.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class OAuth2ProxyUserTest {

    public void assertArrayEquals(String[] expected, String[] actual) {
        if (!Arrays.equals(expected, actual)) {
            String message = String.format(
                    "Array-Inhalte sind unterschiedlich.%n  Erwartet: %s%n  Tatsächlich: %s",
                    Arrays.toString(expected),
                    Arrays.toString(actual));
            throw new AssertionError(message);
        }
    }

    @Test
    void testGetNameParts() {
        Optional<String[]> actual = OAuth2ProxyUser.getNameParts("foo.bar");
        Optional<String[]> expected = Optional.of(new String[] { "Foo", "Bar" });
        assertArrayEquals(expected.get(), actual.get());

        actual = OAuth2ProxyUser.getNameParts("foo.bar@example.com");
        expected = Optional.of(new String[] { "Foo", "Bar" });
        assertArrayEquals(expected.get(), actual.get());

        actual = OAuth2ProxyUser.getNameParts("foo-bar");
        expected = Optional.of(new String[] { "Foo-bar", "Unknown" });
        assertArrayEquals(expected.get(), actual.get());

        actual = OAuth2ProxyUser.getNameParts("");
        expected = Optional.of(new String[] { "Unknown", "Unknown" });
        assertArrayEquals(expected.get(), actual.get());

        actual = OAuth2ProxyUser.getNameParts(null);
        expected = Optional.of(new String[] { "Unknown", "Unknown" });
        assertArrayEquals(expected.get(), actual.get());
    }

    @Test
    void testCapitalize() {
        assertEquals("Foo", OAuth2ProxyUser.capitalize("foo"));
        assertEquals("Foo", OAuth2ProxyUser.capitalize("FOO"));
        assertEquals("Unknown", OAuth2ProxyUser.capitalize(""));
        assertEquals("Unknown", OAuth2ProxyUser.capitalize(null));
    }
}
