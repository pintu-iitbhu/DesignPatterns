package com.rcpky.abstractfactory.uilibrary.window;

import com.rcpky.abstractfactory.uilibrary.OperatingSystem;
import com.rcpky.abstractfactory.uilibrary.UIFactory;
import com.rcpky.abstractfactory.uilibrary.features.Button;
import com.rcpky.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.abstractfactory.uilibrary.features.TextField;

public class WindowOs implements OperatingSystem {

    private final Button button;
    private final Checkbox checkbox;
    private final TextField textField;

    public WindowOs(UIFactory factory) {
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
        System.out.println("[WindowOs] Window is now visible");
        renderAll();
    }

    @Override
    public void hide() {
        System.out.println("[WindowOs] Window minimized to taskbar");
    }

    @Override
    public void minimize() {
        System.out.println("[WindowOs] Window minimized — appears in taskbar");
    }

    @Override
    public void maximize() {
        System.out.println("[WindowOs] Window maximized — Snap layout applied");
    }

    @Override
    public void close() {
        System.out.println("[WindowOs] Window closed (X button)");
    }

    @Override
    public void resize() {
        System.out.println("[WindowOs] Window resized — Snap grid updated");
    }
}
