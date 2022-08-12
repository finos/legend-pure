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

import { observer } from 'mobx-react-lite';
import { useEditorStore } from '../../../stores/EditorStore';
import { useState } from 'react';

export const RuntimeOptions = observer(() => {
  const editorStore = useEditorStore();
  const [debugPlanOption, setDebugPlanOption] = useState(false);
  const [execPlanOption, setExecPlanOption] = useState(false);
  const [localPlanOption, setLocalPlanOption] = useState(false);
  const [showLocalPlanOption, setShowLocalPlanOption] = useState(false);

  const handleDebugOptionChange = (): void => {
    setDebugPlanOption(!debugPlanOption);
    editorStore.setDebugPlatformCodeGen(debugPlanOption);
  };

  const handleExecPlanOptionChange = (): void => {
    setExecPlanOption(!execPlanOption);
    editorStore.setExecPlan(execPlanOption);
  };

  const handleLocalPlanOptionChange = (): void => {
    setLocalPlanOption(!localPlanOption);
    editorStore.setLocalPlan(localPlanOption);
  };

  const handleShowLocalPlanOptionChange = (): void => {
    setShowLocalPlanOption(!showLocalPlanOption);
    editorStore.setShowLocalPlan(showLocalPlanOption);
  };

  const Checkbox = (obj: {
    id: string;
    label: string;
    value: boolean;
    onChange: () => void;
  }): JSX.Element => {
    const checkBoxId = obj.id.concat('__runtimeoptions-panel-checkbox');
    return (
      <label className={checkBoxId}>
        <input type="checkbox" checked={obj.value} onChange={obj.onChange} />
        {obj.label}
      </label>
    );
  };

  const checkboxes = [
    {
      id: 'debugPlatform',
      label: 'Print debug logs for platform code generation',
      value: editorStore.debugPlatformCodeGen,
      onChange: handleDebugOptionChange,
    },
    {
      id: 'genExecPlanEngine',
      label: 'Calculate and execute plans on Engine rather than functions',
      value: editorStore.execPlan,
      onChange: handleExecPlanOptionChange,
    },
    {
      id: 'genPlanIDE',
      label: 'Calculate plans in Pure IDE Light and execute on Engine',
      value: editorStore.localPlan,
      onChange: handleLocalPlanOptionChange,
    },
    {
      id: 'showLocalPlan',
      label: 'Print the plan calculated in Pure IDE Light',
      value: editorStore.showLocalPlan,
      onChange: handleShowLocalPlanOptionChange,
    },
  ];

  return (
    <div className="runtimeoptions-panel">
      <div className="runtimeoptions-buttons">
        <ul>
          {checkboxes.map(({ id, label, value, onChange }) => (
            // eslint-disable-next-line react/jsx-key
            <li>
              <Checkbox
                id={id}
                label={label}
                value={value}
                onChange={onChange}
              />
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
});
