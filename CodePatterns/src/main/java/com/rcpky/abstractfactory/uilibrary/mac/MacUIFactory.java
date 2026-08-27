package com.rcpky.abstractfactory.uilibrary.mac;

import com.rcpky.abstractfactory.uilibrary.UIFactory;
import com.rcpky.abstractfactory.uilibrary.features.Button;
import com.rcpky.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.abstractfactory.uilibrary.features.TextField;
import com.rcpky.abstractfactory.uilibrary.features.buttons.MacButton;
import com.rcpky.abstractfactory.uilibrary.features.checkboxes.MacCheckbox;
import com.rcpky.abstractfactory.uilibrary.features.textfields.MacTextField;

public class MacUIFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}
