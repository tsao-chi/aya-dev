// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.core.cubical;

import kala.collection.immutable.ImmutableSeq;
import org.aya.core.term.Term;
import org.aya.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public record Boundaries(
  @NotNull ImmutableSeq<LocalVar> dimVars,
  @NotNull ImmutableSeq<Clause> clauses
) {
  public record Clause(@NotNull ImmutableSeq<DimPat> patterns, @NotNull Term body) {
  }

  public sealed interface DimPat {
  }

  public record BoundDim(boolean isLeft) implements DimPat {
  }

  public static final class UnmatchedDim implements DimPat {
    public static final @NotNull UnmatchedDim INSTANCE = new UnmatchedDim();

    private UnmatchedDim() {}
  }
}
