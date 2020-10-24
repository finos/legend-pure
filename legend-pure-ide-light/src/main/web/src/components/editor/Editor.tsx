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

import React, { useEffect, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import ReactResizeDetector from 'react-resize-detector';
import SplitPane from 'react-split-pane';
import { AuxiliaryPanel } from './aux-panel/AuxiliaryPanel';
import { SideBar } from './side-bar/SideBar';
import { GlobalHotKeys } from 'react-hotkeys';
import { ActivityBar } from './ActivityBar';
import { SIDE_BAR_RESIZE_SNAP_THRESHOLD, DEFAULT_SIDE_BAR_SIZE, AUX_PANEL_RESIZE_SNAP_THRESHOLD, HOTKEY, HOTKEY_MAP } from 'Stores/EditorConfig';
import { EditorStoreProvider, useEditorStore } from 'Stores/EditorStore';
import clsx from 'clsx';
import Backdrop from '@material-ui/core/Backdrop';
import { useApplicationStore } from 'Stores/ApplicationStore';
import { StatusBar } from 'Components/editor/StatusBar';
import { EditPanel } from 'Components/editor/edit-panel/EditPanel';
import { flowResult } from 'mobx';
import { parse } from 'query-string';
import { FileEditorState } from 'Stores/EditorState';
import { FileSearchCommand } from 'Components/editor/command-center/FileSearchCommand';
import { TextSearchCommand } from 'Components/editor/command-center/TextSearchCommand';

interface EditorQueryParams {
  mode?: string;
  fastCompile?: string;
}

export const EditorInner = observer(() => {
  const editorStore = useEditorStore();
  const applicationStore = useApplicationStore();

  // Resize
  const editorContainerRef = useRef<HTMLDivElement>(null);
  // These create snapping effect on panel resizing
  const snapSideBar = (newSize: number | undefined): void => {
    if (newSize !== undefined) {
      editorStore.setSideBarSize(newSize < SIDE_BAR_RESIZE_SNAP_THRESHOLD ? (editorStore.sideBarSize > 0 ? 0 : DEFAULT_SIDE_BAR_SIZE) : newSize);
    }
  };
  const handleResize = (): void => {
    if (editorContainerRef.current) {
      editorStore.setMaxAuxPanelSize(editorContainerRef.current.offsetHeight);
    }
  };
  const snapAuxPanel = (newSize: number | undefined): void => {
    if (editorContainerRef.current) {
      if (newSize !== undefined) {
        if (newSize >= editorContainerRef.current.offsetHeight - AUX_PANEL_RESIZE_SNAP_THRESHOLD) {
          editorStore.setAuxPanelSize(editorContainerRef.current.offsetHeight);
        } else if (newSize <= AUX_PANEL_RESIZE_SNAP_THRESHOLD) {
          editorStore.setAuxPanelSize(editorStore.auxPanelSize > 0 ? 0 : AUX_PANEL_RESIZE_SNAP_THRESHOLD);
        } else {
          editorStore.setAuxPanelSize(newSize);
        }
      }
    }
  };

  useEffect(() => {
    if (editorContainerRef.current) {
      editorStore.setMaxAuxPanelSize(editorContainerRef.current.offsetHeight);
    }
  }, [editorStore]);

  // Hotkeys
  const keyMap = {
    [HOTKEY.SEARCH_FILE]: HOTKEY_MAP[HOTKEY.SEARCH_FILE],
    [HOTKEY.SEARCH_TEXT]: HOTKEY_MAP[HOTKEY.SEARCH_TEXT],
    [HOTKEY.EXECUTE]: HOTKEY_MAP[HOTKEY.EXECUTE],
    [HOTKEY.TOGGLE_AUX_PANEL]: HOTKEY_MAP[HOTKEY.TOGGLE_AUX_PANEL],
    [HOTKEY.GO_TO_FILE]: HOTKEY_MAP[HOTKEY.GO_TO_FILE],
    [HOTKEY.FULL_RECOMPILE]: HOTKEY_MAP[HOTKEY.FULL_RECOMPILE],
    [HOTKEY.RUN_TEST]: HOTKEY_MAP[HOTKEY.RUN_TEST],
  };
  const handlers = {
    [HOTKEY.SEARCH_FILE]: editorStore.createGlobalHotKeyAction(() => { editorStore.setOpenFileSearchCommand(true) }),
    [HOTKEY.SEARCH_TEXT]: editorStore.createGlobalHotKeyAction(() => { editorStore.setOpenTextSearchCommand(true) }),
    [HOTKEY.EXECUTE]: editorStore.createGlobalHotKeyAction(() => { flowResult(editorStore.executeGo()).catch(applicationStore.alertIllegalUnhandledError) }),
    [HOTKEY.TOGGLE_AUX_PANEL]: editorStore.createGlobalHotKeyAction(() => editorStore.toggleAuxPanel()),
    [HOTKEY.GO_TO_FILE]: editorStore.createGlobalHotKeyAction(() => {
      const currentEditorState = editorStore.currentEditorState;
      if (currentEditorState instanceof FileEditorState) {
        editorStore.directoryTreeState.revealPath(currentEditorState.path, true);
      }
    }),
    [HOTKEY.FULL_RECOMPILE]: editorStore.createGlobalHotKeyAction((event: KeyboardEvent | undefined) => { flowResult(editorStore.fullReCompile(Boolean(event?.shiftKey ?? event?.ctrlKey))).catch(applicationStore.alertIllegalUnhandledError) }),
    [HOTKEY.RUN_TEST]: editorStore.createGlobalHotKeyAction((event: KeyboardEvent | undefined) => { flowResult(editorStore.executeFullTestSuite(event?.shiftKey)).catch(applicationStore.alertIllegalUnhandledError) }),
  };

  // Cleanup the editor
  useEffect(() => (): void => { editorStore.cleanUp() }, [editorStore]);

  // Initialize the app
  useEffect(() => {
    const queryParams = parse(window.location.search) as EditorQueryParams;
    flowResult(editorStore.initialize(false, undefined, queryParams.mode, queryParams.fastCompile)).catch(applicationStore.alertIllegalUnhandledError);
  }, [editorStore, applicationStore]);

  const editable = editorStore.initState.hasSucceeded;

  return (
    <div className="editor">
      <GlobalHotKeys keyMap={keyMap} handlers={handlers}>
        <div className="editor__body">
          <ActivityBar />
          <Backdrop className="backdrop" open={editorStore.backdrop} />
          <ReactResizeDetector
            handleHeight={true}
            handleWidth={true}
            onResize={handleResize}
          >
            <div className="editor__content-container" ref={editorContainerRef}>
              <div className={clsx('editor__content', { 'editor__content--expanded': editorStore.isInExpandedMode })}>
                <SplitPane split="vertical" size={editorStore.sideBarSize} onDragFinished={snapSideBar} minSize={0} maxSize={-600}>
                  <SideBar />
                  <SplitPane primary="second" split="horizontal" size={editorStore.auxPanelSize} onDragFinished={snapAuxPanel} minSize={0} maxSize={0}>
                    <>
                      <EditPanel />
                    </>
                    <AuxiliaryPanel />
                  </SplitPane>
                </SplitPane>
              </div>
            </div>
          </ReactResizeDetector>
        </div>
        <StatusBar actionsDisabled={!editable} />
        {editable && <FileSearchCommand />}
        {editable && <TextSearchCommand />}
      </GlobalHotKeys>
    </div>
  );
});

export const Editor: React.FC = () => (
  <EditorStoreProvider>
    <EditorInner />
  </EditorStoreProvider>
);
