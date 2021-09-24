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

import { uuid } from '@finos/legend-shared';
import { action, makeObservable, observable } from 'mobx';
import type { FileCoordinate, PureFile } from '../models/PureFile';
import { trimPathLeadingSlash } from '../models/PureFile';
import type { EditorStore } from './EditorStore';

export abstract class EditorState {
  uuid = uuid(); // NOTE: used to detect when an element editor state changes so we can force a remount of the editor component
  editorStore: EditorStore;

  constructor(editorStore: EditorStore) {
    this.editorStore = editorStore;
  }

  abstract get headerName(): string;
}

export class FileEditorState extends EditorState {
  file: PureFile;
  path: string;
  coordinate?: FileCoordinate | undefined;

  constructor(
    editorStore: EditorStore,
    file: PureFile,
    path: string,
    coordinate?: FileCoordinate,
  ) {
    super(editorStore);
    makeObservable(this, {
      file: observable,
      coordinate: observable,
      setFile: action,
      setCoordinate: action,
    });
    this.file = file;
    this.path = path;
    this.coordinate = coordinate;
  }

  setFile(value: PureFile): void {
    this.file = value;
  }
  setCoordinate(value: FileCoordinate | undefined): void {
    this.coordinate = value;
  }
  clearError(): void {
    this.coordinate?.setErrorMessage(undefined);
  }

  get headerName(): string {
    return trimPathLeadingSlash(this.path);
  }
}
