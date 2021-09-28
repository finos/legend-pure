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

import { action, makeObservable, observable } from 'mobx';
import type { DiagramInfo } from '../models/DiagramInfo';
import { trimPathLeadingSlash } from '../models/PureFile';
import { EditorState } from './EditorState';
import type { EditorStore } from './EditorStore';

export class DiagramEditorState extends EditorState {
  diagramInfo: DiagramInfo;
  path: string;

  constructor(
    editorStore: EditorStore,
    diagramInfo: DiagramInfo,
    path: string,
  ) {
    super(editorStore);

    makeObservable(this, {
      diagramInfo: observable,
      setDiagramInfo: action,
    });

    this.diagramInfo = diagramInfo;
    this.path = path;
  }

  setDiagramInfo(value: DiagramInfo): void {
    this.diagramInfo = value;
  }

  get headerName(): string {
    return trimPathLeadingSlash(this.path);
  }
}
