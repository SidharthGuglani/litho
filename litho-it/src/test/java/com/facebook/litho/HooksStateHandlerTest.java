/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.annotation.Nullable;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for the hooks state (useState) integration with StateHandler. */
@RunWith(LithoTestRunner.class)
public class HooksStateHandlerTest {

  private static final String GLOBAL_KEY = "globalKey";

  @Test
  public void copyHandler_copyingEmptyStateHandler_createsEmptyStateHandler() {
    final StateHandler first = new StateHandler();
    final StateHandler second = new StateHandler(first);

    assertThat(second.hasUncommittedUpdates()).isFalse();
    assertThat(second.isEmpty()).isTrue();
  }

  @Test
  public void
      commit_copyingHandlerWithPendingStateUpdateAndCommittingIt_copiesAllOldStateValuesAndAppliesStateUpdate() {
    final Object bazState = new Object();
    KStateContainer kStateContainer = KStateContainer.withNewState(null, "test");
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4);
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState);

    final StateHandler first = new StateHandler();
    first.addStateContainer(GLOBAL_KEY, kStateContainer);

    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer currentState) {
            return KStateContainer.withNewState(currentState, "newValue");
          }
        });

    assertThat(first.hasUncommittedUpdates()).isTrue();

    final StateHandler second = new StateHandler(first);

    assertThat(first.hasUncommittedUpdates()).isTrue();
    assertThat(second.hasUncommittedUpdates()).isTrue();

    final KStateContainer secondKstate = (KStateContainer) second.getStateContainer(GLOBAL_KEY);
    second.keepStateContainerForGlobalKey(GLOBAL_KEY);

    assertThat(secondKstate.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"));

    first.commit(second);

    assertThat(first.hasUncommittedUpdates()).isFalse();

    final KStateContainer firstState = (KStateContainer) first.getStateContainer(GLOBAL_KEY);

    assertThat(firstState.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"));
  }

  @Test
  public void
      commit_copyingHandlerWithMultiplePendingStateUpdatesAndCommittingIt_copiesAllOldStateValuesAndAppliesAllStateUpdates() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    KStateContainer kStateContainer = KStateContainer.withNewState(null, "test");
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4);
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState);

    first.addStateContainer(GLOBAL_KEY, kStateContainer);
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer currentState) {
            return KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5);
          }
        });

    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer currentState) {
            return currentState.copyAndMutate(1, 1 + (int) currentState.mStates.get(1));
          }
        });

    assertThat(first.hasUncommittedUpdates()).isTrue();

    final StateHandler second = new StateHandler(first);

    assertThat(first.hasUncommittedUpdates()).isTrue();
    assertThat(second.hasUncommittedUpdates()).isTrue();

    assertThat(second.getStateContainers()).hasSize(1);

    final KStateContainer secondKstate = (KStateContainer) second.getStateContainer(GLOBAL_KEY);
    second.keepStateContainerForGlobalKey(GLOBAL_KEY);

    assertThat(secondKstate.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"));

    first.commit(second);

    assertThat(first.hasUncommittedUpdates()).isFalse();
    final KStateContainer kState = (KStateContainer) first.getStateContainer(GLOBAL_KEY);

    assertThat(kState.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"));
  }

  @Test
  public void
      commit_committingHandlerFirstWithPartOfStateUpdatesAndSecondTimeWithAllStateUpdates_atFirstAppliesOnlyStateUpdatesFromFirstCommitAndKeepOthersPendingAndThenAppliesAllStateUpdates() {
    final Object bazState = new Object();
    final StateHandler first = new StateHandler();
    KStateContainer kStateContainer = KStateContainer.withNewState(null, "test");
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4);
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState);

    first.addStateContainer(GLOBAL_KEY, kStateContainer);
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer currentState) {
            return KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5);
          }
        });

    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer current) {
            return current.copyAndMutate(1, ((Integer) current.mStates.get(1)) + 1);
          }
        });

    final StateHandler second = new StateHandler(first);

    first.queueHookStateUpdate(
        GLOBAL_KEY,
        new HookUpdater() {
          @Nullable
          @Override
          public KStateContainer getUpdatedStateContainer(@Nullable KStateContainer current) {
            return current.copyAndMutate(1, ((Integer) current.mStates.get(1)) + 1);
          }
        });

    final StateHandler third = new StateHandler(first);
    third.keepStateContainerForGlobalKey(GLOBAL_KEY);

    final KStateContainer secondKstate = (KStateContainer) second.getStateContainer(GLOBAL_KEY);
    second.keepStateContainerForGlobalKey(GLOBAL_KEY);

    assertThat(secondKstate.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"));

    first.commit(second);

    assertThat(first.hasUncommittedUpdates()).isTrue();

    final KStateContainer kState = (KStateContainer) first.getStateContainer(GLOBAL_KEY);
    assertThat(kState.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"));

    first.commit(third);

    assertThat(first.hasUncommittedUpdates()).isFalse();

    final KStateContainer firstStateUpdated = (KStateContainer) first.getStateContainer(GLOBAL_KEY);
    assertThat(firstStateUpdated.mStates)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 7, bazState, "newValue"));
  }
}
