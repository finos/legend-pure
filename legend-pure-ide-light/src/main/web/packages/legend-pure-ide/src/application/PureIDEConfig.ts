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

import {
  assertNonNullable,
  guaranteeNonEmptyString,
} from '@finos/legend-shared';
import type {
  LegendApplicationConfigurationData,
  LegendApplicationVersionData,
} from '@finos/legend-application';
import { LegendApplicationConfig } from '@finos/legend-application';

export interface PureIDEConfigData extends LegendApplicationConfigurationData {
  appName: string;
  env: string;
  pure: { url: string };
}

export class PureIDEConfig extends LegendApplicationConfig {
  readonly pureUrl: string;

  constructor(
    configData: PureIDEConfigData,
    versionData: LegendApplicationVersionData,
    baseUrl: string,
  ) {
    super(configData, versionData, baseUrl);

    assertNonNullable(
      configData.pure,
      `Application configuration failure: 'pure' field is missing`,
    );
    this.pureUrl = guaranteeNonEmptyString(
      configData.pure.url,
      `Application configuration failure: 'pure.url' field is missing or empty`,
    );
  }
}
