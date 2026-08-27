package com.rcpky.abstractfactory.uilibrary.window;

import com.rcpky.abstractfactory.uilibrary.UIFactory;
import com.rcpky.abstractfactory.uilibrary.features.Button;
import com.rcpky.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.abstractfactory.uilibrary.features.TextField;
import com.rcpky.abstractfactory.uilibrary.features.buttons.WindowButton;
import com.rcpky.abstractfactory.uilibrary.features.checkboxes.WindowCheckbox;
import com.rcpky.abstractfactory.uilibrary.features.textfields.WindowTextField;

public class WindowUIFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new WindowButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowCheckbox();
    }

    @Override
    public TextField createTextField() {
        return new WindowTextField();
    }
}
