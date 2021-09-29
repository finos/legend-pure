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

import { useEffect, useRef } from 'react';
import { observer } from 'mobx-react-lite';
import { useResizeDetector } from 'react-resize-detector';
import type { DiagramEditorState } from '../../../stores/DiagramEditorState';
import { DiagramRenderer } from '@finos/legend-extension-dsl-diagram';

const DiagramCanvas = observer(
  (
    props: {
      diagramEditorState: DiagramEditorState;
    },
    ref: React.Ref<HTMLDivElement>,
  ) => {
    const { diagramEditorState } = props;
    const diagram = diagramEditorState.diagram;
    const diagramCanvasRef =
      ref as React.MutableRefObject<HTMLDivElement | null>;

    const { width, height } = useResizeDetector<HTMLDivElement>({
      refreshMode: 'debounce',
      refreshRate: 50,
      targetRef: diagramCanvasRef,
    });

    useEffect(() => {
      if (diagramCanvasRef.current) {
        const renderer = new DiagramRenderer(diagramCanvasRef.current, diagram);
        diagramEditorState.setRenderer(renderer);
        renderer.render();
        renderer.autoRecenter();
      }
    }, [diagramCanvasRef, diagramEditorState, diagram]);

    useEffect(() => {
      if (diagramEditorState.isDiagramRendererInitialized) {
        diagramEditorState.renderer.refresh();
      }
    }, [diagramEditorState, width, height]);

    return (
      <div
        ref={diagramCanvasRef}
        className="diagram-canvas"
        tabIndex={0}
        onContextMenu={(event): void => event.preventDefault()}
      />
    );
  },
  { forwardRef: true },
);

export const DiagramEditor = observer(
  (props: { editorState: DiagramEditorState }) => {
    const { editorState } = props;
    const diagramInfo = editorState.diagramInfo;
    const diagramCanvasRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      editorState.rebuild(diagramInfo);
    }, [editorState, diagramInfo]);

    return (
      <div className="panel edit-panel">
        <div className="panel__content edit-panel__content edit-panel__content--headless">
          <DiagramCanvas
            ref={diagramCanvasRef}
            diagramEditorState={editorState}
          />
        </div>
      </div>
    );
  },
);
