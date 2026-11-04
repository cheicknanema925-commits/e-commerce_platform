package com.boutique.catalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// Photos dans un dossier du serveur (boutique.catalog.images-dir) ; en production, un volume Docker sauvegardé
@Component
class LocalImageStorage implements ImageStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

	private final Path directory;

	LocalImageStorage(CatalogProperties properties) throws IOException {
		this.directory = properties.imagesDir().toAbsolutePath().normalize();
		Files.createDirectories(directory);
	}

	@Override
	public void save(String filename, byte[] content) {
		try {
			// CREATE_NEW : jamais d'écrasement d'un fichier existant
			Files.write(resolve(filename), content, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Enregistrement de la photo impossible", ex);
		}
	}

	@Override
	public Optional<Resource> load(String filename) {
		Path path = resolve(filename);
		return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
	}

	@Override
	public void delete(String filename) {
		try {
			Files.deleteIfExists(resolve(filename));
		}
		catch (IOException ex) {
			// Un fichier oublié ne gêne personne ; on le signale sans faire échouer la requête
			log.warn("Photo {} non supprimée du disque", filename, ex);
		}
	}

	// Défense en profondeur : même avec un nom piégé (« ../../etc/passwd »), on ne sort jamais du dossier
	private Path resolve(String filename) {
		Path path = directory.resolve(filename).normalize();
		if (!directory.equals(path.getParent())) {
			throw new IllegalArgumentException("Nom de fichier invalide");
		}
		return path;
	}

}
