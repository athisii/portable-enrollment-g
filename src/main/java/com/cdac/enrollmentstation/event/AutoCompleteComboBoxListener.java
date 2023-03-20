/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.event;

import com.cdac.enrollmentstation.model.Unit;
import static java.util.Locale.filter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author root
 */
public class AutoCompleteComboBoxListener<T> implements EventHandler<KeyEvent> {

    //private ComboBox<Units> comboBox;
    private ComboBox comboBox;
    private StringBuilder sb;
    private ObservableList<Unit> data;
    private boolean moveCaretToPos = false;
    private int caretPos;
    private ComboBoxListViewSkin cbSkin;

    //public AutoCompleteComboBoxListener(final ComboBox<Units> comboBox) {
     public AutoCompleteComboBoxListener(final ComboBox comboBox) {
        this.comboBox = comboBox;
        sb = new StringBuilder();
        data = comboBox.getItems();

        this.comboBox.setEditable(true);
       
        // for Spacebar
        //this.comboBox = comboBox;
    cbSkin = new ComboBoxListViewSkin(comboBox);
    //originalItems = FXCollections.observableArrayList(cmb.getItems());
    //comboBox.setOnKeyPressed(this::handleOnKeyPressed);
    //comboBox.setOnHidden(this::handleOnHiding);
    comboBox.setSkin(cbSkin);
    cbSkin.getPopupContent().addEventFilter(KeyEvent.KEY_PRESSED, (event) -> {
        if(event.getCode() == KeyCode.SPACE){
           // filter += " ";
            event.consume();}                    
    });
        //
        
        
        /*
        comboBox.getEditor().focusedProperty().addListener(observable -> {
        if (0 > comboBox.getSelectionModel().getSelectedIndex()) {
            comboBox.getEditor().setText(null);
        }
        });*/
        this.comboBox.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent t) {
                comboBox.hide();
            }
        });
        this.comboBox.setOnKeyReleased(AutoCompleteComboBoxListener.this);
    }

    @Override
    public void handle(KeyEvent event) {

        if(event.getCode() == KeyCode.UP) {
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if(event.getCode() == KeyCode.DOWN) {
            if(!comboBox.isShowing()) {
                comboBox.show();
            }
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if(event.getCode() == KeyCode.BACK_SPACE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        } else if(event.getCode() == KeyCode.DELETE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        }

        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT
                || event.isControlDown() || event.getCode() == KeyCode.HOME
                || event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB ) {
            return;
        }
        
 

        ObservableList list = FXCollections.observableArrayList();
        for (int i=0; i<data.size(); i++) {
            if(data.get(i).toString().toLowerCase().startsWith(
                AutoCompleteComboBoxListener.this.comboBox
                .getEditor().getText().toLowerCase())) {
                list.add(data.get(i));
            }
        }
        String t = comboBox.getEditor().getText();

        comboBox.setItems(list);
        comboBox.getEditor().setText(t);
        if(!moveCaretToPos) {
            caretPos = -1;
        }
        moveCaret(t.length());
        if(!list.isEmpty()) {
            comboBox.show();
        }
    }

    private void moveCaret(int textLength) {
        if(caretPos == -1) {
            comboBox.getEditor().positionCaret(textLength);
        } else {
            comboBox.getEditor().positionCaret(caretPos);
        }
        moveCaretToPos = false;
    }

}