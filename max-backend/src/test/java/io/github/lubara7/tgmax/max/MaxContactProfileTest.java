package io.github.lubara7.tgmax.max;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaxContactProfileTest {
    @Test public void maxProfileNamePhotoAndPhoneArePreserved() {
        MaxContactProfile p = new MaxContactProfile(Maps.of("id", 12345L,
                "names", List.of(Maps.of("name", "Fallback"), Maps.of("type", "ONEME", "firstName", "Иван", "lastName", "Иванов")),
                "baseUrl", "https://i.oneme.ru/avatar/synthetic", "photoId", 9876L, "phone", 79995550123L));
        assertEquals("Иван", p.firstName);
        assertEquals("Иванов", p.lastName);
        assertEquals("79995550123", p.phone);
        assertEquals(9876L, p.photoId);
        assertEquals("https://i.oneme.ru/avatar/synthetic", p.avatarUrl);
        assertTrue(p.hasName());
    }

    @Test public void unknownContactsDoNotBecomeGeneratedMaxIdNames() {
        MaxContactProfile p = new MaxContactProfile(Maps.of("id", 12345L, "baseUrl", "file:///private/example"));
        assertFalse(p.hasName());
        assertEquals("", p.firstName);
        assertEquals("", p.avatarUrl);
    }
}
