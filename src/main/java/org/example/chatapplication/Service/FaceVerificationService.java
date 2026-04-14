package org.example.chatapplication.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
public class FaceVerificationService {
    private static final int SIGNATURE_SIZE = 24;
    private static final int MAX_HAMMING_DISTANCE = 88;
    private static final String MULTI_SIGNATURE_PREFIX = "v2:";
    private static final String VARIANT_SEPARATOR = "|";
    private static final float FACE_REGION_TOP_RATIO = 0.04f;
    private static final float FACE_REGION_BOTTOM_RATIO = 0.60f;

    public String generateSignature(MultipartFile file) {
        BufferedImage image = readImage(file);
        return generateSignature(image);
    }

    public String generateSignature(Path imagePath) {
        BufferedImage image = readImage(imagePath);
        return generateSignature(image);
    }

    public String generateSignatureFromStoredPath(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            throw new IllegalArgumentException("Stored face template path is required");
        }

        String normalizedPath = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        return generateSignature(Paths.get(normalizedPath));
    }

    public String generateSignature(BufferedImage image) {
        List<String> variants = List.of(
                Base64.getEncoder().encodeToString(buildSignature(enhanceContrast(normalizeFaceRegion(image, 0.00f, 0.00f)))),
                Base64.getEncoder().encodeToString(buildSignature(enhanceContrast(normalizeFaceRegion(image, 0.03f, 0.04f)))),
                Base64.getEncoder().encodeToString(buildSignature(enhanceContrast(normalizeFaceRegion(image, 0.00f, 0.07f))))
        );
        return MULTI_SIGNATURE_PREFIX + String.join(VARIANT_SEPARATOR, variants);
    }

    public boolean matches(String expectedSignature, MultipartFile file) {
        if (!StringUtils.hasText(expectedSignature)) {
            return false;
        }

        String actualSignature = generateSignature(file);
        return distance(expectedSignature, actualSignature) <= MAX_HAMMING_DISTANCE;
    }

    public int distance(String leftSignature, String rightSignature) {
        if (!StringUtils.hasText(leftSignature) || !StringUtils.hasText(rightSignature)) {
            return Integer.MAX_VALUE;
        }

        List<String> leftVariants = splitVariants(leftSignature);
        List<String> rightVariants = splitVariants(rightSignature);

        int best = Integer.MAX_VALUE;
        for (String leftVariant : leftVariants) {
            for (String rightVariant : rightVariants) {
                best = Math.min(best, hammingDistance(leftVariant, rightVariant));
            }
        }
        return best;
    }

    private BufferedImage readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Face image is required");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("Face image must be a valid image file");
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read face image", ex);
        }
    }

    private BufferedImage readImage(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Face image path is required");
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("Stored face template must be a valid image file");
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read stored face template image", ex);
        }
    }

    private BufferedImage normalizeFaceRegion(BufferedImage source, float leftTrimRatio, float topTrimRatio) {
        int width = source.getWidth();
        int height = source.getHeight();
        int squareSize = Math.min(width, height);

        int x = clampToBounds(Math.round((width - squareSize) / 2f + squareSize * leftTrimRatio), Math.max(0, width - squareSize));
        int y = clampToBounds(Math.round((height - squareSize) / 2f - squareSize * topTrimRatio), Math.max(0, height - squareSize));

        BufferedImage square = source.getSubimage(x, y, squareSize, squareSize);
        int focusHeight = Math.max(1, Math.round(squareSize * FACE_REGION_BOTTOM_RATIO));
        int focusTop = clampToBounds(Math.round(squareSize * FACE_REGION_TOP_RATIO), Math.max(0, squareSize - focusHeight));
        BufferedImage faceRegion = square.getSubimage(0, focusTop, squareSize, focusHeight);

        BufferedImage grayscale = new BufferedImage(SIGNATURE_SIZE, SIGNATURE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscale.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, SIGNATURE_SIZE, SIGNATURE_SIZE);

            double scale = Math.min((double) SIGNATURE_SIZE / faceRegion.getWidth(), (double) SIGNATURE_SIZE / faceRegion.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(faceRegion.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(faceRegion.getHeight() * scale));
            int drawX = (SIGNATURE_SIZE - drawWidth) / 2;
            int drawY = (SIGNATURE_SIZE - drawHeight) / 2;
            graphics.drawImage(faceRegion, drawX, drawY, drawWidth, drawHeight, null);
        } finally {
            graphics.dispose();
        }
        return grayscale;
    }

    private BufferedImage enhanceContrast(BufferedImage source) {
        byte[] pixels = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
        if (pixels.length == 0) {
            return source;
        }

        int min = 255;
        int max = 0;
        for (byte pixel : pixels) {
            int value = pixel & 0xFF;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        if (max <= min) {
            return source;
        }

        BufferedImage stretched = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        byte[] targetPixels = ((DataBufferByte) stretched.getRaster().getDataBuffer()).getData();
        double scale = 255.0 / (max - min);

        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i] & 0xFF;
            int normalized = (int) Math.round((value - min) * scale);
            targetPixels[i] = (byte) Math.max(0, Math.min(255, normalized));
        }

        return stretched;
    }

    private byte[] buildSignature(BufferedImage image) {
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        int total = 0;
        for (byte pixel : pixels) {
            total += pixel & 0xFF;
        }
        int average = total / pixels.length;

        byte[] signature = new byte[(pixels.length + 7) / 8];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i] & 0xFF;
            if (pixel >= average) {
                signature[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return signature;
    }

    private int hammingDistance(String leftSignature, String rightSignature) {
        byte[] left = decode(leftSignature);
        byte[] right = decode(rightSignature);
        int maxLength = Math.max(left.length, right.length);
        int distance = 0;

        for (int i = 0; i < maxLength; i++) {
            int leftByte = i < left.length ? left[i] & 0xFF : 0;
            int rightByte = i < right.length ? right[i] & 0xFF : 0;
            distance += Integer.bitCount(leftByte ^ rightByte);
        }

        return distance;
    }

    private List<String> splitVariants(String signature) {
        String trimmed = signature.trim();
        if (trimmed.startsWith(MULTI_SIGNATURE_PREFIX)) {
            String payload = trimmed.substring(MULTI_SIGNATURE_PREFIX.length());
            if (!StringUtils.hasText(payload)) {
                return List.of();
            }
            return List.of(payload.split("\\|", -1));
        }
        return List.of(trimmed);
    }

    private int clampToBounds(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private byte[] decode(String signature) {
        try {
            return Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Stored face signature is invalid", ex);
        }
    }
}

