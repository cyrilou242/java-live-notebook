/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln.render;

// some comment
public class Nb {

  // # The big one
  // ## a title we like
  // ### Does this even work
  // [link](example.org)
  // a multi line comment line 1
  // a multi line comment line 2
  public static final int constant = 3;
  
  public static int aNumber() {
    System.out.println("lol");
    return 7;
  }

  // # Another one
  // ## yet another h2
  // ### h3 forever
  // links
  // text
  // and stuff
  // links
  // text
  // and stuff
  // and stuff
  // links
  // text
  // and stuff
  public static void main(String[] args) {
    // some doc
    
    // a space in doc
    int x = 3;
    int x2 = 7;
    int x3 = x + x2 * 2;
    
    for (int i =0; i<3; i++) {
      System.out.println(i);
    }

    // other doc
    int y = aNumber();
    // whatever
    
    // ## That's a header for you
    // nooo HEY HO NO WAY WAY WAY
    int res = y + x3 + 1;

    // # here
    // ## i go
    // ### h3 forever ever
    // # here
    java.lang.String c = "Coukilou Louise";
  }
}
