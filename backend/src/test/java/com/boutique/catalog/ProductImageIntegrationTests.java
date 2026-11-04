package com.boutique.catalog;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;
import com.boutique.order.OrderLineRequest;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Photos des produits de bout en bout : envoi (admin), lecture publique, photo principale, suppression du fichier
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductImageIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CatalogProperties catalogProperties;

	private Long productId;

	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		productId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5)).getId();
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void adminUploadsAPhotoThatAnyoneCanThenDownload() throws Exception {
		String url = upload(png(), "photo.png");

		assertThat(url).matches("/api/images/[0-9a-f-]{36}\\.jpg");
		// Lecture publique (sans jeton), mise en cache longue durée par le navigateur
		mockMvc.perform(get(url))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_JPEG))
			.andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
		// Le catalogue renvoie la photo avec le produit
		mockMvc.perform(get("/api/products/{id}", productId))
			.andExpect(jsonPath("$.images[0].url").value(url));
	}

	@Test
	void fileThatIsNotAnImageIsRefusedEvenWithAnImageName() throws Exception {
		MockMultipartFile fake = new MockMultipartFile("file", "photo.png", "image/png",
				"<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/products/{id}/images", productId).file(fake).with(admin()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Formats acceptés : JPEG ou PNG."));
	}

	@Test
	void onlyAnAdminCanUploadAPhoto() throws Exception {
		MockMultipartFile photo = new MockMultipartFile("file", "photo.png", "image/png", png());

		mockMvc.perform(multipart("/api/products/{id}/images", productId).file(photo))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(multipart("/api/products/{id}/images", productId).file(photo)
			.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void secondPhotoCanBecomeTheMainOne() throws Exception {
		upload(png(), "face.png");
		String backUrl = upload(png(), "dos.png");
		Long backId = imageIds().get(1);

		mockMvc.perform(put("/api/products/{id}/images/{imageId}/main", productId, backId).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.images[0].url").value(backUrl));
	}

	@Test
	void ninthPhotoIsRefused() throws Exception {
		for (int i = 0; i < Product.MAX_IMAGES; i++) {
			upload(png(), "photo" + i + ".png");
		}

		mockMvc.perform(multipart("/api/products/{id}/images", productId)
			.file(new MockMultipartFile("file", "trop.png", "image/png", png()))
			.with(admin())).andExpect(status().isConflict());
		assertThat(imageIds()).hasSize(Product.MAX_IMAGES);
	}

	@Test
	void deletingAPhotoAlsoDeletesItsFile() throws Exception {
		String url = upload(png(), "photo.png");
		Long imageId = imageIds().getFirst();

		mockMvc.perform(delete("/api/products/{id}/images/{imageId}", productId, imageId).with(admin()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(url)).andExpect(status().isNotFound());
		assertThat(Files.exists(file(url))).isFalse();
	}

	// La suppression est refusée (produit déjà commandé) : la transaction est annulée, la photo doit rester
	@Test
	void refusedProductDeletionKeepsItsPhotos() throws Exception {
		String url = upload(png(), "photo.png");
		Long userId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		orderService.createOrder(userId, List.of(new OrderLineRequest(productId, 1)));

		mockMvc.perform(delete("/api/products/{id}", productId).with(admin())).andExpect(status().isConflict());

		assertThat(Files.exists(file(url))).isTrue();
	}

	@Test
	void unexpectedFileNamesAreNeverLookedUpOnDisk() throws Exception {
		mockMvc.perform(get("/api/images/application.yml")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/images/photo.svg")).andExpect(status().isNotFound());
	}

	// Envoie une photo et renvoie l'URL de la dernière photo du produit (celle qu'on vient d'ajouter)
	private String upload(byte[] content, String name) throws Exception {
		String body = mockMvc
			.perform(multipart("/api/products/{id}/images", productId)
				.file(new MockMultipartFile("file", name, "image/png", content))
				.with(admin()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<String> urls = JsonPath.read(body, "$.images[*].url");
		return urls.getLast();
	}

	// Lus via l'API : hors transaction, la collection « images » (LAZY) n'est plus accessible depuis l'entité
	private List<Long> imageIds() throws Exception {
		String body = mockMvc.perform(get("/api/products/{id}", productId)).andReturn().getResponse().getContentAsString();
		List<Number> ids = JsonPath.read(body, "$.images[*].id");
		return ids.stream().map(Number::longValue).toList();
	}

	private Path file(String url) {
		return catalogProperties.imagesDir().resolve(url.substring(url.lastIndexOf('/') + 1));
	}

	private static byte[] png() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB), "png", bytes);
		return bytes.toByteArray();
	}

	private static RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
