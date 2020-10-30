/**
 * Copyright 2020 Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const TAB_SIZE = 2;

export const SIDE_BAR_RESIZE_SNAP_THRESHOLD = 150;
export const DEFAULT_SIDE_BAR_SIZE = 300;

export const AUX_PANEL_RESIZE_TOP_SNAP_THRESHOLD = 50;
export const AUX_PANEL_RESIZE_BOTTOM_SNAP_THRESHOLD = 150;
export const DEFAULT_AUX_PANEL_SIZE = 300;

export enum HOTKEY {
  SEARCH_FILE = 'SEARCH_FILE',
  SEARCH_TEXT = 'SEARCH_TEXT',
  EXECUTE = 'EXECUTE',
  TOGGLE_AUX_PANEL = 'TOGGLE_AUX_PANEL',
  GO_TO_FILE = 'GO_TO_FILE',
  FULL_RECOMPILE = 'FULL_RECOMPILE',
  RUN_TEST = 'RUN_TEST',
}

export const HOTKEY_MAP: Record<HOTKEY, string[]> = Object.freeze({
  [HOTKEY.SEARCH_FILE]: ['ctrl+p', 'ctrl+shift+n'],
  [HOTKEY.SEARCH_TEXT]: ['ctrl+shift+f'],
  [HOTKEY.TOGGLE_AUX_PANEL]: ['ctrl+`'],
  [HOTKEY.EXECUTE]: ['f9'],
  [HOTKEY.GO_TO_FILE]: ['ctrl+f1'],
  [HOTKEY.FULL_RECOMPILE]: ['f11', 'ctrl+f11', 'shift+f11'],
  [HOTKEY.RUN_TEST]: ['f10', 'shift+f10'],
});

export enum ACTIVITY_MODE {
  CONCEPT = 'CONCEPT',
  FILE = 'FILE',
}

export enum AUX_PANEL_MODE {
  CONSOLE = 'CONSOLE',
  SEARCH_RESULT = 'SEARCH_RESULT',
  TEST_RUNNER = 'TEST_RUNNER',
}

export enum EDITOR_THEME {
  NATIVE = 'NATIVE',
}

export enum EDITOR_LANGUAGE {
  PURE = 'pure',
}
