package com.boutique.catalog;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;

// Transforme un fichier reçu en photo sûre et légère, sans jamais faire confiance à ce qu'il annonce :
// 1. format reconnu à ses premiers octets (pas au nom ni au Content-Type, faciles à falsifier) ;
// 2. dimensions lues AVANT le décodage (une « bombe » de 50 000 × 50 000 px saturerait la mémoire) ;
// 3. image redessinée puis réenregistrée en JPEG : les métadonnées (EXIF, position GPS du téléphone…) disparaissent,
//    et un fichier piégé (SVG avec du JavaScript, HTML déguisé) ne peut pas survivre à cette copie pixel par pixel.
@Component
class ImageProcessor {

	static final int MAX_SIDE = 1600;

	static final int MAX_SOURCE_SIDE = 8000;

	private static final float JPEG_QUALITY = 0.85f;

	private static final String UNSUPPORTED_FORMAT = "Formats acceptés : JPEG ou PNG.";

	byte[] toJpeg(byte[] data) {
		if (!isJpeg(data) && !isPng(data)) {
			throw new InvalidImageException(UNSUPPORTED_FORMAT);
		}
		return encode(resize(decode(data)));
	}

	private BufferedImage decode(byte[] data) {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new InvalidImageException(UNSUPPORTED_FORMAT);
			}
			ImageReader reader = readers.next();
			try {
				// ignoreMetadata = true : on ne lit même pas l'EXIF
				reader.setInput(input, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width > MAX_SOURCE_SIDE || height > MAX_SOURCE_SIDE) {
					throw new InvalidImageException(
							"Image trop grande : " + MAX_SOURCE_SIDE + " pixels de côté au maximum.");
				}
				// Sous-échantillonnage : une photo de 6 000 px est décodée directement au tiers (1 pixel sur 3)
				int step = Math.max(1, Math.max(width, height) / MAX_SIDE);
				ImageReadParam param = reader.getDefaultReadParam();
				param.setSourceSubsampling(step, step, 0, 0);
				return reader.read(0, param);
			}
			finally {
				reader.dispose();
			}
		}
		catch (InvalidImageException ex) {
			throw ex;
		}
		catch (IOException | RuntimeException ex) {
			throw new InvalidImageException("Image illisible ou endommagée.");
		}
	}

	// Côté le plus long ramené à 1 600 px ; la transparence (PNG) devient un fond blanc
	private static BufferedImage resize(BufferedImage source) {
		double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
		int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.drawImage(source, 0, 0, width, height, null);
		}
		finally {
			graphics.dispose();
		}
		return target;
	}

	private static byte[] encode(BufferedImage image) {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(output);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			// Métadonnées null : le fichier écrit ne contient que les pixels
			writer.write(null, new IIOImage(image, null, null), param);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Encodage JPEG impossible", ex);
		}
		finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	private static boolean isJpeg(byte[] data) {
		return data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
	}

	private static boolean isPng(byte[] data) {
		byte[] signature = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
		if (data.length < signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if (data[i] != signature[i]) {
				return false;
			}
		}
		return true;
	}

}
