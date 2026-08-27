package com.rcpky.abstractfactory.uilibrary.features.checkboxes;

import com.rcpky.abstractfactory.uilibrary.features.Checkbox;

public class WindowCheckbox implements Checkbox {

    private boolean checked = false;

    @Override
    public void render() {
        System.out.println("[Windows] Rendering checkbox with Fluent Design style — " + (checked ? "☑ checked" : "☐ unchecked"));
    }

    @Override
    public void check() {
        checked = true;
        System.out.println("[Windows] Checkbox checked — accent color fill");
    }

    @Override
    public void uncheck() {
        checked = false;
        System.out.println("[Windows] Checkbox unchecked — border only");
    }

    @Override
    public boolean isChecked() {
        return checked;
    }
}
