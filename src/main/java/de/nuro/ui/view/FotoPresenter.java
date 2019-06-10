package de.nuro.ui.view;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.imageio.ImageIO;

import de.nuro.service.ClusterService;
import de.nuro.service.BlackWhiteService;
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
    private BlackWhiteService blackWhiteService;
    private ClusterService clusterService;
    private NetworkService networkService;

    @Autowired
    public FotoPresenter(final ImageService imageService, final BlackWhiteService blackWhiteService,
                         final ClusterService clusterService, final NetworkService networkService) {
        super();
        this.imageService = imageService;
        this.blackWhiteService = blackWhiteService;
        this.clusterService = clusterService;
        this.networkService = networkService;
    }

    public void init(final FotoView view) {
        this.view = view;
        initUploaderImage();
    }

    private void initUploaderImage() {

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        view.getUpload().setReceiver(buffer);
        view.getUpload().setAcceptedFileTypes("image/jpeg", "image/jpg", "image/png");

        view.getUpload().addStartedListener(e -> view.getZahlSpan().setText("Ergebnis:"));

        view.getUpload().addSucceededListener(event -> {
            String attachmentName = event.getFileName();
            String mimeType = event.getMIMEType();
            try {
                // The image can be jpg png or gif, but we store it always as png file in this example

                byte[] bytes = IOUtils.toByteArray(buffer.getInputStream(attachmentName));
                byte[] bytesRotated = imageService.rotateImage(bytes, mimeType);
                byte[] bytesRotatedAndResized = imageService.resizeImage(bytesRotated);
                int threshold = blackWhiteService.calcThreshold(bytesRotatedAndResized);
                // TODO: Feature 2 umsetzen (auf Quadrat verschieben und skalieren)
                ByteArrayInputStream bis = new ByteArrayInputStream(bytesRotatedAndResized);

                BufferedImage inputImage = ImageIO.read(bis);
                BufferedImage bwImage = blackWhiteService.toBlackWhiteInverted(inputImage, threshold);
                File adhocFolder = new File(NetworkService.ADHOC_FOLDER);
                if (!adhocFolder.exists()) {
                    adhocFolder.mkdirs();
                }
                File adhocFile = new File(NetworkService.ADHOC_FOLDER + "/foto.png");
                adhocFile.createNewFile();
                ImageIO.write(bwImage, "png", adhocFile);

                int result = networkService.guessNumber();
                view.getZahlSpan().setText("Ergebnis: " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
