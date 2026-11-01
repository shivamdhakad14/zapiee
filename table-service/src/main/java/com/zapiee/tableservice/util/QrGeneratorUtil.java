package com.zapiee.tableservice.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class QrGeneratorUtil {

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;


    public String generateQrBase64(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("QR content cannot be empty.");
        }

        try {

            Map<EncodeHintType, Object> hints = new HashMap<>();

            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            BitMatrix bitMatrix = qrCodeWriter.encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            QR_WIDTH,
                            QR_HEIGHT,
                            hints
                    );

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            addBrandText(qrImage);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ImageIO.write(
                    qrImage,
                    "PNG",
                    outputStream
            );

            return Base64
                    .getEncoder()
                    .encodeToString(outputStream.toByteArray());

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to generate QR code.",
                    ex
            );
        }
    }


    private void addBrandText(BufferedImage qrImage) {

        Graphics2D graphics =
                qrImage.createGraphics();

        try {

            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            String brandText = "Zapiee";

            Font font =
                    new Font(
                            "Arial",
                            Font.BOLD,
                            16
                    );

            graphics.setFont(font);

            FontMetrics metrics = graphics.getFontMetrics();

            int textWidth = metrics.stringWidth(brandText);

            int textHeight = metrics.getHeight();

            int x = (qrImage.getWidth() - textWidth) / 2;

            int y = (qrImage.getHeight() - textHeight) / 2
                            + metrics.getAscent();

            int padding = 10;

            graphics.setColor(Color.WHITE);

            graphics.fillRoundRect(
                    x - padding,
                    y - metrics.getAscent() - padding,
                    textWidth + (padding * 2),
                    textHeight + (padding * 2),
                    12,
                    12
            );

            graphics.setColor(Color.BLACK);

            graphics.drawString(
                    brandText,
                    x,
                    y
            );

        } finally {
            graphics.dispose();
        }
    }
}

