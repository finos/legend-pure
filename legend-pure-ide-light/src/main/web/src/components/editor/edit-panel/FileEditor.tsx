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

import React, { useEffect, useState, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { editor as monacoEditorAPI, KeyCode } from 'monaco-editor';
import { useEditorStore } from 'Stores/EditorStore';
import { disposeEditor, disableEditorHotKeys, moveToPosition, setErrorMarkers } from 'Utilities/TextEditorUtil';
import { TAB_SIZE, EDITOR_THEME, EDITOR_LANGUAGE, MONOSPACE_FONT_FAMILY } from 'Stores/EditorConfig';
import ReactResizeDetector from 'react-resize-detector';
import { useApplicationStore } from 'Stores/ApplicationStore';
import type { FileEditorState } from 'Stores/EditorState';
import { flowResult } from 'mobx';
import { FileCoordinate } from 'Models/PureFile';

export const FileEditor = observer((props: {
  editorState: FileEditorState
}) => {
  const { editorState } = props;
  const [editor, setEditor] = useState<monacoEditorAPI.IStandaloneCodeEditor | undefined>();
  const editorStore = useEditorStore();
  const applicationStore = useApplicationStore();
  const content = editorState.file.content;
  const textInput = useRef<HTMLDivElement>(null);
  const handleResize = (width: number | undefined, height: number | undefined): void => editor?.layout({ height: height ?? 0, width: width ?? 0 });

  useEffect(() => {
    if (!editor && textInput.current) {
      const element = textInput.current;
      const editor = monacoEditorAPI.create(element, {
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
        fontFamily: MONOSPACE_FONT_FAMILY,
        fontLigatures: true,
        fixedOverflowWidgets: true, // make sure hover or widget near boundary are not truncated
        language: EDITOR_LANGUAGE.PURE,
        theme: EDITOR_THEME.NATIVE,
      });
      editor.onDidChangeModelContent(() => {
        const currentVal = editor.getValue();
        if (currentVal !== editorState.file.content) {
          // the assertion above is to ensure we don't accidentally clear error on initialization of the editor
          editorState.clearError(); // clear error on content change/typing
        }
        editorState.file.setContent(currentVal);
      });
      editor.onKeyDown(event => {
        if (event.keyCode === KeyCode.F9) {
          event.preventDefault();
          event.stopPropagation();
          flowResult(editorStore.executeGo()).catch(applicationStore.alertIllegalUnhandledError);
        } else if (event.keyCode === KeyCode.KEY_B && event.ctrlKey && !event.altKey) {
          // [ctrl + b] Navigate
          const currentPosition = editor.getPosition();
          if (currentPosition) {
            const coordinate = new FileCoordinate(editorState.path, currentPosition.lineNumber, currentPosition.column);
            flowResult(editorStore.executeNavigation(coordinate)).catch(applicationStore.alertIllegalUnhandledError);
          }
        } else if (event.keyCode === KeyCode.KEY_B && event.ctrlKey && event.altKey) {
          // [ctrl + alt + b] Navigate back
          flowResult(editorStore.navigateBack()).catch(applicationStore.alertIllegalUnhandledError);
        } else if (event.keyCode === KeyCode.F7 && event.altKey) {
          // [alt + f7] Find usages
          const currentPosition = editor.getPosition();
          if (currentPosition) {
            const coordinate = new FileCoordinate(editorState.path, currentPosition.lineNumber, currentPosition.column);
            flowResult(editorStore.findUsages(coordinate)).catch(applicationStore.alertIllegalUnhandledError);
          }
        }
        // NOTE: Legacy IDE's [alt + g] -> go to line ~ equivalent to `monaco-editor`'s [ctrl + g]
      });
      disableEditorHotKeys(editor);
      editor.focus(); // focus on the editor initially
      setEditor(editor);
    }
  }, [editorStore, applicationStore, editor, editorState]);

  if (editor) {
    // Set the value of the editor
    const currentValue = editor.getValue();
    if (currentValue !== content) {
      editor.setValue(content);
    }
    const editorModel = editor.getModel();
    if (editorModel) {
      editorModel.updateOptions({ tabSize: TAB_SIZE });
      const pos = editorState.coordinate;
      if (pos?.errorMessage) {
        setErrorMarkers(editorModel, {
          sourceId: '',
          line: pos.line,
          column: pos.column,
          startLine: pos.line,
          startColumn: pos.column,
          endLine: pos.line,
          endColumn: pos.column,
        }, pos.errorMessage);
      } else {
        monacoEditorAPI.setModelMarkers(editorModel, 'Error', []);
      }
    }
  }

  useEffect(() => {
    const pos = editorState.coordinate;
    if (editor && pos) {
      moveToPosition(editor, pos.line, pos.column);
    }
  }, [editor, editorState.coordinate]);

  // NOTE: dispose the editor to prevent potential memory-leak
  useEffect(() => (): void => { if (editor) { disposeEditor(editor) } }, [editor]);

  return (
    <div className="panel edit-panel">
      <div className="panel__content edit-panel__content edit-panel__content--headless">
        <ReactResizeDetector
          handleWidth={true}
          handleHeight={true}
          onResize={handleResize}
        >
          <div className="text-editor__container">
            <div className="text-editor__body" ref={textInput} />
          </div>
        </ReactResizeDetector>
      </div>
    </div>
  );
});
