package de.flur4.roomiefunds.models.webclient.enablebanking;

public record Credential(
        String name,
        String title,
        boolean required,
        String description
) {
}
