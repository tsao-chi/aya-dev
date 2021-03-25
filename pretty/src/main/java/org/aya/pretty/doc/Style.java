// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
package org.aya.pretty.doc;

import org.glavo.kala.collection.mutable.Buffer;
import org.jetbrains.annotations.NotNull;

/**
 * Text styles. Inspired by terminal-colors.d(5)
 *
 * @author kiva
 */
public sealed interface Style {
  default Styles and() {
    return new Styles(this);
  }

  enum Attr implements Style {
    Italic,
    Bold,
    Strike,
    Underline,
  }

  final record ColorName(@NotNull String colorName, boolean background) implements Style {
  }

  final record ColorHex(int color, boolean background) implements Style {
  }

  /**
   * Make your custom style a subclass of this one. For example:
   * <pre>
   *   enum UnixTermStyle implements CustomStyle {
   *     DoubleUnderline,
   *     CurlyUnderline,
   *   }
   * </pre>
   */
  non-sealed interface CustomStyle extends Style {
  }

  class Styles {
    Buffer<Style> styles;

    Styles(Style style) {
      this.styles = Buffer.of(style);
    }

    public @NotNull Style.Styles italic() {
      styles.append(Attr.Italic);
      return this;
    }

    public @NotNull Style.Styles bold() {
      styles.append(Attr.Bold);
      return this;
    }

    public @NotNull Style.Styles strike() {
      styles.append(Attr.Strike);
      return this;
    }

    public @NotNull Style.Styles underline() {
      styles.append(Attr.Underline);
      return this;
    }

    public @NotNull Style.Styles color(@NotNull String colorName) {
      styles.append(new ColorName(colorName, false));
      return this;
    }

    public @NotNull Style.Styles colorBG(@NotNull String colorName) {
      styles.append(new ColorName(colorName, true));
      return this;
    }

    public @NotNull Style.Styles color(int color) {
      styles.append(Style.color(color));
      return this;
    }

    public @NotNull Style.Styles color(float r, float g, float b) {
      styles.append(Style.color(r, g, b));
      return this;
    }

    public @NotNull Style.Styles colorBG(int color) {
      styles.append(new ColorHex(color, true));
      return this;
    }

    public @NotNull Style.Styles custom(@NotNull CustomStyle style) {
      styles.append(style);
      return this;
    }
  }

  static @NotNull Style italic() {
    return Attr.Italic;
  }

  static @NotNull Style bold() {
    return Attr.Bold;
  }

  static @NotNull Style strike() {
    return Attr.Strike;
  }

  static @NotNull Style underline() {
    return Attr.Underline;
  }

  static @NotNull Style color(@NotNull String colorName) {
    return new ColorName(colorName, false);
  }

  static @NotNull Style colorBg(@NotNull String colorName) {
    return new ColorName(colorName, true);
  }

  static @NotNull Style color(int color) {
    return new ColorHex(color, false);
  }

  static @NotNull Style color(float r, float g, float b) {
    var red = (int) (r * 0xFF);
    var green = (int) (g * 0xFF);
    var blue = (int) (b * 0xFF);
    return new ColorHex(red << 16 | green << 8 | blue, false);
  }

  static @NotNull Style colorBg(int color) {
    return new ColorHex(color, true);
  }
}
