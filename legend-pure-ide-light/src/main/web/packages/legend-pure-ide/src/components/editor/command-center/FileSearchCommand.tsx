/**
 * Copyright (c) 2020-present, Goldman Sachs
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

import { useMemo, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { VscRegex } from 'react-icons/vsc';
import { compareLabelFn, debounce } from '../../../utils/GeneralUtil';
import { useEditorStore } from '../../../stores/EditorStore';
import Dialog from '@material-ui/core/Dialog';
import type {
  SelectComponent,
  SelectOption,
} from '../..//shared/CustomSelectorInput';
import { CustomSelectorInput } from '../../shared/CustomSelectorInput';
import clsx from 'clsx';
import { flowResult } from 'mobx';
import { useApplicationStore } from '../../../stores/ApplicationStore';

export const FileSearchCommand = observer(() => {
  const editorStore = useEditorStore();
  const applicationStore = useApplicationStore();
  const loadingOptionsState = editorStore.fileSearchCommandLoadingState;
  const searchState = editorStore.fileSearchCommandState;
  const selectorRef = useRef<SelectComponent>(null);
  // configs
  const toggleRegExp = (): void => searchState.toggleRegExp();
  // actions
  const debouncedSearch = useMemo(
    () =>
      debounce((): void => {
        flowResult(editorStore.searchFile()).catch(
          applicationStore.alertIllegalUnhandledError,
        );
      }, 500),
    [applicationStore, editorStore],
  );
  const closeModal = (): void => editorStore.setOpenFileSearchCommand(false);
  const onSearchTextChange = (val: string): void => {
    searchState.setText(val);
    debouncedSearch.cancel();
    debouncedSearch();
  };
  const openFile = (val: SelectOption | null): void => {
    if (val?.value) {
      closeModal();
      searchState.reset();
      flowResult(editorStore.loadFile(val.value)).catch(
        applicationStore.alertIllegalUnhandledError,
      );
    }
  };
  const handleEnter = (): void => {
    selectorRef.current?.focus();
  };

  return (
    <Dialog
      open={editorStore.openFileSearchCommand}
      onClose={closeModal}
      onEnter={handleEnter}
      classes={{ container: 'command-modal__container' }}
      PaperProps={{ classes: { root: 'command-modal__inner-container' } }}
    >
      <div className="modal modal--dark command-modal">
        <div className="modal__title">Open file</div>
        <div className="command-modal__content">
          <CustomSelectorInput
            ref={selectorRef}
            className="command-modal__content__input"
            options={editorStore.fileSearchCommandResults
              .map((option) => ({ label: option, value: option }))
              .sort(compareLabelFn)}
            onChange={openFile}
            onInputChange={onSearchTextChange}
            placeholder="Enter file name or path"
            escapeClearsValue={true}
            darkMode={true}
            isLoading={loadingOptionsState.isInProgress}
          />
          <button
            className={clsx('command-modal__content__config-btn btn--sm', {
              'command-modal__content__config-btn--toggled':
                searchState.isRegExp,
            })}
            title={`Use Regular Expression (${
              searchState.isRegExp ? 'on' : 'off'
            })`}
            onClick={toggleRegExp}
          >
            <VscRegex />
          </button>
        </div>
      </div>
    </Dialog>
  );
});
