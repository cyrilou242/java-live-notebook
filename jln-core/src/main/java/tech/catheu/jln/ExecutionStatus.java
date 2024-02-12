/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static tech.catheu.jln.ExecutionStatus.Status.FAILURE;
import static tech.catheu.jln.ExecutionStatus.Status.OK;

public record ExecutionStatus(@NonNull Status status,
                              @Nullable String failureMessage,
                              @Nullable Exception failureException) {

  public enum Status {
    OK,
    FAILURE
  }

  public boolean isOk() {
    return this.status.equals(OK);
  }

  public static ExecutionStatus ok() {
    return new ExecutionStatus(OK, null, null);
  }

  public static ExecutionStatus failure(@NonNull final String failureMessage,
                                 @Nullable final Exception failureException) {
    return new ExecutionStatus(FAILURE, failureMessage, failureException);
  }
}

