/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.editor;

import com.facebook.litho.editor.instances.BoolEditorInstance;
import com.facebook.litho.editor.instances.NumberEditorInstance;
import com.facebook.litho.editor.instances.StringEditorInstance;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A repository of known Editor instances. As Editor is not aware of the Class it is defined for,
 * this registry is meant to enforce that relationship.
 *
 * <p>When it is first loaded it will register editors for basic Java types
 *
 * @see com.facebook.litho.editor.Editor
 */
public final class EditorRegistry {

  private EditorRegistry() {}

  private static final Map<Class<?>, Editor> EDITORS = new HashMap<>();

  private static @Nullable Editor getEditor(final Class<?> c) {
    Class<?> clazz = c;
    while (true) {
      if (EDITORS.containsKey(clazz)) {
        return EDITORS.get(clazz);
      }
      final Class<?> parent = clazz.getSuperclass();
      if (parent == null) {
        break;
      }
      clazz = parent;
    }

    for (Class ifaceClass : c.getInterfaces()) {
      if (EDITORS.containsKey(ifaceClass)) {
        return EDITORS.get(ifaceClass);
      }
    }

    return null;
  }

  public static void registerEditor(final Class<?> c, final Editor e) {
    EDITORS.put(c, e);
  }

  public static void registerEditors(final Map<Class<?>, Editor> e) {
    EDITORS.putAll(e);
  }

  /**
   * Reads an EditorValue for a field if there is an Editor defined for the Class parameter. Returns
   * null otherwise.
   */
  public static @Nullable EditorValue read(final Class<?> c, final Field f, final Object node) {
    final Editor editor = getEditor(c);
    if (editor == null) {
      return null;
    }
    return editor.read(f, node);
  }

  /**
   * Writes an EditorValue into a field if there is an Editor defined for the Class parameter.
   * Returns null otherwise.
   */
  public static @Nullable Boolean write(
      final Class<?> c, final Field f, final Object node, final EditorValue values) {
    final Editor editor = getEditor(c);
    if (editor == null) {
      return null;
    }
    return editor.write(f, node, values);
  }

  static {
    registerEditor(int.class, new NumberEditorInstance<>(int.class));
    registerEditor(float.class, new NumberEditorInstance<>(float.class));
    registerEditor(double.class, new NumberEditorInstance<>(double.class));
    registerEditor(long.class, new NumberEditorInstance<>(long.class));
    registerEditor(short.class, new NumberEditorInstance<>(short.class));
    registerEditor(byte.class, new NumberEditorInstance<>(byte.class));
    registerEditor(Integer.class, new NumberEditorInstance<>(Integer.class));
    registerEditor(Float.class, new NumberEditorInstance<>(Float.class));
    registerEditor(Double.class, new NumberEditorInstance<>(Double.class));
    registerEditor(Long.class, new NumberEditorInstance<>(Long.class));
    registerEditor(Short.class, new NumberEditorInstance<>(Short.class));
    registerEditor(Byte.class, new NumberEditorInstance<>(Byte.class));

    registerEditor(CharSequence.class, new StringEditorInstance());

    final BoolEditorInstance boolEditor = new BoolEditorInstance();
    registerEditor(Boolean.class, boolEditor);
    registerEditor(boolean.class, boolEditor);
  }
}
