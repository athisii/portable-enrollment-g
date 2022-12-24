/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.model.UnitListDetails;
import com.cdac.enrollmentstation.model.Units;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoCompleteBox implements EventHandler<KeyEvent> {
    public ComboBox<Units> comboBox;
    public ObservableList<Units> data;
    private Integer sid;

    public AutoCompleteBox(final ComboBox<Units> comboBox) {

        try {

            comboBox.valueProperty().addListener((obs, oldval, newval) -> {
                if (newval != null)
                    System.out.println("Selected unit in 1: " + newval.getCaption() + ". ID: " + newval.getValue());
            });

            String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
            // System.out.println("Output str : "+json);
            ObjectMapper objectmapper = new ObjectMapper();
            UnitListDetails details;
            details = objectmapper.readValue(json1, UnitListDetails.class);
            this.comboBox = comboBox;
            //this.data = comboBox.getItems();        
            this.data = FXCollections.observableArrayList(details.getUnits()).sorted();

            this.doAutoCompleteBox();
        } catch (JsonProcessingException ex) {
            Logger.getLogger(AutoCompleteBox.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public AutoCompleteBox(final ComboBox comboBox, Integer sid) {
        this.comboBox = comboBox;
        this.data = comboBox.getItems();
        // this.data = comboBox.getSelectionModel().getSelectedItem();
        this.sid = sid;

        this.doAutoCompleteBox();

    }

    private void doAutoCompleteBox() {
        this.comboBox.setEditable(true);
        this.comboBox.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {//mean onfocus
                this.comboBox.show();

            }
        });

        this.comboBox.getEditor().setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                if (event.getClickCount() == 2) {
                    return;
                }
            }
            this.comboBox.show();
        });


        this.comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            moveCaret(this.comboBox.getEditor().getText().length());
            System.out.println("Selected unitaaa: " + newValue + ". ID: ");
        });

        comboBox.valueProperty().addListener((obs, oldval, newval) -> {
            if (newval != null)
                System.out.println("Selected unit in 1: " + newval.getCaption() + ". ID: " + newval.getValue());
        });

        comboBox.valueProperty().addListener((obs, oldval, newval) -> {
            if (newval != null) System.out.println("Selected unitaa: " + newval);

        });
        this.comboBox.setOnKeyPressed(t -> comboBox.hide());

        this.comboBox.setOnKeyReleased(AutoCompleteBox.this);

        if (this.sid != null) this.comboBox.getSelectionModel().select(this.sid);
    }

    @Override
    public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB) {
            return;
        }

        if (event.getCode() == KeyCode.BACK_SPACE) {
            String str = this.comboBox.getEditor().getText();
            if (str != null && str.length() > 0) {
                str = str.substring(0, str.length() - 1);
            }
            if (str != null) {
                this.comboBox.getEditor().setText(str);
                moveCaret(str.length());
            }
            this.comboBox.getSelectionModel().clearSelection();
        }

        if (event.getCode() == KeyCode.ENTER && comboBox.getSelectionModel().getSelectedIndex() > -1) return;

        setItems();
    }

    private void setItems() {
        ObservableList<Units> list = FXCollections.observableArrayList();

        for (Units datum : this.data) {
            String s = this.comboBox.getEditor().getText().toLowerCase();
            if (datum.toString().toLowerCase().contains(s.toLowerCase())) {
                list.add(datum);
            }
        }

        if (list.isEmpty()) this.comboBox.hide();
        UnitListDetails details = new UnitListDetails();
        details.setUnits(list);
        ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits());
        this.comboBox.setItems(units);


        this.comboBox.show();

    }

    private void moveCaret(int textLength) {
        this.comboBox.getEditor().positionCaret(textLength);
    }


}