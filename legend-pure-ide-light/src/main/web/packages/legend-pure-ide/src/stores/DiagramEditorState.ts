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

import type {
  Diagram,
  DiagramRenderer,
} from '@finos/legend-extension-dsl-diagram';
import type { PureModel } from '@finos/legend-graph';
import { guaranteeNonNullable } from '@finos/legend-shared';
import { action, makeObservable, observable } from 'mobx';
import type { DiagramInfo } from '../models/DiagramInfo';
import { buildGraphFromDiagramInfo } from '../models/DiagramInfo';
import { trimPathLeadingSlash } from '../models/PureFile';
import { EditorState } from './EditorState';
import type { EditorStore } from './EditorStore';

export class DiagramEditorState extends EditorState {
  diagramInfo: DiagramInfo;
  _renderer?: DiagramRenderer | undefined;
  diagram: Diagram;
  graph: PureModel;
  path: string;

  constructor(
    editorStore: EditorStore,
    diagramInfo: DiagramInfo,
    path: string,
  ) {
    super(editorStore);

    makeObservable(this, {
      _renderer: observable,
      diagram: observable,
      diagramInfo: observable,
      rebuild: action,
      setRenderer: action,
    });

    this.path = path;
    this.diagramInfo = diagramInfo;
    const [diagram, graph] = buildGraphFromDiagramInfo(diagramInfo);
    this.diagram = diagram;
    this.graph = graph;
  }

  rebuild(value: DiagramInfo): void {
    this.diagramInfo = value;
    const [diagram, graph] = buildGraphFromDiagramInfo(value);
    this.diagram = diagram;
    this.graph = graph;
  }

  get renderer(): DiagramRenderer {
    return guaranteeNonNullable(
      this._renderer,
      `Diagram renderer must be initialized (this is likely caused by calling this method at the wrong place)`,
    );
  }

  get isDiagramRendererInitialized(): boolean {
    return Boolean(this._renderer);
  }

  setRenderer(val: DiagramRenderer): void {
    this._renderer = val;
  }

  get headerName(): string {
    return trimPathLeadingSlash(this.path);
  }
}