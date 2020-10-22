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
import { Editor } from './editor/Editor';
import { ApplicationStoreProvider } from 'Stores/ApplicationStore';
import { NotificationSnackbar } from 'Components/shared/NotificationSnackbar';
import { ActionAlert } from 'Components/application/ActionAlert';
import { BlockingAlert } from 'Components/application/BlockingAlert';
import { MuiThemeProvider } from '@material-ui/core';
import { materialUiTheme } from 'Style/MaterialUITheme';

export const App: React.FC = () => (
  <ApplicationStoreProvider>
    <MuiThemeProvider theme={materialUiTheme}>
      <div className="app">
        <BlockingAlert />
        <ActionAlert />
        <NotificationSnackbar />
        <Editor />
      </div>
    </MuiThemeProvider>
  </ApplicationStoreProvider>
);
