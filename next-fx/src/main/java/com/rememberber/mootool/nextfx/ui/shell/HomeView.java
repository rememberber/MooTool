package com.rememberber.mootool.nextfx.ui.shell;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import com.rememberber.mootool.nextfx.platform.DesktopLinks;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public final class HomeView extends ScrollPane {

    public HomeView(ProductIdentity identity, Translator translator) {
        getStyleClass().add("mt-home");
        setFitToWidth(true);
        VBox content = new VBox(20);
        content.getStyleClass().add("mt-home-content");
        content.setPadding(new Insets(32, 40, 48, 40));
        content.setMaxWidth(760);

        HBox hero = new HBox(20);
        hero.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView(new Image(HomeView.class.getResource("/brand/mootool-logo.png").toExternalForm()));
        logo.setFitWidth(96);
        logo.setFitHeight(96);
        logo.setPreserveRatio(true);
        VBox identityBox = new VBox(6);
        Hyperlink website = link("mootool.luoboduner.com", () -> DesktopLinks.open("home"));
        Label title = new Label(identity.displayName());
        title.getStyleClass().add("mt-home-title");
        Label version = new Label("v" + identity.version());
        version.getStyleClass().add("mt-home-version");
        Label tagline = new Label(translator.t("app.home.tagline"));
        tagline.getStyleClass().add("mt-muted");
        Label author = new Label(translator.t("app.home.author"));
        author.getStyleClass().add("mt-muted");
        identityBox.getChildren().addAll(website, new HBox(12, title, version), tagline, author);
        hero.getChildren().addAll(logo, identityBox);

        content.getChildren().addAll(
                hero,
                section(translator.t("app.home.about.title"), about(translator)),
                section(translator.t("app.home.contributors.title"), contributors(translator)),
                section(translator.t("app.home.sponsor.title"), sponsor(translator)),
                section(translator.t("app.home.source.title"), links(
                        link("GitHub", () -> DesktopLinks.open("github")),
                        link("Gitee", () -> DesktopLinks.open("gitee"))
                )),
                section(translator.t("app.home.help.title"), link(translator.t("app.home.help.issue"), () -> DesktopLinks.open("issues"))),
                section(translator.t("app.home.otherWorks.title"), links(
                        work("WePush", translator.t("app.home.wePush.desc"), "wePush"),
                        work("MooInfo", translator.t("app.home.mooInfo.desc"), "mooInfo")
                ))
        );
        VBox wrapper = new VBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        setContent(wrapper);
    }

    private static VBox about(Translator translator) {
        VBox box = new VBox(8);
        box.getChildren().addAll(
                paragraph(translator.t("app.home.about.line1")),
                paragraph(translator.t("app.home.about.lineDaily")),
                paragraph(translator.t("app.home.about.line2")),
                paragraph(translator.t("app.home.about.line2Note")),
                paragraph(translator.t("app.home.about.line3")),
                paragraph(translator.t("app.home.about.line4")),
                paragraph(translator.t("app.home.about.line5"))
        );
        return box;
    }

    private static VBox contributors(Translator translator) {
        FlowPane pane = new FlowPane(8, 8);
        for (String name : List.of("CassianFlorin", "felixcn", "felixnan168", "Lyp", "sunsence", "rememberber")) {
            String page = switch (name) {
                case "CassianFlorin" -> "contributorCassianFlorin";
                case "felixcn" -> "contributorFelixcn";
                case "felixnan168" -> "contributorFelixnan168";
                case "Lyp" -> "contributorLyp";
                case "sunsence" -> "contributorSunsence";
                default -> "contributorRememberber";
            };
            pane.getChildren().add(link(name, () -> DesktopLinks.open(page)));
        }
        Label thanks = new Label(translator.t("app.home.contributors.thanks"));
        thanks.getStyleClass().add("mt-muted");
        return new VBox(12, pane, thanks);
    }

    private static VBox sponsor(Translator translator) {
        ImageView qr = new ImageView(new Image(HomeView.class.getResource("/brand/wx-zanshang.jpg").toExternalForm()));
        qr.setFitWidth(180);
        qr.setPreserveRatio(true);
        Label prompt = paragraph(translator.t("app.home.sponsor.prompt"));
        Label tip = new Label(translator.t("app.home.sponsor.tip"));
        tip.getStyleClass().add("mt-muted");
        return new VBox(12, prompt, qr, tip);
    }

    private static VBox section(String title, javafx.scene.Node body) {
        Label heading = new Label(title);
        heading.getStyleClass().add("mt-section-title");
        return new VBox(10, heading, body);
    }

    private static VBox links(Hyperlink... items) {
        return new VBox(6, items);
    }

    private static Label paragraph(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private static Hyperlink link(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(event -> action.run());
        return link;
    }

    private static Hyperlink work(String name, String description, String pageId) {
        Hyperlink link = new Hyperlink(name + " — " + description);
        link.setOnAction(event -> DesktopLinks.open(pageId));
        return link;
    }
}
