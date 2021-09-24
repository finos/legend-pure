/**
 * Copyright (c) 2020-present, Goldman Sachs
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

import { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { AuxiliaryPanel } from './aux-panel/AuxiliaryPanel';
import { SideBar } from './side-bar/SideBar';
import { GlobalHotKeys } from 'react-hotkeys';
import { ActivityBar } from './ActivityBar';
import { IDE_HOTKEY, IDE_HOTKEY_MAP } from '../../stores/EditorConfig';
import { EditorStoreProvider, useEditorStore } from '../../stores/EditorStore';
import { StatusBar } from './StatusBar';
import { EditPanel } from './edit-panel/EditPanel';
import { flowResult } from 'mobx';
import { FileEditorState } from '../../stores/EditorState';
import { FileSearchCommand } from './command-center/FileSearchCommand';
import { TextSearchCommand } from './command-center/TextSearchCommand';
import { useApplicationStore } from '@finos/legend-application';
import type { ResizablePanelHandlerProps } from '@finos/legend-art';
import {
  clsx,
  ResizablePanelSplitterLine,
  ResizablePanel,
  ResizablePanelGroup,
  ResizablePanelSplitter,
  getControlledResizablePanelProps,
} from '@finos/legend-art';
import { useResizeDetector } from 'react-resize-detector';
import { getQueryParameters } from '@finos/legend-shared';

interface EditorQueryParams {
  mode?: string;
  fastCompile?: string;
}

export const EditorInner = observer(() => {
  const editorStore = useEditorStore();
  const applicationStore = useApplicationStore();
  const { ref, width, height } = useResizeDetector<HTMLDivElement>();

  const resizeSideBar = (handleProps: ResizablePanelHandlerProps): void =>
    editorStore.sideBarDisplayState.setSize(
      (handleProps.domElement as HTMLDivElement).getBoundingClientRect().width,
    );
  const resizeAuxPanel = (handleProps: ResizablePanelHandlerProps): void =>
    editorStore.auxPanelDisplayState.setSize(
      (handleProps.domElement as HTMLDivElement).getBoundingClientRect().height,
    );

  useEffect(() => {
    if (ref.current) {
      editorStore.auxPanelDisplayState.setMaxSize(ref.current.offsetHeight);
    }
  }, [editorStore, ref, height, width]);

  // Hotkeys
  const keyMap = {
    [IDE_HOTKEY.SEARCH_FILE]: IDE_HOTKEY_MAP[IDE_HOTKEY.SEARCH_FILE],
    [IDE_HOTKEY.SEARCH_TEXT]: IDE_HOTKEY_MAP[IDE_HOTKEY.SEARCH_TEXT],
    [IDE_HOTKEY.EXECUTE]: IDE_HOTKEY_MAP[IDE_HOTKEY.EXECUTE],
    [IDE_HOTKEY.TOGGLE_AUX_PANEL]: IDE_HOTKEY_MAP[IDE_HOTKEY.TOGGLE_AUX_PANEL],
    [IDE_HOTKEY.GO_TO_FILE]: IDE_HOTKEY_MAP[IDE_HOTKEY.GO_TO_FILE],
    [IDE_HOTKEY.FULL_RECOMPILE]: IDE_HOTKEY_MAP[IDE_HOTKEY.FULL_RECOMPILE],
    [IDE_HOTKEY.RUN_TEST]: IDE_HOTKEY_MAP[IDE_HOTKEY.RUN_TEST],
    [IDE_HOTKEY.TOGGLE_OPEN_TABS_MENU]:
      IDE_HOTKEY_MAP[IDE_HOTKEY.TOGGLE_OPEN_TABS_MENU],
  };
  const handlers = {
    [IDE_HOTKEY.SEARCH_FILE]: editorStore.createGlobalHotKeyAction(() => {
      editorStore.setOpenFileSearchCommand(true);
    }),
    [IDE_HOTKEY.SEARCH_TEXT]: editorStore.createGlobalHotKeyAction(() => {
      editorStore.setOpenTextSearchCommand(true);
    }),
    [IDE_HOTKEY.EXECUTE]: editorStore.createGlobalHotKeyAction(() => {
      flowResult(editorStore.executeGo()).catch(
        applicationStore.alertIllegalUnhandledError,
      );
    }),
    [IDE_HOTKEY.TOGGLE_AUX_PANEL]: editorStore.createGlobalHotKeyAction(() =>
      editorStore.auxPanelDisplayState.toggle(),
    ),
    [IDE_HOTKEY.GO_TO_FILE]: editorStore.createGlobalHotKeyAction(() => {
      const currentEditorState = editorStore.currentEditorState;
      if (currentEditorState instanceof FileEditorState) {
        editorStore.directoryTreeState.revealPath(
          currentEditorState.path,
          true,
        );
      }
    }),
    [IDE_HOTKEY.FULL_RECOMPILE]: editorStore.createGlobalHotKeyAction(
      (event: KeyboardEvent | undefined) => {
        flowResult(
          editorStore.fullReCompile(Boolean(event?.shiftKey ?? event?.ctrlKey)),
        ).catch(applicationStore.alertIllegalUnhandledError);
      },
    ),
    [IDE_HOTKEY.RUN_TEST]: editorStore.createGlobalHotKeyAction(
      (event: KeyboardEvent | undefined) => {
        flowResult(editorStore.executeFullTestSuite(event?.shiftKey)).catch(
          applicationStore.alertIllegalUnhandledError,
        );
      },
    ),
    // NOTE: right now this is fairly simplistic, we can create it to navigate in 2 directions like `Tab` and `Shift + Tab`.
    // in VSCode for example, they always show the current tab on top/bottom based on the navigation direction
    [IDE_HOTKEY.TOGGLE_OPEN_TABS_MENU]: editorStore.createGlobalHotKeyAction(
      () => {
        editorStore.setShowOpenedTabsMenu(!editorStore.showOpenedTabsMenu);
      },
    ),
  };

  // Cleanup the editor
  useEffect(
    () => (): void => {
      editorStore.cleanUp();
    },
    [editorStore],
  );

  // Initialize the app
  useEffect(() => {
    const queryParams = getQueryParameters<EditorQueryParams>(
      window.location.search,
    );
    flowResult(
      editorStore.initialize(
        false,
        undefined,
        queryParams.mode,
        queryParams.fastCompile,
      ),
    ).catch(applicationStore.alertIllegalUnhandledError);
  }, [editorStore, applicationStore]);

  const editable = editorStore.initState.hasSucceeded;

  return (
    <div className="editor">
      <GlobalHotKeys keyMap={keyMap} handlers={handlers}>
        <div className="editor__body">
          <ActivityBar />
          <div className="editor__content-container" ref={ref}>
            <div
              className={clsx('editor__content', {
                'editor__content--expanded': editorStore.isInExpandedMode,
              })}
            >
              <ResizablePanelGroup orientation="vertical">
                <ResizablePanel
                  {...getControlledResizablePanelProps(
                    editorStore.sideBarDisplayState.size === 0,
                    {
                      onStopResize: resizeSideBar,
                    },
                  )}
                  size={editorStore.sideBarDisplayState.size}
                  direction={1}
                >
                  <SideBar />
                </ResizablePanel>
                <ResizablePanelSplitter />
                <ResizablePanel minSize={100}>
                  <ResizablePanelGroup orientation="horizontal">
                    <ResizablePanel
                      {...getControlledResizablePanelProps(
                        editorStore.auxPanelDisplayState.isMaximized,
                      )}
                    >
                      <EditPanel />
                    </ResizablePanel>
                    <ResizablePanelSplitter>
                      <ResizablePanelSplitterLine
                        color={
                          editorStore.auxPanelDisplayState.isMaximized
                            ? 'transparent'
                            : 'var(--color-dark-grey-250)'
                        }
                      />
                    </ResizablePanelSplitter>
                    <ResizablePanel
                      {...getControlledResizablePanelProps(
                        editorStore.auxPanelDisplayState.size === 0,
                        {
                          onStopResize: resizeAuxPanel,
                        },
                      )}
                      flex={0}
                      direction={-1}
                      size={editorStore.auxPanelDisplayState.size}
                    >
                      <AuxiliaryPanel />
                    </ResizablePanel>
                  </ResizablePanelGroup>
                </ResizablePanel>
              </ResizablePanelGroup>
            </div>
          </div>
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
