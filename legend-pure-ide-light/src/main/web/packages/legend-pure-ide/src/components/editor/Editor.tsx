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
import type { KeyMap } from 'react-hotkeys';
import { GlobalHotKeys } from 'react-hotkeys';
import { ActivityBar } from './ActivityBar';
import type { EditorHotkey } from '../../stores/EditorStore';
import { EditorStoreProvider, useEditorStore } from '../../stores/EditorStore';
import { StatusBar } from './StatusBar';
import { EditPanel } from './edit-panel/EditPanel';
import { flowResult } from 'mobx';
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
import { HTML5Backend } from 'react-dnd-html5-backend';
import { DndProvider } from 'react-dnd';

const buildHotkeySupport = (
  hotkeys: EditorHotkey[],
): [KeyMap, { [key: string]: (keyEvent?: KeyboardEvent) => void }] => {
  const keyMap: Record<PropertyKey, string[]> = {};
  hotkeys.forEach((hotkey) => {
    keyMap[hotkey.name] = hotkey.keyBinds;
  });
  const handlers: Record<PropertyKey, (keyEvent?: KeyboardEvent) => void> = {};
  hotkeys.forEach((hotkey) => {
    handlers[hotkey.name] = hotkey.handler;
  });
  return [keyMap, handlers];
};
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
  const [hotkeyMapping, hotkeyHandlers] = buildHotkeySupport(
    editorStore.hotkeys,
  );

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
      <GlobalHotKeys
        keyMap={hotkeyMapping}
        handlers={hotkeyHandlers}
        allowChanges={true}
      >
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
    <DndProvider backend={HTML5Backend}>
      <EditorInner />
    </DndProvider>
  </EditorStoreProvider>
);
