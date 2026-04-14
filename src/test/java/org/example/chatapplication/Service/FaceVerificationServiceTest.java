package org.example.chatapplication.Service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceVerificationServiceTest {
    private final FaceVerificationService faceVerificationService = new FaceVerificationService();

    @Test
    void sameFaceWithDifferentClothesShouldMatch() throws Exception {
        MockMultipartFile reference = imageFile(createFaceLikeImage(0, 0, 0, new Color(120, 70, 45)));
        MockMultipartFile differentShirt = imageFile(createFaceLikeImage(0, 0, 0, new Color(30, 120, 200)));

        String referenceSignature = faceVerificationService.generateSignature(reference);
        String shirtChangedSignature = faceVerificationService.generateSignature(differentShirt);

        assertThat(referenceSignature).startsWith("v2:");
        assertThat(faceVerificationService.distance(referenceSignature, shirtChangedSignature)).isLessThanOrEqualTo(96);
        assertThat(faceVerificationService.matches(referenceSignature, differentShirt)).isTrue();
    }

    @Test
    void sameFaceWithSmallShiftAndBrightnessVariationShouldMatch() throws Exception {
        MockMultipartFile reference = imageFile(createFaceLikeImage(0, 0, 0, new Color(120, 70, 45)));
        MockMultipartFile shifted = imageFile(createFaceLikeImage(10, -6, 12, new Color(120, 70, 45)));

        String referenceSignature = faceVerificationService.generateSignature(reference);
        String shiftedSignature = faceVerificationService.generateSignature(shifted);

        assertThat(referenceSignature).startsWith("v2:");
        assertThat(faceVerificationService.distance(referenceSignature, shiftedSignature)).isLessThanOrEqualTo(96);
        assertThat(faceVerificationService.matches(referenceSignature, shifted)).isTrue();
    }

    @Test
    void invalidImageShouldBeRejected() {
        MockMultipartFile invalid = new MockMultipartFile("faceImage", "face.txt", "text/plain", "not-an-image".getBytes());

        assertThatThrownBy(() -> faceVerificationService.generateSignature(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid image file");
    }

    private BufferedImage createFaceLikeImage(int shiftX, int shiftY, int brightnessDelta, Color shirtColor) {
        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(clamp(245 + brightnessDelta), clamp(225 + brightnessDelta), clamp(205 + brightnessDelta)));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            g.setColor(new Color(clamp(210 + brightnessDelta), clamp(175 + brightnessDelta), clamp(145 + brightnessDelta)));
            g.fillOval(60 + shiftX, 35 + shiftY, 120, 150);

            g.setColor(Color.BLACK);
            g.fillOval(92 + shiftX, 85 + shiftY, 14, 18);
            g.fillOval(134 + shiftX, 85 + shiftY, 14, 18);
            g.fillRoundRect(100 + shiftX, 130 + shiftY, 40, 10, 8, 8);

            g.setColor(shirtColor);
            g.fillRoundRect(82 + shiftX, 55 + shiftY, 76, 18, 12, 12);
        } finally {
            g.dispose();
        }
        return image;
    }

    private MockMultipartFile imageFile(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new MockMultipartFile("faceImage", "face.png", "image/png", outputStream.toByteArray());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

