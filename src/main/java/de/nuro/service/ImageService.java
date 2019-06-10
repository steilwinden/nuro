package de.nuro.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;
import org.springframework.stereotype.Service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;

@Service
public class ImageService {

    private static final int widthLimit = 28;
    private static final int heightLimit = 28;

    public byte[] resizeImage(final byte[] bytes) throws IOException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage sourceImage = ImageIO.read(bis);
            Image thumbnail = sourceImage.getScaledInstance(widthLimit, heightLimit, Image.SCALE_SMOOTH);
            BufferedImage bufferedThumbnail =
                new BufferedImage(thumbnail.getWidth(null), thumbnail.getHeight(null), BufferedImage.TYPE_INT_RGB);
            bufferedThumbnail.getGraphics().drawImage(thumbnail, 0, 0, null);
            ImageIO.write(bufferedThumbnail, "png", baos);
            return baos.toByteArray();
        }
    }

    /**
     * @See https://stackoverflow.com/questions/21951892/how-to-determine-and-auto-rotate-images
     */
    public byte[] rotateImage(final byte[] bytes, final String mimeType) throws IOException, CompoundException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ByteArrayInputStream bisClone = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(bis);
            Metadata metadata = ImageMetadataReader.readMetadata(bisClone);
            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            int orientation = 1;
            try {
                orientation = exifIFD0Directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
            } catch (Exception e) {
                System.out.println(
                    "No EXIF information found for image. MimeType=" + mimeType + ". Image rotation will be skipped.");
            }

            switch (orientation) {
                case 1 :
                    break;
                case 2 : // Flip X
                    image = Scalr.rotate(image, Rotation.FLIP_HORZ);
                    break;
                case 3 : // PI rotation
                    image = Scalr.rotate(image, Rotation.CW_180);
                    break;
                case 4 : // Flip Y
                    image = Scalr.rotate(image, Rotation.FLIP_VERT);
                    break;
                case 5 : // - PI/2 and Flip X
                    image = Scalr.rotate(image, Rotation.CW_90);
                    image = Scalr.rotate(image, Rotation.FLIP_HORZ);
                    break;
                case 6 : // -PI/2 and -width
                    image = Scalr.rotate(image, Rotation.CW_90);
                    break;
                case 7 : // PI/2 and Flip
                    image = Scalr.rotate(image, Rotation.CW_90);
                    image = Scalr.rotate(image, Rotation.FLIP_VERT);
                    break;
                case 8 : // PI / 2
                    image = Scalr.rotate(image, Rotation.CW_270);
                    break;
                default :
                    break;
            }
            // ---- End orientation handling ----

            if (mimeType.equals("image/png")) {
                ImageIO.write(image, "png", baos);
            } else if (mimeType.equals("image/gif")) {
                ImageIO.write(image, "gif", baos);
            } else {
                ImageIO.write(image, "jpeg", baos);
            }
            return baos.toByteArray();
        }
    }
}
