/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln.render;

public class NbExpected {

  // a comment
  public static final int constant = 3;

  public static void main2(String[] args) {
    System.out.println("a field comment");
    System.out.println("public static final int constant = 3;");
    System.out.println("main:");
    System.out.println("some doc");
    System.out.println("int x = 3;");
    int x = 3;
    System.out.println(x);

    System.out.println("other doc");
    System.out.println("int y = 7;");
    int y = 7;
    System.out.println(y);
  }

  public static void main(String[] args) {
    System.out.println("a multi line comment line 1\na multi line comment line 2");
    System.out.println("public static final int constant = 3;");
    System.out.println("MAIN:");
    System.out.println("some doc");
    System.out.println("int x = 3");
    int x = 3;
    System.out.println(x);
    System.out.println("other doc");
    System.out.println("int y = tech.catheu.jln.experiment.Nb.aNumber()");
    int y = tech.catheu.jln.render.Nb.aNumber();
    System.out.println(y);
    System.out.println("public static int aNumber() {\n  return 7;\n}");
  }
}
