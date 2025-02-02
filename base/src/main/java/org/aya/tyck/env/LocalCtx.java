// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.env;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableLinkedHashMap;
import kala.collection.mutable.MutableList;
import kala.tuple.Tuple2;
import kala.tuple.Unit;
import org.aya.core.Meta;
import org.aya.core.term.CallTerm;
import org.aya.core.term.IntroTerm;
import org.aya.core.term.Term;
import org.aya.core.visitor.VarConsumer;
import org.aya.generic.Constants;
import org.aya.generic.util.InternalException;
import org.aya.ref.LocalVar;
import org.aya.tyck.TyckState;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Debug.Renderer(hasChildren = "true", childrenArray = "extract().toArray()")
public sealed interface LocalCtx permits MapLocalCtx, SeqLocalCtx {
  @NotNull default Tuple2<CallTerm.Hole, Term> freshHole(@NotNull Term type, @NotNull SourcePos sourcePos) {
    return freshHole(type, Constants.ANONYMOUS_PREFIX, sourcePos);
  }
  default @NotNull Tuple2<CallTerm.Hole, Term>
  freshHole(@Nullable Term type, @NotNull String name, @NotNull SourcePos sourcePos) {
    var ctxTele = extract();
    var meta = Meta.from(ctxTele, name, type, sourcePos);
    var hole = new CallTerm.Hole(meta, 0, ctxTele.map(Term.Param::toArg), meta.telescope.map(Term.Param::toArg));
    return Tuple2.of(hole, IntroTerm.Lambda.make(meta.telescope, hole));
  }
  default <T> T with(@NotNull Term.Param param, @NotNull Supplier<T> action) {
    return with(param.ref(), param.type(), action);
  }
  void remove(@NotNull SeqView<LocalVar> vars);
  default void forward(@NotNull LocalCtx dest, @NotNull Term term, @NotNull TyckState state) {
    term.accept((VarConsumer<Unit>) (usage, o) -> {
      switch (usage) {
        case LocalVar localVar -> dest.put(localVar, get(localVar));
        case Meta meta -> {
          var sol = state.metas().getOrNull(meta);
          if (sol != null) forward(dest, sol, state);
        }
        case null, default -> {}
      }
    }, Unit.unit());
  }
  default <T> T with(@NotNull LocalVar var, @NotNull Term type, @NotNull Supplier<T> action) {
    put(var, type);
    try {
      return action.get();
    } finally {
      remove(SeqView.of(var));
    }
  }
  default @NotNull ImmutableSeq<Term.Param> extract() {
    var ctx = MutableList.<Term.Param>create();
    var map = this;
    while (map != null) {
      map.extractToLocal(ctx);
      map = map.parent();
    }
    return ctx.toImmutableSeq();
  }
  @Contract(mutates = "param1") void extractToLocal(@NotNull MutableList<Term.Param> dest);
  @Contract(pure = true) default @NotNull Term get(@NotNull LocalVar var) {
    var ctx = this;
    while (ctx != null) {
      var res = ctx.getLocal(var);
      if (res != null) return res;
      ctx = ctx.parent();
    }
    throw new InternalException(var.name());
  }

  @Contract(pure = true) @Nullable Term getLocal(@NotNull LocalVar var);
  default void put(@NotNull Term.Param param) {
    put(param.ref(), param.type());
  }
  void put(@NotNull LocalVar var, @NotNull Term term);
  boolean isEmpty();
  @Contract(" -> new") default @NotNull MapLocalCtx deriveMap() {
    return new MapLocalCtx(MutableLinkedHashMap.of(), this);
  }
  @Contract(" -> new") default @NotNull SeqLocalCtx deriveSeq() {
    return new SeqLocalCtx(MutableList.create(), this);
  }
  @Nullable LocalCtx parent();
}
