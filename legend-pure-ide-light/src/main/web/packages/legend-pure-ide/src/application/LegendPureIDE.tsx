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
  LegendApplicationConfig,
  LegendApplicationVersionData,
} from '@finos/legend-application';
import {
  LegendApplication,
  setupLegendApplicationUILibrary,
  WebApplicationNavigatorProvider,
} from '@finos/legend-application';
import { configure as configureReactHotkeys } from 'react-hotkeys';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import { LegendPureIDEApplication } from '../components/LegendPureIDEApplication';
import { PureIDEPluginManager } from './PureIDEPluginManager';
import { getRootElement } from '@finos/legend-art';
import type { PureIDEConfigData } from './PureIDEConfig';
import { PureIDEConfig } from './PureIDEConfig';
import { DSLDiagram_GraphPreset } from '@finos/legend-extension-dsl-diagram';

export const setupLegendPureIDEUILibrary = async (): Promise<void> => {
  configureReactHotkeys({
    // By default, `react-hotkeys` will avoid capturing keys from input tags like <input>, <textarea>, <select>
    // We want to listen to hotkey from every where in the app so we disable that
    // See https://github.com/greena13/react-hotkeys#ignoring-events
    ignoreTags: [],
    ignoreEventsCondition: (event) =>
      // This is a hack to solve an issue in react-hotkeys.
      // Sometimes it thinks that a letter is still pressed.
      // It happens when you type shift+letter and it registers
      // the keydown event of the letter as upper case, but it
      // registers the keyup event as lower case. Since there's
      // no keyup event for the upper case letter it thinks
      // that it's still pressed
      // See https://github.com/greena13/react-hotkeys/issues/294
      !event.altKey && !event.ctrlKey && !event.metaKey && event.shiftKey,
  });
};

export class LegendPureIDE extends LegendApplication {
  declare config: PureIDEConfig;
  declare pluginManager: PureIDEPluginManager;

  static create(): LegendPureIDE {
    const application = new LegendPureIDE(PureIDEPluginManager.create());
    application.withBasePresets([new DSLDiagram_GraphPreset()]);
    return application;
  }

  async configureApplication(
    configData: PureIDEConfigData,
    versionData: LegendApplicationVersionData,
    baseUrl: string,
  ): Promise<LegendApplicationConfig> {
    return new PureIDEConfig(configData, versionData, baseUrl);
  }

  async loadApplication(): Promise<void> {
    // Setup React application libraries
    await setupLegendApplicationUILibrary(this.pluginManager, this.log);
    await setupLegendPureIDEUILibrary();

    // Render React application
    ReactDOM.render(
      <BrowserRouter basename={this.baseUrl}>
        <WebApplicationNavigatorProvider>
          <LegendPureIDEApplication
            config={this.config}
            pluginManager={this.pluginManager}
            log={this.log}
          />
        </WebApplicationNavigatorProvider>
      </BrowserRouter>,
      getRootElement(),
    );
  }
}
