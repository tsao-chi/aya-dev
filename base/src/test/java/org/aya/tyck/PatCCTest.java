// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.api.error.Reporter;
import org.aya.api.error.SourcePos;
import org.aya.core.Matching;
import org.aya.core.def.FnDef;
import org.aya.core.def.PrimDef;
import org.aya.core.pat.Pat;
import org.aya.test.ThrowingReporter;
import org.aya.tyck.pat.PatClassifier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CC = coverage and confluence
 */
public class PatCCTest {
  public static @NotNull ImmutableSeq<PatClassifier.PatClass> testClassify(
    @NotNull ImmutableSeq<Matching> clauses,
    @NotNull Reporter reporter, @NotNull SourcePos pos,
    PrimDef.PrimFactory primFactory
  ) {
    return PatClassifier.classify(clauses.map(Pat.PrototypeClause::prototypify), reporter, pos, primFactory, true);
  }

  @Test public void addCC() {
    var primFactory = PrimDef.PrimFactory.create();
    var decls = TyckDeclTest.successTyckDecls("""
      open data Nat : Set | zero | suc Nat
      def add (a b : Nat) : Nat
       | zero, b => b
       | a, zero => a
       | suc a, b => suc (add a b)
       | a, suc b => suc (add a b)""", primFactory);
      var clauses = ((FnDef) decls.get(1)).body.getRightValue();
    var classified = testClassify(clauses, ThrowingReporter.INSTANCE, SourcePos.NONE, primFactory);
    assertEquals(4, classified.size());
    classified.forEach(cls ->
      assertEquals(2, cls.contents().size()));
  }

  @Test public void maxCC() {
    var primFactory = PrimDef.PrimFactory.create();
    var decls = TyckDeclTest.successTyckDecls("""
      open data Nat : Set | zero | suc Nat
      def max (a b : Nat) : Nat
       | zero, b => b
       | a, zero => a
       | suc a, suc b => suc (max a b)""", primFactory);
      var clauses = ((FnDef) decls.get(1)).body.getRightValue();
    var classified = testClassify(clauses, ThrowingReporter.INSTANCE, SourcePos.NONE, primFactory);
    assertEquals(4, classified.size());
    assertEquals(3, classified.filter(patClass -> patClass.contents().sizeEquals(1)).size());
    assertEquals(1, classified.filter(patClass -> patClass.contents().sizeEquals(2)).size());
  }

  @Test public void tupleCC() {
    var primFactory = PrimDef.PrimFactory.create();
    var decls = TyckDeclTest.successTyckDecls("""
      open data Nat : Set | zero | suc Nat
      open data Unit : Set | unit Nat
      def max (a : Sig Nat ** Nat) (b : Unit) : Nat
       | (zero, b), unit x => b
       | (a, zero), y => a
       | (suc a, suc b), unit y => suc (max (a, b) (unit zero))""", primFactory);
      var clauses = ((FnDef) decls.get(2)).body.getRightValue();
    var classified = testClassify(clauses, ThrowingReporter.INSTANCE, SourcePos.NONE, primFactory);
    assertEquals(4, classified.size());
    assertEquals(3, classified.filter(patClass -> patClass.contents().sizeEquals(1)).size());
    assertEquals(1, classified.filter(patClass -> patClass.contents().sizeEquals(2)).size());
  }
}
