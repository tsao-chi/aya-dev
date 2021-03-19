// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.test;

import org.aya.api.error.CollectReporter;
import org.aya.api.error.SourcePos;
import org.aya.api.ref.Var;
import org.aya.core.term.Term;
import org.aya.ref.LocalVar;
import org.aya.tyck.LocalCtx;
import org.aya.tyck.MetaContext;
import org.aya.tyck.unify.PatDefEq;
import org.aya.tyck.unify.TypedDefEq;
import org.aya.util.Ordering;
import org.glavo.kala.collection.mutable.MutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LispTestCase {
  protected final MutableMap<String, @NotNull Var> vars = MutableMap.create();
  protected final CollectReporter reporter = new CollectReporter();

  protected @NotNull TypedDefEq eq(MutableMap<LocalVar, Term> localCtx) {
    return new TypedDefEq(eq -> new PatDefEq(eq, Ordering.Eq, new MetaContext(reporter)), new LocalCtx(localCtx, null), SourcePos.NONE);
  }

  @AfterEach
  public void clearVars() {
    vars.clear();
  }

  @AfterEach
  public void assertNoErrors() {
    assertTrue(reporter.errors().isEmpty());
  }
}
