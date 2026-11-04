package com.boutique.catalog;

import java.nio.file.Path;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("boutique.catalog")
public record CatalogProperties(@NotNull Path imagesDir) {
}
