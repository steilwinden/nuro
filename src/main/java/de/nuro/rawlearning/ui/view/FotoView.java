package de.nuro.rawlearning.ui.view;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * A Designer generated component for the foto-view.html template.
 *
 * Designer will add and remove fields with @Id mappings but
 * does not overwrite or otherwise change this file.
 */
@Tag("foto-view")
@HtmlImport("src/view/foto-view.html")
@Route(value = "foto")
@RouteAlias(value = "")
//@Push
public class FotoView extends PolymerTemplate<FotoView.FotoViewModel> {

    private static final long serialVersionUID = 1L;

    private FotoPresenter presenter;

    @Id("main-layout")
    private VerticalLayout mainLayout;
    @Id("zahlSpan")
    private Span zahlSpan;
    @Id("upload")
    private Upload upload;
    @Id("init")
    private Button init;

    /**
     * This model binds properties between FotoView and foto-view.html
     */
    public interface FotoViewModel extends TemplateModel {
        // Add setters and getters for template properties here.
    }

    /**
     * Creates a new FotoView.
     */
    @Autowired
    public FotoView(final FotoPresenter presenter) {

        this.presenter = presenter;
        mainLayout.setFlexGrow(2, zahlSpan);
        zahlSpan.setText("Hello World");
        init.addClickListener(e -> initNetwork());
        presenter.init(this);
    }

    public void initNetwork() {

        UI ui = UI.getCurrent();

        Thread thread = new Thread(() -> {

            presenter.initNetwork();
            ui.access(() -> zahlSpan.setText("init Network finished"));
        });
        thread.start();
    }

    public Upload getUpload() {
        return upload;
    }

}
