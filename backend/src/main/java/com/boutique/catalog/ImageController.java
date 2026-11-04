package com.boutique.catalog;

import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Lecture publique des photos (catalogue). Seuls des noms générés par le serveur sont acceptés : tout autre
// nom (« ../application.yml », « photo.svg »…) est refusé avant même de toucher au disque.
@RestController
public class ImageController {

	static final String PATH = "/api/images/";

	private static final Pattern FILENAME = Pattern
		.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");

	private final ImageStorage imageStorage;

	public ImageController(ImageStorage imageStorage) {
		this.imageStorage = imageStorage;
	}

	@GetMapping(PATH + "{filename}")
	public ResponseEntity<Resource> image(@PathVariable String filename) {
		if (!FILENAME.matcher(filename).matches()) {
			throw new ProductImageNotFoundException(filename);
		}
		Resource image = imageStorage.load(filename).orElseThrow(() -> new ProductImageNotFoundException(filename));
		return ResponseEntity.ok()
			.contentType(MediaType.IMAGE_JPEG)
			// Une photo ne change jamais (nouvelle photo = nouveau nom) : le navigateur la garde un an
			.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
			.body(image);
	}

}
