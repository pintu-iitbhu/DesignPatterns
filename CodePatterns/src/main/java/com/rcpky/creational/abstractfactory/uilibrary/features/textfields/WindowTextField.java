package com.rcpky.creational.abstractfactory.uilibrary.features.textfields;

import com.rcpky.creational.abstractfactory.uilibrary.features.TextField;

public class WindowTextField implements TextField {

    private String text = "";

    @Override
    public void render() {
        System.out.println("[Windows] Rendering underlined text field with Fluent Design — value: \"" + text + "\"");
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        System.out.println("[Windows] Text field updated — value: \"" + text + "\"");
    }

    @Override
    public void clear() {
        this.text = "";
        System.out.println("[Windows] Text field cleared — placeholder restored");
    }
}
