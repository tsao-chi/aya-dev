// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import kala.tuple.Tuple;
import org.aya.api.error.Reporter;
import org.aya.concrete.Expr;
import org.aya.concrete.stmt.Decl;
import org.aya.concrete.stmt.Signatured;
import org.aya.core.Matching;
import org.aya.core.def.*;
import org.aya.core.pat.Pat;
import org.aya.core.sort.LevelSubst;
import org.aya.core.sort.Sort;
import org.aya.core.term.CallTerm;
import org.aya.core.term.FormTerm;
import org.aya.core.term.Term;
import org.aya.core.visitor.Substituter;
import org.aya.generic.Level;
import org.aya.generic.Modifier;
import org.aya.tyck.error.PrimProblem;
import org.aya.tyck.pat.Conquer;
import org.aya.tyck.pat.PatClassifier;
import org.aya.tyck.pat.PatTycker;
import org.aya.tyck.trace.Trace;
import org.aya.util.TreeBuilder;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author ice1000, kiva
 * @apiNote this class does not create {@link ExprTycker} instances itself,
 * but use the one passed to it. {@link StmtTycker#newTycker()} creates instances
 * of expr tyckers.
 */
public record StmtTycker(
  @NotNull Reporter reporter,
  Trace.@Nullable Builder traceBuilder
) {
  public @NotNull ExprTycker newTycker() {
    return new ExprTycker(reporter, traceBuilder);
  }

  private void tracing(@NotNull Consumer<Trace.@NotNull Builder> consumer) {
    if (traceBuilder != null) consumer.accept(traceBuilder);
  }

  private <S extends Signatured, D extends Def> D
  traced(@NotNull S yeah, ExprTycker p, @NotNull BiFunction<S, ExprTycker, D> f) {
    tracing(builder -> builder.shift(new Trace.DeclT(yeah.ref(), yeah.sourcePos)));
    var parent = p.localCtx;
    p.localCtx = parent.derive();
    var r = f.apply(yeah, p);
    tracing(Trace.Builder::reduce);
    p.localCtx = parent;
    return r;
  }

  public @NotNull Def tyck(@NotNull Decl decl, @NotNull ExprTycker tycker) {
    return traced(decl, tycker, this::doTyck);
  }

  private @NotNull Def doTyck(@NotNull Decl predecl, @NotNull ExprTycker tycker) {
    if (predecl.signature == null) tyckHeader(predecl, tycker);
    else predecl.signature.param().forEach(param -> tycker.localCtx.put(param.ref(), param.type()));
    var signature = predecl.signature;
    return switch (predecl) {
      case Decl.FnDecl decl -> {
        assert signature != null;
        var factory = FnDef.factory((resultTy, body) ->
          new FnDef(decl.ref, signature.param(), signature.sortParam(), resultTy, decl.modifiers, body));
        yield decl.body.fold(
          body -> {
            var nobody = tycker.inherit(body, signature.result()).wellTyped();
            tycker.solveMetas();
            var zonker = tycker.newZonker();
            // It may contain unsolved metas. See `checkTele`.
            var resultTy = zonker.zonk(signature.result(), decl.result.sourcePos());
            return factory.apply(resultTy, Either.left(zonker.zonk(nobody, body.sourcePos())));
          },
          clauses -> {
            var patTycker = new PatTycker(tycker);
            var result = patTycker.elabClauses(clauses, signature, decl.result.sourcePos());
            var matchings = result._2.flatMap(Pat.PrototypeClause::deprototypify);
            var def = factory.apply(result._1, Either.right(matchings));
            if (patTycker.noError()) {
              var orderIndependent = decl.modifiers.contains(Modifier.Overlap);
              ensureConfluent(tycker, signature, result._2, matchings, decl.sourcePos, true, orderIndependent);
            }
            return def;
          }
        );
      }
      case Decl.DataDecl decl -> {
        assert signature != null;
        var result = tycker.sort(decl.result, signature.result());
        var body = decl.body.map(clause -> traced(clause, tycker, (ctor, t) -> visitCtor(ctor, t, result)));
        yield new DataDef(decl.ref, signature.param(), signature.sortParam(), result, body);
      }
      case Decl.PrimDecl decl -> decl.ref.core;
      case Decl.StructDecl decl -> {
        assert signature != null;
        var result = tycker.sort(decl.result, signature.result());
        yield new StructDef(decl.ref, signature.param(), signature.sortParam(), result, decl.fields.map(field ->
          traced(field, tycker, (f, tyck) -> visitField(f, tyck, result))));
      }
    };
  }

  public void tyckHeader(@NotNull Decl decl, @NotNull ExprTycker tycker) {
    switch (decl) {
      case Decl.FnDecl fn -> {
        tracing(builder -> builder.shift(new Trace.LabelT(fn.sourcePos, "telescope")));
        var resultTele = checkTele(tycker, fn.telescope, FormTerm.freshSort(fn.sourcePos));
        // It might contain unsolved holes, but that's acceptable.
        var resultRes = tycker.synthesize(fn.result).wellTyped();
        tracing(TreeBuilder::reduce);
        fn.signature = new Def.Signature(tycker.extractLevels(), resultTele, resultRes);
      }
      case Decl.DataDecl data -> {
        var pos = data.sourcePos;
        var tele = checkTele(tycker, data.telescope, FormTerm.freshSort(pos));
        var result = data.result instanceof Expr.HoleExpr ? FormTerm.Univ.ZERO
          // ^ probably omitted
          : tycker.zonk(data.result, tycker.inherit(data.result, FormTerm.freshUniv(pos))).wellTyped();
        data.signature = new Def.Signature(tycker.extractLevels(), tele, result);
      }
      case Decl.StructDecl struct -> {
        var pos = struct.sourcePos;
        var tele = checkTele(tycker, struct.telescope, FormTerm.freshSort(pos));
        var result = tycker.zonk(struct.result, tycker.inherit(struct.result, FormTerm.freshUniv(pos))).wellTyped();
        // var levelSubst = tycker.equations.solve();
        struct.signature = new Def.Signature(tycker.extractLevels(), tele, result);
      }
      case Decl.PrimDecl prim -> {
        assert tycker.localCtx.isEmpty();
        var core = prim.ref.core;
        var tele = checkTele(tycker, prim.telescope, FormTerm.freshSort(prim.sourcePos));
        if (tele.isNotEmpty()) {
          // ErrorExpr on prim.result means the result type is unspecified.
          if (prim.result instanceof Expr.ErrorExpr) {
            reporter.report(new PrimProblem.NoResultTypeError(prim));
            return;
          }
          var result = tycker.synthesize(prim.result).wellTyped();
          var levelSubst = new LevelSubst.Simple(MutableMap.create());
          // Homotopy level goes first
          var levels = tycker.extractLevels();
          for (var lvl : core.levels.zip(levels))
            levelSubst.solution().put(lvl._1, new Sort(new Level.Reference<>(lvl._2)));
          var target = FormTerm.Pi.make(core.telescope(), core.result())
            .subst(Substituter.TermSubst.EMPTY, levelSubst);
          tycker.unifyTyReported(FormTerm.Pi.make(tele, result), target, prim.result);
          prim.signature = new Def.Signature(levels, tele, result);
        } else if (!(prim.result instanceof Expr.ErrorExpr)) {
          var result = tycker.synthesize(prim.result).wellTyped();
          tycker.unifyTyReported(result, core.result(), prim.result);
        } else prim.signature = new Def.Signature(ImmutableSeq.empty(), core.telescope(), core.result());
        tycker.solveMetas();
      }
    }
  }

  private @NotNull CtorDef visitCtor(Decl.@NotNull DataCtor ctor, ExprTycker tycker, Sort dataSort) {
    var dataRef = ctor.dataRef;
    var dataSig = dataRef.concrete.signature;
    assert dataSig != null;
    var dataArgs = dataSig.param().map(Term.Param::toArg);
    var sortParam = dataSig.sortParam();
    var dataCall = new CallTerm.Data(dataRef, sortParam.view()
      .map(Level.Reference::new)
      .map(Sort::new)
      .toImmutableSeq(), dataArgs);
    var sig = new Def.Signature(sortParam, dataSig.param(), dataCall);
    var patTycker = new PatTycker(tycker);
    // There might be patterns in the constructor
    var pat = ctor.patterns.isNotEmpty()
      ? patTycker.visitPatterns(sig, ctor.patterns.view())._1
      // No patterns, leave it blank
      : ImmutableSeq.<Pat>empty();
    var tele = checkTele(tycker, ctor.telescope, dataSort);
    var signature = new Def.Signature(sortParam, tele, dataCall);
    ctor.signature = signature;
    var dataTeleView = dataSig.param().view();
    if (pat.isNotEmpty()) {
      dataCall = (CallTerm.Data) dataCall.subst(ImmutableMap.from(
        dataTeleView.map(Term.Param::ref).zip(pat.view().map(Pat::toTerm))));
    }
    ctor.patternTele = pat.isEmpty() ? dataTeleView.map(Term.Param::implicitify).toImmutableSeq() : Pat.extractTele(pat);
    var elabClauses = patTycker.elabClauses(ctor.clauses, signature, ctor.sourcePos)._2;
    var matchings = elabClauses.flatMap(Pat.PrototypeClause::deprototypify);
    var elaborated = new CtorDef(dataRef, ctor.ref, pat, ctor.patternTele, tele, matchings, dataCall, ctor.coerce);
    if (patTycker.noError())
      ensureConfluent(tycker, signature, elabClauses, matchings, ctor.sourcePos, false, true);
    return elaborated;
  }

  private void ensureConfluent(
    ExprTycker tycker, Def.Signature signature, ImmutableSeq<Pat.PrototypeClause> elabClauses,
    ImmutableSeq<@NotNull Matching> matchings, @NotNull SourcePos pos,
    boolean coverage, boolean orderIndependent
  ) {
    if (!matchings.isNotEmpty()) return;
    tracing(builder -> builder.shift(new Trace.LabelT(pos, "confluence check")));
    var classification = PatClassifier.classify(elabClauses, signature.param(),
      tycker.state, tycker.reporter, pos, coverage);
    if (orderIndependent) PatClassifier.confluence(elabClauses, tycker, pos, signature.result(), classification);
    else if (classification.isNotEmpty())
      PatClassifier.firstMatchDomination(elabClauses, reporter, pos, classification);
    Conquer.against(matchings, orderIndependent, tycker, pos, signature);
    tycker.solveMetas();
    tracing(TreeBuilder::reduce);
  }

  private @NotNull FieldDef visitField(Decl.@NotNull StructField field, ExprTycker tycker, @NotNull Sort structSort) {
    var tele = checkTele(tycker, field.telescope, structSort);
    var structRef = field.structRef;
    var result = tycker.zonk(field.result, tycker.inherit(field.result, new FormTerm.Univ(structSort))).wellTyped();
    var structSig = structRef.concrete.signature;
    assert structSig != null;
    field.signature = new Def.Signature(structSig.sortParam(), tele, result);
    var patTycker = new PatTycker(tycker);
    var elabClauses = patTycker.elabClauses(field.clauses, field.signature, field.result.sourcePos())._2;
    var matchings = elabClauses.flatMap(Pat.PrototypeClause::deprototypify);
    var body = field.body.map(e -> tycker.inherit(e, result).wellTyped());
    var elaborated = new FieldDef(structRef, field.ref, structSig.param(), tele, result, matchings, body, field.coerce);
    if (patTycker.noError())
      ensureConfluent(tycker, field.signature, elabClauses, matchings, field.sourcePos, false, true);
    return elaborated;
  }

  private @NotNull ImmutableSeq<Term.Param>
  checkTele(@NotNull ExprTycker exprTycker, @NotNull ImmutableSeq<Expr.Param> tele, Sort sort) {
    var okTele = tele.map(param -> {
      assert param.type() != null; // guaranteed by AyaProducer
      var paramTyped = exprTycker.inherit(param.type(), new FormTerm.Univ(sort)).wellTyped();
      exprTycker.localCtx.put(param.ref(), paramTyped);
      return Tuple.of(new Term.Param(param, paramTyped), param.sourcePos());
    });
    exprTycker.solveMetas();
    var zonker = exprTycker.newZonker();
    return okTele.map(tt -> {
      var t = tt._1;
      var term = zonker.zonk(t.type(), tt._2);
      exprTycker.localCtx.put(t.ref(), term);
      return new Term.Param(t, term);
    });
  }
}
