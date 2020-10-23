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
import ReactDOM from 'react-dom';
import 'Style/CssLoader';
import { configure as hotkeysConfigure } from 'react-hotkeys';
import { editor as monacoEditorAPI, languages as monacoLanguagesAPI } from 'monaco-editor';
import { configuration, language, theme } from 'Utilities/LanguageUtil';
import { EDITOR_THEME, EDITOR_LANGUAGE } from 'Stores/EditorConfig';
import { App } from 'Components/App';

// Register Pure as a language in `monaco-editor`
monacoEditorAPI.defineTheme(EDITOR_THEME.NATIVE, theme);
monacoLanguagesAPI.register({ id: EDITOR_LANGUAGE.PURE });
monacoLanguagesAPI.setLanguageConfiguration(EDITOR_LANGUAGE.PURE, configuration);
monacoLanguagesAPI.setMonarchTokensProvider(EDITOR_LANGUAGE.PURE, language);

hotkeysConfigure({
  // By default, `react-hotkeys` will avoid capturing keys from input tags like <input>, <textarea>, <select>
  // We want to listen to hotkey from every where in the app so we disable that
  // See https://github.com/greena13/react-hotkeys#ignoring-events
  ignoreTags: [],
});

const root = ((): Element => {
  let rootEl = document.getElementsByTagName('root').length ? document.getElementsByTagName('root')[0] : undefined;
  if (!rootEl) {
    rootEl = document.createElement('root');
    document.body.appendChild(rootEl);
  }
  return rootEl;
})();

ReactDOM.render(<App />, root);
