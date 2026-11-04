package com.boutique.catalog;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Test unitaire pur (sans Spring) : le traitement d'image est une simple classe Java
class ImageProcessorTest {

	private final ImageProcessor processor = new ImageProcessor();

	@Test
	void transparentPngBecomesJpegOnWhiteBackground() throws IOException {
		BufferedImage transparent = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);

		byte[] result = processor.toJpeg(png(transparent));

		BufferedImage jpeg = ImageIO.read(new ByteArrayInputStream(result));
		assertThat(result[0] & 0xFF).isEqualTo(0xFF);
		assertThat(result[1] & 0xFF).isEqualTo(0xD8);
		assertThat(jpeg.getWidth()).isEqualTo(40);
		assertThat(jpeg.getHeight()).isEqualTo(30);
		// Pixel transparent → blanc (le JPEG ne connaît pas la transparence)
		assertThat(new Color(jpeg.getRGB(10, 10)).getRed()).isGreaterThan(240);
	}

	@Test
	void largePhotoIsReducedTo1600PixelsKeepingItsProportions() throws IOException {
		byte[] result = processor.toJpeg(png(new BufferedImage(4000, 2000, BufferedImage.TYPE_INT_RGB)));

		BufferedImage jpeg = ImageIO.read(new ByteArrayInputStream(result));
		assertThat(jpeg.getWidth()).isEqualTo(1600);
		assertThat(jpeg.getHeight()).isEqualTo(800);
	}

	@Test
	void exifMetadataSuchAsGpsPositionIsRemoved() throws IOException {
		byte[] withExif = insertExifSegment(jpeg(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB)),
				"Exif\0\0GPS 14.6928N 17.4467W");

		byte[] result = processor.toJpeg(withExif);

		assertThat(new String(withExif, StandardCharsets.ISO_8859_1)).contains("GPS");
		assertThat(new String(result, StandardCharsets.ISO_8859_1)).doesNotContain("Exif").doesNotContain("GPS");
	}

	@Test
	void fileThatIsNotAnImageIsRefusedWhateverItsName() {
		byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
			.getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> processor.toJpeg(svg)).isInstanceOf(InvalidImageException.class)
			.hasMessageContaining("JPEG ou PNG");
	}

	@Test
	void corruptedImageIsRefused() {
		byte[] truncated = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H' };

		assertThatThrownBy(() -> processor.toJpeg(truncated)).isInstanceOf(InvalidImageException.class)
			.hasMessageContaining("illisible");
	}

	// « Bombe de décompression » : un petit fichier qui annonce 20 000 × 20 000 pixels (1,6 Go une fois décodé).
	// Les dimensions sont lues dans l'en-tête et refusées AVANT tout décodage.
	@Test
	void decompressionBombIsRefusedBeforeDecoding() {
		assertThatThrownBy(() -> processor.toJpeg(pngHeaderAnnouncing(20_000, 20_000)))
			.isInstanceOf(InvalidImageException.class)
			.hasMessageContaining("trop grande");
	}

	private static byte[] png(BufferedImage image) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		return bytes.toByteArray();
	}

	private static byte[] jpeg(BufferedImage image) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "jpeg", bytes);
		return bytes.toByteArray();
	}

	// Segment APP1 (FF E1 + longueur) juste après le marqueur de début (FF D8), comme le fait un téléphone
	private static byte[] insertExifSegment(byte[] jpeg, String content) {
		byte[] payload = content.getBytes(StandardCharsets.ISO_8859_1);
		ByteBuffer buffer = ByteBuffer.allocate(jpeg.length + 4 + payload.length);
		buffer.put(jpeg, 0, 2).put((byte) 0xFF).put((byte) 0xE1).putShort((short) (payload.length + 2)).put(payload);
		buffer.put(jpeg, 2, jpeg.length - 2);
		return buffer.array();
	}

	// Signature PNG + bloc IHDR (largeur, hauteur, 8 bits, RGB) avec son CRC, sans aucun pixel
	private static byte[] pngHeaderAnnouncing(int width, int height) {
		ByteBuffer ihdr = ByteBuffer.allocate(17).put("IHDR".getBytes(StandardCharsets.US_ASCII));
		ihdr.putInt(width).putInt(height).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
		CRC32 crc = new CRC32();
		crc.update(ihdr.array());
		return ByteBuffer.allocate(8 + 4 + 17 + 4)
			.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' })
			.putInt(13)
			.put(ihdr.array())
			.putInt((int) crc.getValue())
			.array();
	}

}
