//
// MessagePack for Java
//
// Copyright (C) 2009 - 2013 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.template.builder;

import org.msgpack.template.FieldList;
import org.msgpack.template.Template;

import java.lang.reflect.Type;

public interface TemplateBuilder {

    boolean matchType(Type targetType, boolean forceBuild);

    <T> Template<T> buildTemplate(Type targetType)
            throws TemplateBuildException;

    <T> Template<T> buildTemplate(Class<T> targetClass, FieldList flist)
            throws TemplateBuildException;

    void writeTemplate(Type targetType, String directoryName);

    <T> Template<T> loadTemplate(Type targetType);
}
