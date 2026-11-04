package com.boutique.catalog;

import java.util.Optional;

import org.springframework.core.io.Resource;

// Où sont rangés les fichiers des photos. Aujourd'hui un dossier local ; demain un stockage objet (S3, MinIO…)
// sans toucher au reste du code (même principe que PaymentProvider)
public interface ImageStorage {

	void save(String filename, byte[] content);

	Optional<Resource> load(String filename);

	void delete(String filename);

}
