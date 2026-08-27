package com.rcpky.abstractfactory.uilibrary;

import com.rcpky.abstractfactory.uilibrary.features.Button;
import com.rcpky.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.abstractfactory.uilibrary.features.TextField;

public interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField();
}
