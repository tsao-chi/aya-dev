// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level definitions.
 *
 * @author ice1000
 */
public sealed abstract class TopLevelDef implements Def, Def.DefWithTelescope permits UserDef, PrimDef {
  public final @NotNull ImmutableSeq<Term.Param> telescope;
  public final @NotNull Term result;

  protected TopLevelDef(
    @NotNull ImmutableSeq<Term.Param> telescope,
    @NotNull Term result
  ) {
    this.telescope = telescope;
    this.result = result;
  }

  @Override public @NotNull ImmutableSeq<Term.Param> telescope() {
    return telescope;
  }

  @Override public @NotNull Term result() {
    return result;
  }
}
