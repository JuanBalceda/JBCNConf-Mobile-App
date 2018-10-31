/**
 * Copyright (c) 2016, 2018 Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.RatingData;
import com.devoxx.model.Session;
import com.devoxx.model.Vote;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.control.Rating;
import com.gluonhq.charm.glisten.control.TextArea;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class VotePresenter extends GluonPresenter<DevoxxApplication> {

    @FXML private ResourceBundle bundle = ResourceBundle.getBundle("com/devoxx/views/vote");
    @FXML private View vote;
    @FXML private Label title;
    @FXML private Label ratingLabel;
    @FXML private Rating rating;
    @FXML private Label compliment;
    @FXML private TextField feedbackTF;
    @FXML private ListView<RatingData> comments;

    @Inject private Service service;

    private Session session;
    private TextArea feedback;
    private Dialog<String> feedbackDialog;

    public void initialize() {

        vote.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setTitleText(DevoxxView.VOTE.getTitle());
            appBar.setNavIcon(MaterialDesignIcon.CLEAR.button(e -> {
                MobileApplication.getInstance().switchToPreviousView().ifPresent(view -> {
                    DevoxxView.getAppView(view).ifPresent(av -> {
                        av.getPresenter().ifPresent(presenter -> {
                            ((SessionPresenter)presenter).showSession(session, SessionPresenter.Pane.INFO);
                        });
                    });
                });
            }));
            appBar.getActionItems().clear();
        });

        updateRating((int) rating.getRating());

        rating.ratingProperty().addListener((o, ov, nv) -> {
            comments.scrollTo(0);
            updateRating(nv.intValue());
        });

        comments.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        comments.setCellFactory(param -> new UnselectListCell<RatingData>() {

            @Override
            protected void updateItem(RatingData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setGraphic(null);
                } else {
                    setText(item.getText());
                    if (Util.isEmptyString(item.getImageUrl())) {
                        imageView.setImage(randomImage());
                    } else {
                        imageView.setImage(new Image(item.getImageUrl()));
                    }
                    setGraphic(imageView);
                }
            }
        });
    }

    public void showVote(Session session) {
        this.session = session;
        if (session != null) {
            title.setText(session.getTitle());
        }
        // Remove last feedback
        if (feedback != null) {
            feedback.setText("");
        }
        feedbackTF.clear();
        rating.setRating(4);
        comments.getSelectionModel().clearSelection();
    }

    @FXML
    private void submit() {
        // Submit Vote to Backend
        service.voteTalk(createVote(session.getTalk().getId()));
        // Switch to INFO Pane
        MobileApplication.getInstance().switchToPreviousView().ifPresent(view -> {
            DevoxxView.getAppView(view).ifPresent(av -> {
                av.getPresenter().ifPresent(presenter -> {
                    ((SessionPresenter)presenter).showSession(session, SessionPresenter.Pane.INFO);
                });
            });
        });
        // Show Toast
        Toast toast = new Toast(DevoxxBundle.getString("OTN.VOTEPANE.SUBMIT_VOTE"));
        toast.show();
    }

    private Vote createVote(String talkId) {
        Vote vote = new Vote(talkId);
        // vote.setContent(content.getText());
        if (comments.getSelectionModel().getSelectedItem() != null) {
            vote.setDelivery(comments.getSelectionModel().getSelectedItem().getText());
        }
        if (feedback != null) {
            vote.setOther(feedback.getText());
        }
        vote.setValue((int) rating.getRating());
        return vote;
    }

    private void updateRating(int rating) {
        switch (rating) {
            case 5:
                ratingLabel.setText(bundle.getString("OTN.VOTE.EXCELLENT"));
                compliment.setText(bundle.getString("OTN.VOTE.COMPLIMENT"));
                comments.setItems(service.retrieveVoteTexts(5));
                break;
            case 4:
                ratingLabel.setText(bundle.getString("OTN.VOTE.VERY.GOOD"));
                compliment.setText(bundle.getString("OTN.VOTE.COMPLIMENT"));
                comments.setItems(service.retrieveVoteTexts(4));
                break;
            case 3:
                ratingLabel.setText(bundle.getString("OTN.VOTE.GOOD"));
                compliment.setText(bundle.getString("OTN.VOTE.COMPLIMENT"));
                comments.setItems(service.retrieveVoteTexts(3));
                break;
            case 2:
                ratingLabel.setText(bundle.getString("OTN.VOTE.FAIR"));
                compliment.setText(bundle.getString("OTN.VOTE.IMPROVEMENT"));
                comments.setItems(service.retrieveVoteTexts(2));
                break;
            case 1:
                ratingLabel.setText(bundle.getString("OTN.VOTE.POOR"));
                compliment.setText(bundle.getString("OTN.VOTE.IMPROVEMENT"));
                comments.setItems(service.retrieveVoteTexts(1));
                break;
        }
    }

    @FXML
    private void showFeedback() {
        if (feedback == null) {
            feedback = new TextArea();
        }
        feedback.setUserData(feedback.getText());
        if (feedbackDialog == null) {
            feedbackDialog = new Dialog<>();
            feedbackDialog.setContent(new VBox(10, new Label(bundle.getString("OTN.VOTE.TEXT")), feedback));
            Button saveButton = new Button("Save");
            saveButton.setOnAction(e -> feedbackDialog.hide());
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(e -> {
                feedback.setText((String) feedback.getUserData());
                feedbackDialog.hide();
            });
            feedbackDialog.getButtons().addAll(cancelButton, saveButton);
            feedbackDialog.setOnCloseRequest(e -> feedbackTF.setText(feedback.getText()));
        }
        feedbackDialog.showAndWait();
    }

    private class UnselectListCell<T> extends ListCell<T> {

        protected final ImageView imageView;

        public UnselectListCell() {
            imageView = new ImageView();
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            Platform.runLater(() -> prefWidthProperty().bind(getListView().widthProperty().divide(3.2)));
            addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!isEmpty()) {
                    MultipleSelectionModel<T> selectionModel = getListView().getSelectionModel();
                    int index = getIndex();
                    if (selectionModel.getSelectedIndex() == index) {
                        selectionModel.clearSelection(index);
                    } else {
                        selectionModel.select(index);
                    }
                    event.consume();
                }
            });
        }
    }

    private Image randomImage() {
        List<String> stars = Arrays.asList(
                VotePresenter.class.getResource("/star/star-1.png").toExternalForm(),
                VotePresenter.class.getResource("/star/star-2.png").toExternalForm(),
                VotePresenter.class.getResource("/star/star-3.png").toExternalForm(),
                VotePresenter.class.getResource("/star/star-4.png").toExternalForm()
        );
        return new Image(stars.get(new Random().nextInt(stars.size())));
    }
}
