// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.concrete.visitor;

import kala.tuple.Unit;
import org.aya.concrete.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author kiva
 */
public interface StmtConsumer<P> extends Stmt.Visitor<P, Unit>, ExprConsumer<P>, Signatured.Visitor<P, Unit>, Decl.Visitor<P, Unit>, Pattern.Visitor<P, Unit> {
  default void visitSignatured(@NotNull Signatured signatured, P pp) {
    signatured.telescope.forEach(p -> {
      var type = p.type();
      if (type != null) type.accept(this, pp);
    });
  }

  default void visitDecl(@NotNull Decl decl, P pp) {
    visitSignatured(decl, pp);
    decl.abuseBlock.forEach(stmt -> stmt.accept(this, pp));
  }

  default void visitClause(@NotNull Pattern.Clause c, P pp) {
    c.patterns.forEach(pattern -> pattern.accept(this, pp));
    c.expr.forEach(expr -> expr.accept(this, pp));
  }

  @Override default Unit visitData(@NotNull Decl.DataDecl decl, P p) {
    visitDecl(decl, p);
    decl.result.accept(this, p);
    decl.body.forEach(ctor -> ctor.accept(this, p));
    return Unit.unit();
  }

  @Override default Unit visitStruct(@NotNull Decl.StructDecl decl, P p) {
    visitDecl(decl, p);
    decl.result.accept(this, p);
    decl.fields.forEach(f -> f.accept(this, p));
    return Unit.unit();
  }

  @Override default Unit visitFn(@NotNull Decl.FnDecl decl, P p) {
    visitDecl(decl, p);
    decl.result.accept(this, p);
    decl.body.forEach(
      expr -> expr.accept(this, p),
      clauses -> clauses.forEach(clause -> visitClause(clause, p))
    );
    return Unit.unit();
  }

  @Override default Unit visitPrim(@NotNull Decl.PrimDecl decl, P p) {
    visitDecl(decl, p);
    if (decl.result != null) decl.result.accept(this, p);
    return Unit.unit();
  }

  @Override default Unit visitImport(Stmt.@NotNull ImportStmt cmd, P p) {
    return Unit.unit();
  }

  @Override default Unit visitOpen(Stmt.@NotNull OpenStmt cmd, P p) {
    return Unit.unit();
  }

  @Override default Unit visitModule(Stmt.@NotNull ModuleStmt mod, P p) {
    mod.contents().forEach(stmt -> stmt.accept(this, p));
    return Unit.unit();
  }

  @Override default Unit visitCtor(Decl.@NotNull DataCtor ctor, P p) {
    visitSignatured(ctor, p);
    ctor.patterns.forEach(pattern -> pattern.accept(this, p));
    ctor.clauses.forEach(clause -> visitClause(clause, p));
    return Unit.unit();
  }

  @Override default Unit visitField(Decl.@NotNull StructField field, P p) {
    visitSignatured(field, p);
    field.result.accept(this, p);
    field.clauses.forEach(clause -> visitClause(clause, p));
    field.body.forEach(expr -> expr.accept(this, p));
    return Unit.unit();
  }

  @Override default Unit visitBind(Stmt.@NotNull BindStmt bind, P p) {
    return Unit.unit();
  }

  @Override default Unit visitLevels(Generalize.@NotNull Levels levels, P p) {
    return Unit.unit();
  }

  @Override default Unit visitTuple(Pattern.@NotNull Tuple tuple, P p) {
    tuple.patterns().forEach(pattern -> pattern.accept(this, p));
    return Unit.unit();
  }

  @Override default Unit visitNumber(Pattern.@NotNull Number number, P p) {
    return Unit.unit();
  }

  @Override default Unit visitAbsurd(Pattern.@NotNull Absurd absurd, P p) {
    return Unit.unit();
  }

  @Override default Unit visitCalmFace(Pattern.@NotNull CalmFace calmFace, P p) {
    return Unit.unit();
  }

  @Override default Unit visitBind(Pattern.@NotNull Bind bind, P p) {
    return Unit.unit();
  }

  @Override default Unit visitCtor(Pattern.@NotNull Ctor ctor, P p) {
    ctor.params().forEach(pattern -> pattern.accept(this, p));
    return Unit.unit();
  }

  @Override default void traceExit(P p, Unit unit) {
  }

  @Override default Unit visitExample(Sample.@NotNull Working example, P p) {
    return example.delegate.accept(this, p);
  }
  @Override default Unit visitCounterexample(Sample.@NotNull Counter example, P p) {
    return example.delegate.accept(this, p);
  }
}
