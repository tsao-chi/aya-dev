// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.def;

import org.aya.core.sort.Sort;
import org.aya.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level definitions.
 *
 * @author ice1000
 */
public sealed abstract class TopLevelDef implements Def permits UserDef, PrimDef {
  public final Term.Param @NotNull [] telescope;
  public final @NotNull Term result;
  public final @NotNull Sort.LvlVar @NotNull [] levels;

  protected TopLevelDef(
    Term.Param @NotNull [] telescope,
    @NotNull Term result, @NotNull Sort.LvlVar @NotNull [] levels
  ) {
    this.telescope = telescope;
    this.result = result;
    this.levels = levels;
  }

  @Override public Term.Param @NotNull [] telescope() {
    return telescope;
  }

  @Override public @NotNull Term result() {
    return result;
  }
}
