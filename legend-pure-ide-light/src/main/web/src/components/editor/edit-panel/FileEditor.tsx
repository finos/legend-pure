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
import { disposeEditor, baseTextEditorSettings, disableEditorHotKeys, moveToPosition, setErrorMarkers } from 'Utilities/TextEditorUtil';
import { TAB_SIZE, EDITOR_THEME, EDITOR_LANGUAGE } from 'Stores/EditorConfig';
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
  const handleResize = (width: number, height: number): void => editor?.layout({ height, width });

  useEffect(() => {
    if (!editor && textInput.current) {
      const element = textInput.current;
      const editor = monacoEditorAPI.create(element, {
        ...baseTextEditorSettings,
        language: EDITOR_LANGUAGE.PURE,
        theme: EDITOR_THEME.NATIVE,
        fontSize: 12,
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
