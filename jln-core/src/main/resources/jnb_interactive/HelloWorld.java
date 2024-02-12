/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
// # Hello world
// Cells are delimited by blank lines

String s1 = "Hello";

String s2 = "World";

// for multi-statements cells, only the last value or method result is returned.

String s3 = "!";
String greeting = s1 + " " + s2 + s3;

// import Nb to get access to built-in integrations


Nb.plotly(List.of(
          Map.of("z", List.of(List.of(1, 2, 3), List.of(1, 2, 3)), "type", "surface")),
          Map.of(),
          Map.of());

// ## markdown
// ### is
// #### supported
// Learn more on [jln.catheu.tech](https://jln.catheu.tech)
