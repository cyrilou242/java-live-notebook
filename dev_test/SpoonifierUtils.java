/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln.render;

import spoon.Launcher;
import spoon.experimental.SpoonifierVisitor;

public class SpoonifierUtils {

  public static void main(String[] args) {
    SpoonifierVisitor v = new SpoonifierVisitor(true);
    Launcher.parseClass("""
                        class A { 
                        public static void main () {
                        for (int i= 0; i<3; i++) {
                         System.out.println(i);
                        } 
                        return "Hello World!";}}
                        """)
            .getMethodsByName("main")
            .get(0)
            .accept(v);
    System.out.println(v.getResult());
  }

}
