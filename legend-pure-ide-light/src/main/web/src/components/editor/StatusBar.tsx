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

import React from 'react';
import { observer } from 'mobx-react-lite';
import { FaRegWindowMaximize, FaTerminal, FaHammer } from 'react-icons/fa';
import { useEditorStore } from 'Stores/EditorStore';
import clsx from 'clsx';
import { flowResult } from 'mobx';
import { useApplicationStore } from 'Stores/ApplicationStore';

export const StatusBar = observer((props: {
  actionsDisabled: boolean;
}) => {
  const editorStore = useEditorStore();
  const applicationStore = useApplicationStore();

  // Other actions
  const toggleAuxPanel = (): void => editorStore.toggleAuxPanel();
  const toggleExpandMode = (): void => editorStore.setExpandedMode(!editorStore.isInExpandedMode);
  const executeGo = (): Promise<void> => flowResult(editorStore.executeGo()).catch(applicationStore.alertIllegalUnhandledError);

  return (
    <div className={clsx('editor__status-bar')}>
      <div className="editor__status-bar__left">
        <div className="editor__status-bar__workspace">
        </div>
      </div>
      <div className="editor__status-bar__right">
        <button
          className={clsx('editor__status-bar__action editor__status-bar__compile-btn',
            { 'editor__status-bar__compile-btn--wiggling': editorStore.executionState.isInProgress }
          )}
          disabled={editorStore.executionState.isInProgress}
          onClick={executeGo}
          tabIndex={-1}
          title="Execute (F9)"
        ><FaHammer /></button>
        <button
          className={clsx('editor__status-bar__action editor__status-bar__action__toggler',
            { 'editor__status-bar__action__toggler--active': editorStore.isInExpandedMode }
          )}
          onClick={toggleExpandMode}
          tabIndex={-1}
          title="Maximize/Minimize"
        ><FaRegWindowMaximize /></button>
        <button
          className={clsx('editor__status-bar__action editor__status-bar__action__toggler',
            { 'editor__status-bar__action__toggler--active': Boolean(editorStore.auxPanelSize) }
          )}
          onClick={toggleAuxPanel}
          tabIndex={-1}
          title="Toggle auxiliary panel (Ctrl + `)"
        ><FaTerminal /></button>
      </div>
    </div >
  );
});
