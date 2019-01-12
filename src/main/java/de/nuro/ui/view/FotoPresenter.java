package de.nuro.ui.view;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.spring.annotation.SpringComponent;

import de.nuro.service.ImageService;
import de.nuro.service.NetworkService;

@SpringComponent
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FotoPresenter {

    private FotoView view;
    private ImageService imageService;
    private NetworkService networkService;

    @Autowired
    public FotoPresenter(final ImageService imageService, final NetworkService networkService) {
        super();
        this.imageService = imageService;
        this.networkService = networkService;
    }

    public void init(final FotoView view) {
        this.view = view;
        initUploaderImage();
    }

    private void initUploaderImage() {

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        view.getUpload().setReceiver(buffer);
        view.getUpload().setAcceptedFileTypes("image/jpeg", "image/jpg");

        view.getUpload().addSucceededListener(event -> {
            String attachmentName = event.getFileName();
            String mimeType = event.getMIMEType();
            try {
                // The image can be jpg png or gif, but we store it always as png file in this example

                byte[] bytes = IOUtils.toByteArray(buffer.getInputStream(attachmentName));
                byte[] bytesRotated = imageService.rotateImage(bytes, mimeType);
//                byte[] bytesRotatedAndResized = imageService.resizeImage(bytesRotated);
                ByteArrayInputStream bis = new ByteArrayInputStream(bytesRotated);

                BufferedImage inputImage = ImageIO.read(bis);
//                BufferedImage grayscaleImage = imageService.invertAndGrayscale(inputImage);
                File tmpFile = new File(NetworkService.TMP_FOLDER + "/foto.png");
                ImageIO.write(inputImage, "png", tmpFile);

//                networkService.guessNumber();
                networkService.simple();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
