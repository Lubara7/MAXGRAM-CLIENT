package io.github.lubara7.tgmax.max;

import java.net.URI;
import java.util.Map;

public final class MaxContactProfile {
    public final String firstName, lastName, avatarUrl, phone;
    public final long photoId;

    public MaxContactProfile(Map<String, Object> contact) {
        Map<String, Object> chosen = contact;
        for (Map<String, Object> name : Maps.objects(contact.get("names"))) {
            if (!text(name, "firstName", "name").isEmpty()) {
                chosen = name;
                if ("ONEME".equals(name.get("type"))) break;
            }
        }
        firstName = text(chosen, "firstName", "name");
        lastName = Maps.string(chosen.get("lastName")).trim();
        avatarUrl = imageUrl(text(contact, "baseUrl", "baseRawUrl"));
        photoId = Maps.number(contact.get("photoId"), 0);
        phone = Maps.string(contact.get("phone")).replaceAll("[^0-9]", "");
    }

    public boolean hasName() { return !firstName.isEmpty() || !lastName.isEmpty(); }

    private static String text(Map<String, Object> values, String first, String second) {
        String result = Maps.string(values.get(first)).trim();
        return result.isEmpty() ? Maps.string(values.get(second)).trim() : result;
    }

    private static String imageUrl(String value) {
        if (value.length() > 8192) return "";
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null ? value : "";
        } catch (IllegalArgumentException invalid) { return ""; }
    }
}
