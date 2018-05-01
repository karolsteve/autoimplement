package com.steve.mvp.autoimplementprocessor.model;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * This class represent a member and current associated interface
 * Created by Steve Tchatchouang on 27/03/2018
 */

public class BindingImpl {
    //member
    private VariableElement field;
    //interface
    private TypeMirror      _interface;

    public BindingImpl(VariableElement field, TypeMirror _interface) {
        this.field = field;
        this._interface = _interface;
    }

    public VariableElement getField() {
        return field;
    }

    public TypeMirror getInterface() {
        return _interface;
    }

}
