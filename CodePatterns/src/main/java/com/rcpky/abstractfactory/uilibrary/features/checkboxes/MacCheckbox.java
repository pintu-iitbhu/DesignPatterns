package com.rcpky.abstractfactory.uilibrary.features.checkboxes;

import com.rcpky.abstractfactory.uilibrary.features.Checkbox;

public class MacCheckbox implements Checkbox {

    private boolean checked = false;

    @Override
    public void render() {
        System.out.println("[Mac] Rendering checkbox with SF Symbol style — " + (checked ? "✓ checked" : "○ unchecked"));
    }

    @Override
    public void check() {
        checked = true;
        System.out.println("[Mac] Checkbox checked — smooth spring animation");
    }

    @Override
    public void uncheck() {
        checked = false;
        System.out.println("[Mac] Checkbox unchecked — fade out animation");
    }

    @Override
    public boolean isChecked() {
        return checked;
    }
}
