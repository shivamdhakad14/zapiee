package com.zapiee.mediaservice.validation;

import com.zapiee.mediaservice.dto.MediaProperties;
import com.zapiee.mediaservice.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class FileValidator {

    private final MediaProperties mediaProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was provided");
        }

        if (file.getSize() > mediaProperties.maxFileSizeBytes()) {
            throw new InvalidFileException(
                    "File exceeds the maximum allowed size of " + (mediaProperties.maxFileSizeBytes() / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || mediaProperties.allowedContentTypes().stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new InvalidFileException(
                    "Unsupported content type '" + contentType + "'. Allowed: " + mediaProperties.allowedContentTypes());
        }

        validateDimensions(file);
    }

    private void validateDimensions(MultipartFile file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidFileException("File content does not look like a valid image");
            }
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int max = mediaProperties.maxImageDimension();
                if (width > max || height > max) {
                    throw new InvalidFileException(
                            "Image dimensions " + width + "x" + height + " exceed the maximum of " + max + "x" + max);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidFileException("Could not read image file: " + e.getMessage());
        }
    }
}
