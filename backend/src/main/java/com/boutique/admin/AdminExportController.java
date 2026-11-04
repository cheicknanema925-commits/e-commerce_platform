package com.boutique.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.function.Supplier;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Téléchargements CSV (ouvrables dans Excel, LibreOffice, Google Sheets)
@RestController
@RequestMapping("/api/admin/exports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

	private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

	private final AdminExportService exportService;

	public AdminExportController(AdminExportService exportService) {
		this.exportService = exportService;
	}

	@GetMapping("/products.csv")
	public ResponseEntity<byte[]> products() {
		return download("produits", exportService::products);
	}

	@GetMapping("/orders.csv")
	public ResponseEntity<byte[]> orders() {
		return download("commandes", exportService::orders);
	}

	@GetMapping("/customers.csv")
	public ResponseEntity<byte[]> customers() {
		return download("clients", exportService::customers);
	}

	@GetMapping("/newsletter.csv")
	public ResponseEntity<byte[]> newsletter() {
		return download("infolettre", exportService::newsletter);
	}

	// « attachment » : le navigateur enregistre le fichier au lieu de l'afficher ; nom daté (commandes-2026-11-04.csv)
	private static ResponseEntity<byte[]> download(String name, Supplier<byte[]> content) {
		String filename = name + "-" + LocalDate.now(AdminStatsService.SHOP_ZONE) + ".csv";
		return ResponseEntity.ok()
			.contentType(CSV)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
			.body(content.get());
	}

}
