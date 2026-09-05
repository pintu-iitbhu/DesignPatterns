package com.rcpky.creational.abstractfactory.uilibrary.features.textfields;

import com.rcpky.creational.abstractfactory.uilibrary.features.TextField;

public class MacTextField implements TextField {

    private String text = "";

    @Override
    public void render() {
        System.out.println("[Mac] Rendering rounded text field with focus ring — value: \"" + text + "\"");
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        System.out.println("[Mac] Text field updated — value: \"" + text + "\"");
    }

    @Override
    public void clear() {
        this.text = "";
        System.out.println("[Mac] Text field cleared with fade animation");
    }
}
