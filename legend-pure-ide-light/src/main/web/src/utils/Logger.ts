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

export enum LOG_EVENT {
  UNSUPPORTED_ENTITY_DETECTED = 'UNSUPPORTED_ENTITY_DETECTED',
  ILLEGAL_APPLICATION_STATE_OCCURRED = 'ILLEGAL_APPLICATION_STATE_OCCURRED',
  DEVELOPMENT_MODE = '[DEVELOPMENT]',
  NONE = 'NONE',
}

export const SKIP_LOGGING_INFO = Symbol('SKIP_LOGGING_INFO');

// We use numeric enum here for because we want to do comparison
// In order to retrieve the name of the enum we can do reverse mapping, for example: LogLevel[LogLevel.INFO] -> INFO
// https://www.typescriptlang.org/docs/handbook/enums.html#reverse-mappings
export enum LOG_LEVEL {
  DEBUG = 1,
  INFO,
  WARN,
  ERROR,
  SILENT,
}

export class Logger {
  level: LOG_LEVEL = LOG_LEVEL.DEBUG;
  previousLevelBeforeMuting: LOG_LEVEL = LOG_LEVEL.DEBUG;

  setLogLevel = (level: LOG_LEVEL): void => { this.level = level };
  /**
   * Mute logging, if a level is specified, mute all event of lower severity than that level
   */
  mute = (level?: LOG_LEVEL): void => {
    this.previousLevelBeforeMuting = this.level;
    this.level = level ?? LOG_LEVEL.SILENT;
  };
  unmute = (): void => { this.level = this.previousLevelBeforeMuting };
  runInSilent = (fn: Function, level?: LOG_LEVEL): void => {
    this.mute(level);
    fn();
    this.unmute();
  };

  /* eslint-disable no-console */
  debug = (eventType: LOG_EVENT, ...info: unknown[]): void =>
    this.level > LOG_LEVEL.DEBUG ? undefined : console.debug((eventType !== LOG_EVENT.NONE ? info.filter(i => i !== SKIP_LOGGING_INFO).length ? `${eventType}:` : eventType : ''), ...info.filter(i => i !== SKIP_LOGGING_INFO));

  info = (eventType: LOG_EVENT, ...info: unknown[]): void =>
    this.level > LOG_LEVEL.INFO ? undefined : console.info((eventType !== LOG_EVENT.NONE ? info.filter(i => i !== SKIP_LOGGING_INFO).length ? `${eventType}:` : eventType : ''), ...info.filter(i => i !== SKIP_LOGGING_INFO));

  warn = (eventType: LOG_EVENT, ...info: unknown[]): void =>
    this.level > LOG_LEVEL.WARN ? undefined : console.warn((eventType !== LOG_EVENT.NONE ? info.filter(i => i !== SKIP_LOGGING_INFO).length ? `${eventType}:` : eventType : ''), ...info.filter(i => i !== SKIP_LOGGING_INFO));

  error = (eventType: LOG_EVENT, ...info: unknown[]): void =>
    this.level > LOG_LEVEL.ERROR ? undefined : console.error((eventType !== LOG_EVENT.NONE ? info.filter(i => i !== SKIP_LOGGING_INFO).length ? `${eventType}:` : eventType : ''), ...info.filter(i => i !== SKIP_LOGGING_INFO));
  /* eslint-enable no-console */
}
