/*
 * Copyright 2018 Steve Tchatchouang
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
