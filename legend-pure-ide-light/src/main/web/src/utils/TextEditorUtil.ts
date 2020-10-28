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

import { editor as monacoEditorAPI, MarkerSeverity, KeyCode } from 'monaco-editor';
import { noop } from 'Utilities/GeneralUtil';
import type { SourceInformation } from 'Models/SourceInformation';

/**
 * Normally `monaco-editor` worker disposes after 5 minutes staying idle, but we fasten
 * this pace just in case the usage of the editor causes memory-leak somehow
 */
export const disposeEditor = (editor: monacoEditorAPI.IStandaloneCodeEditor): void => {
  editor.dispose();
  // NOTE: just to be sure, we dispose the model after disposing the editor
  editor.getModel()?.dispose();
};

export const disposeDiffEditor = (editor: monacoEditorAPI.IStandaloneDiffEditor): void => {
  editor.dispose();
  editor.getOriginalEditor().getModel()?.dispose();
  editor.getModifiedEditor().getModel()?.dispose();
};

export const baseTextEditorSettings: monacoEditorAPI.IStandaloneEditorConstructionOptions = {
  contextmenu: false,
  copyWithSyntaxHighlighting: false,
  // NOTE: These following font options are needed (and CSS font-size option `.monaco-editor * { font-size: ... }` as well)
  // in order to make the editor appear properly on multiple platform, the ligatures option is needed for Mac to display properly
  // otherwise the cursor position relatively to text would be off
  // Another potential cause for this misaligment is that the fonts are being lazy-loaded and made available after `monaco-editor`
  // calculated the font-width, for this, we can use `remeasureFonts`, but our case here, `fontLigatures: true` seems
  // to do the trick
  // See https://github.com/microsoft/monaco-editor/issues/392
  fontSize: 14,
  // Enforce a fixed font-family to make cross platform display consistent (i.e. Mac defaults to use `Menlo` which is bigger than
  // `Consolas` on Windows, etc.)
  fontFamily: 'Fira Code',
  fontLigatures: true,
};

// There is currently no good way to `monaco-editor` to disable hotkeys as part of the settings or global state
// See https://github.com/microsoft/monaco-editor/issues/287
// See https://github.com/microsoft/monaco-editor/issues/102
export const disableEditorHotKeys = (editor: monacoEditorAPI.IStandaloneCodeEditor | monacoEditorAPI.IStandaloneDiffEditor): void => {
  editor.addCommand(KeyCode.F1, noop()); // disable command pallete
  editor.addCommand(KeyCode.F8, noop()); // disable show error command
};

export const moveToPosition = (editor: monacoEditorAPI.ICodeEditor, line: number, column: number): void => {
  if (!editor.hasTextFocus()) { editor.focus() } // focus the editor first so that it can shows the cursor
  editor.revealPositionInCenter({ lineNumber: line, column: column }, 0);
  editor.setPosition({ lineNumber: line, column: column });
};

export const revealError = (editor: monacoEditorAPI.ICodeEditor, sourceInformation: SourceInformation): void => {
  moveToPosition(editor, sourceInformation.startLine, sourceInformation.startColumn);
};

export const setErrorMarkers = (editorModel: monacoEditorAPI.ITextModel, sourceInformation: SourceInformation, message: string): void => {
  const { startLine, startColumn, endLine, endColumn } = sourceInformation;
  monacoEditorAPI.setModelMarkers(editorModel, 'Error', [{
    startLineNumber: startLine,
    startColumn,
    endColumn: endColumn + 1, // add a 1 to endColumn as monaco editor range is not inclusive
    endLineNumber: endLine,
    message,
    severity: MarkerSeverity.Error
  }]);
};
