// Copyright (c) 2020-2020 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the Apache-2.0 license that can be found in the LICENSE file.
package org.mzi.tyck.order;

import asia.kala.collection.Seq;
import asia.kala.collection.mutable.Buffer;
import asia.kala.collection.mutable.MutableSet;
import org.jetbrains.annotations.Nullable;
import org.mzi.core.def.Def;
import org.mzi.core.visitor.RefFinder;

/**
 * Generate the order of dependency of headers, fail if a cycle occurs.
 * @author re-xyr
 */
public final class HeaderOrder {
  private static void visit(Def def, MutableSet<Def> visited, MutableSet<Def> inStack, Buffer<Def> order) {
    if (inStack.contains(def)) throw new IllegalStateException("Circular reference in definition header."); // TODO[xyr]: report instead of throw
    visited.add(def);
    inStack.add(def);
    order.append(def);
    var references = Buffer.<Def>of();
    def.accept(RefFinder.HEADER_ONLY, references);
    for (var nextDef : references) {
      if (visited.contains(nextDef)) continue;
      visit(nextDef, visited, inStack, order);
    }
    inStack.remove(def);
  }

  public static @Nullable Buffer<Def> genHeaderOrder(Seq<Def> defs) {
    var visited = MutableSet.<Def>of();
    var inStack = MutableSet.<Def>of();
    var order = Buffer.<Def>of();
    for (var def : defs) {
      if (visited.contains(def)) continue;
      visit(def, visited, inStack, order);
    }
    return order;
  }
}
