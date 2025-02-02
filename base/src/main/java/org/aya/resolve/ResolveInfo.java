// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.concrete.desugar.AyaBinOpSet;
import org.aya.concrete.stmt.BindBlock;
import org.aya.concrete.stmt.Stmt;
import org.aya.core.def.PrimDef;
import org.aya.resolve.context.ModuleContext;
import org.aya.tyck.order.TyckOrder;
import org.aya.util.MutableGraph;
import org.aya.util.binop.OpDecl;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;

/**
 * @param opSet     binary operators
 * @param imports   modules imported using `import` command
 * @param reExports modules re-exported using `public open` command
 * @param depGraph  dependency graph of definitions. for each (v, successors) in the graph,
 *                  `successors` should be tycked first.
 */
@Debug.Renderer(text = "thisModule.moduleName().joinToString(\"::\")")
public record ResolveInfo(
  @NotNull ModuleContext thisModule,
  @NotNull ImmutableSeq<Stmt> program,
  @NotNull PrimDef.Factory primFactory,
  @NotNull AyaBinOpSet opSet,
  @NotNull MutableMap<ImmutableSeq<String>, ResolveInfo> imports,
  @NotNull MutableList<ImmutableSeq<String>> reExports,
  @NotNull MutableGraph<TyckOrder> depGraph,
  @NotNull MutableMap<OpDecl, BindBlock> bindBlockRename
) {
  public ResolveInfo(@NotNull PrimDef.Factory primFactory, @NotNull ModuleContext thisModule, @NotNull ImmutableSeq<Stmt> thisProgram, @NotNull AyaBinOpSet opSet) {
    this(thisModule, thisProgram, primFactory, opSet, MutableMap.create(), MutableList.create(), MutableGraph.create(), MutableMap.create());
  }
}
