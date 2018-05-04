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

package com.steve.mvp.autoimplementprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.steve.mvp.autoimplement.internal.AutoImplement;
import com.steve.mvp.autoimplement.internal.ImplementationOf;
import com.steve.mvp.autoimplementprocessor.model.BindingImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by Steve Tchatchouang
 */
public class AutoImplementProcessor extends AbstractProcessor {
    private static final String SUFFIX = "_Impl";

    private Messager messager;
    private Filer    filer;
    private Types    typesUtils;
    private Elements elementsUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.messager = processingEnvironment.getMessager();
        this.filer = processingEnvironment.getFiler();
        this.typesUtils = processingEnvironment.getTypeUtils();
        this.elementsUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(AutoImplement.class);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS || !element.getModifiers().contains(Modifier.ABSTRACT)) {
                messager.printMessage(Diagnostic.Kind.ERROR, element.toString() + " isn't an abstract class");
                return false;
            }
            processElement((TypeElement) element);
        }
        return true;
    }

    private void processElement(TypeElement element) {
        //Get all member and interfaces
        List<BindingImpl> implMap = getImplMap(element);
        //Get all method associated with #implementationOf interface
        Map<BindingImpl, List<ExecutableElement>> executableMap = getBindingListMap(implMap);
        //Get all constructors
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(element.getEnclosedElements());
        //check for private constructor
        for (ExecutableElement cons : constructors) {
            if (!cons.getModifiers().contains(Modifier.PUBLIC)) {
                messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "non public constructor can generate illegalAccessException if using AutoImplUtils"
                );
            }
        }

        //Generating methods
        List<MethodSpec> methodSpecList = new LinkedList<>();

        executableMap.forEach((binding, executableElements) -> {
            for (ExecutableElement executableElement : executableElements) {
                methodSpecList.add(buildMethod(binding.getField(), executableElement));
            }
        });

        //Class name (and package)
        ClassName impl = ClassName.get(
                elementsUtils.getPackageOf(element).getQualifiedName().toString(),
                element.getSimpleName() + SUFFIX
        );

        //Generate type (class)
        TypeSpec typeSpec = TypeSpec.classBuilder(impl)
                .addMethods(methodSpecList)
                .addJavadoc("Generated implementation of "+element.getSimpleName()+"\n")
                .addJavadoc("@author : Steve Tchatchouang\nEmail : steve.tchatchouang@gmail.com\n")
                .addMethods(buildConstructors(constructors))
                .superclass(TypeName.get(element.asType()))
                .addModifiers(Modifier.PUBLIC)
                .build();

        //Generate file
        JavaFile file = JavaFile.builder(impl.packageName(), typeSpec).build();
        try {
            file.writeTo(filer);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    private List<MethodSpec> buildConstructors(List<ExecutableElement> constructors) {
        List<MethodSpec> consSpec = new LinkedList<>();
        for (ExecutableElement cons : constructors) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            List<ParameterSpec> result = new ArrayList<>();
            for (VariableElement parameter : cons.getParameters()) {
                result.add(ParameterSpec.get(parameter));
            }
            builder.addParameters(result);
            StringBuilder params = new StringBuilder();
            boolean hasComma = false;
            for (VariableElement variableElement : cons.getParameters()) {
                params.append(variableElement.getSimpleName()).append(",");
                hasComma = true;
            }
            String paramsMethod = "";
            if (hasComma) {
                paramsMethod = params.substring(0, params.length() - 1);
            }
            builder.addStatement("super(" + paramsMethod + ")");
            consSpec.add(builder.build());
        }
        return consSpec;
    }

    private MethodSpec buildMethod(VariableElement field, ExecutableElement methodElement) {
        String methodName = methodElement.getSimpleName().toString();
        StringBuilder params = new StringBuilder();
        boolean hasComma = false;
        for (VariableElement variableElement : methodElement.getParameters()) {
            params.append(variableElement.getSimpleName()).append(",");
            hasComma = true;
        }
        MethodSpec.Builder builder = MethodSpec.overriding(methodElement);
        String paramsMethod = "";
        if (hasComma) {
            paramsMethod = params.substring(0, params.length() - 1);
        }
        if (methodElement.getReturnType().getKind() != TypeKind.VOID) {
            builder.addStatement("return " + field.getSimpleName().toString() + "." + methodName + "(" + paramsMethod + ")");
        } else {
            builder.addStatement(field.getSimpleName().toString() + "." + methodName + "(" + paramsMethod + ")");
        }
        return builder.build();
    }

    /**
     * @param implMap : class members
     * @return map of methods associated with members interfaces(implementation)
     */
    private Map<BindingImpl, List<ExecutableElement>> getBindingListMap(List<BindingImpl> implMap) {
        Map<BindingImpl, List<ExecutableElement>> executableMap = new LinkedHashMap<>();
        for (BindingImpl binding : implMap) {
            if (!executableMap.containsKey(binding)) {
                executableMap.put(binding, new LinkedList<>());
            }
            TypeElement bindingInterface = (TypeElement) typesUtils.asElement(binding.getInterface());
            //Get all interface method and add them to map
            for (ExecutableElement e : ElementFilter.methodsIn(bindingInterface.getEnclosedElements())) {
                executableMap.get(binding).add(e);
            }
            //browse all interfaces of interface and add all methods
            for (TypeMirror typeMirror : getAllInterfaces(bindingInterface)) {
                for (ExecutableElement e : ElementFilter.methodsIn(typesUtils.asElement(typeMirror).getEnclosedElements())) {
                    executableMap.get(binding).add(e);
                }
            }
        }
        return executableMap;
    }

    private List<BindingImpl> getImplMap(TypeElement te) {
        List<BindingImpl> implMap = new LinkedList<>();
        for (VariableElement e : ElementFilter.fieldsIn(te.getEnclosedElements())) {
            ImplementationOf implementationOf = e.getAnnotation(ImplementationOf.class);
            if (implementationOf != null) {
                TypeMirror typeMirror = null;
                try {
                    implementationOf.value();
                } catch (MirroredTypeException ex) {
                    typeMirror = ex.getTypeMirror();
                }
                implMap.add(new BindingImpl(e, typeMirror));
            }
        }
        return implMap;
    }

    private Set<? extends TypeMirror> getAllInterfaces(TypeElement te) {
        Set<TypeMirror> interfaces = new HashSet<>(te.getInterfaces());
        for (TypeMirror tm : te.getInterfaces()) {
            Set<TypeMirror> set = new LinkedHashSet<>();
            interfaces.addAll(listInterfaces(set, tm));
        }
        return interfaces;
    }

    private Collection<? extends TypeMirror> listInterfaces(Set<TypeMirror> set, TypeMirror tm) {
        TypeElement te = (TypeElement) typesUtils.asElement(tm);
        if (te.getInterfaces().size() == 0) {
            return Collections.singleton(tm);
        } else {
            for (TypeMirror typeMirror : te.getInterfaces()) {
                TypeElement typeElement = (TypeElement) typesUtils.asElement(typeMirror);
                Set<TypeMirror> childNode;
                if (typeElement.getInterfaces().size() == 0) {
                    childNode = Collections.singleton(typeMirror);
                } else {
                    childNode = new LinkedHashSet<>();
                    childNode.add(typeMirror);
                    set.addAll(listInterfaces(childNode, typeMirror));
                }
                set.addAll(childNode);
            }
        }
        return set;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(AutoImplement.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
