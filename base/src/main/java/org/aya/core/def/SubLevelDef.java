// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.def;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.core.Matching;
import org.aya.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Fields or constructors, in contrast to {@link TopLevelDef}.
 *
 * @author ice1000
 */
public sealed abstract class SubLevelDef implements Def permits CtorDef, FieldDef {
  public final Term.Param @NotNull [] ownerTele;
  public final Term.Param @NotNull [] selfTele;
  public final @NotNull Term result;
  public final @NotNull ImmutableSeq<Matching> clauses;
  public final boolean coerce;

  protected SubLevelDef(
    Term.Param @NotNull [] ownerTele, Term.Param @NotNull [] selfTele,
    @NotNull Term result, @NotNull ImmutableSeq<Matching> clauses, boolean coerce
  ) {
    this.ownerTele = ownerTele;
    this.selfTele = selfTele;
    this.result = result;
    this.clauses = clauses;
    this.coerce = coerce;
  }

  public @NotNull SeqView<Term.Param> fullTelescope() {
    return ownerTele.view().concat(selfTele);
  }

  @Override public @NotNull Term result() {
    return result;
  }
}
