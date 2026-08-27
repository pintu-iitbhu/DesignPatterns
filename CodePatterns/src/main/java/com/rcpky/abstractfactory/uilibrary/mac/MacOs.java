package com.rcpky.abstractfactory.uilibrary.mac;

import com.rcpky.abstractfactory.uilibrary.OperatingSystem;
import com.rcpky.abstractfactory.uilibrary.UIFactory;
import com.rcpky.abstractfactory.uilibrary.features.Button;
import com.rcpky.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.abstractfactory.uilibrary.features.TextField;

public class MacOs implements OperatingSystem {

    private final Button button;
    private final Checkbox checkbox;
    private final TextField textField;

    public MacOs(UIFactory factory) {
        this.button    = factory.createButton();
        this.checkbox  = factory.createCheckbox();
        this.textField = factory.createTextField();
    }

    public void renderAll() {
        button.render();
        checkbox.render();
        textField.render();
    }

    @Override
    public void show() {
        System.out.println("[MacOs] Window is now visible");
        renderAll();
    }

    @Override
    public void hide() {
        System.out.println("[MacOs] Window hidden behind other apps");
    }

    @Override
    public void minimize() {
        System.out.println("[MacOs] Window minimized to Dock with genie effect");
    }

    @Override
    public void maximize() {
        System.out.println("[MacOs] Window entered full-screen (green traffic light)");
    }

    @Override
    public void close() {
        System.out.println("[MacOs] Window closed (red traffic light)");
    }

    @Override
    public void resize() {
        System.out.println("[MacOs] Window resized — live layout update");
    }
}
