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

import { Editor } from './editor/Editor';
import { ThemeProvider } from '@material-ui/core';
import { LegendMaterialUITheme } from '@finos/legend-art';
import {
  ActionAlert,
  ApplicationStoreProvider,
  BlockingAlert,
  NotificationSnackbar,
  useWebApplicationNavigator,
} from '@finos/legend-application';
import type { PureIDEConfig } from '../application/PureIDEConfig';
import type { PureIDEPluginManager } from '../application/PureIDEPluginManager';
import { observer } from 'mobx-react-lite';
import type { Log } from '@finos/legend-shared';

export const LegendPureIDEApplication = observer(
  (props: {
    config: PureIDEConfig;
    pluginManager: PureIDEPluginManager;
    log: Log;
  }) => {
    const { config, log } = props;
    const navigator = useWebApplicationNavigator();

    return (
      <ApplicationStoreProvider config={config} navigator={navigator} log={log}>
        <ThemeProvider theme={LegendMaterialUITheme}>
          <div className="app">
            <BlockingAlert />
            <ActionAlert />
            <NotificationSnackbar />
            <Editor />
          </div>
        </ThemeProvider>
      </ApplicationStoreProvider>
    );
  },
);
