// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.lsp.actions;

import kala.collection.SeqView;
import org.aya.cli.library.source.LibraryOwner;
import org.aya.cli.library.source.LibrarySource;
import org.aya.lsp.utils.Log;
import org.aya.lsp.utils.LspRange;
import org.aya.lsp.utils.ModuleVar;
import org.aya.lsp.utils.Resolver;
import org.aya.ref.DefVar;
import org.aya.ref.LocalVar;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ice1000, kiva
 */
public interface GotoDefinition {
  static @NotNull List<LocationLink> invoke(
    @NotNull LibrarySource source,
    @NotNull Position position,
    @NotNull SeqView<LibraryOwner> libraries
  ) {
    return findDefs(source, position, libraries).mapNotNull(pos -> {
      var from = pos.sourcePos();
      var to = pos.data();
      var res = LspRange.toLoc(from, to);
      if (res != null) Log.d("Resolved: %s in %s", to, res.getTargetUri());
      return res;
    }).collect(Collectors.toList());
  }

  static @NotNull SeqView<WithPos<SourcePos>> findDefs(
    @NotNull LibrarySource source,
    @NotNull Position position,
    @NotNull SeqView<LibraryOwner> libraries
  ) {
    return Resolver.resolveVar(source, position).mapNotNull(pos -> {
      var from = pos.sourcePos();
      var target = switch (pos.data()) {
        case DefVar<?, ?> defVar -> defVar.concrete.sourcePos();
        case LocalVar localVar -> localVar.definition();
        case ModuleVar moduleVar -> mockSourcePos(libraries, moduleVar);
        case default -> null;
      };
      if (target == null) return null;
      return new WithPos<>(from, target);
    });
  }

  private static @Nullable SourcePos mockSourcePos(@NotNull SeqView<LibraryOwner> libraries, @NotNull ModuleVar moduleVar) {
    return Resolver.resolveModule(libraries, moduleVar.path().ids())
      .map(src -> src.toSourceFile(""))
      .map(src -> new SourcePos(src, 0, 0, 1, 0, 1, 0))
      .getOrNull();
  }
}
