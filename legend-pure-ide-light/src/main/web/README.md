# Pure IDE (Light) UI

This is the code for the web application of the Pure IDE (Light). Notice that if you just need to boot the IDE, you can do so by doing a `mvn install` on the root project and start the Pure IDE with dropwizard. Hence, starting the IDE here is mainly for development purpose.

## Getting started

```bash
  yarn install
  yarn start
```

Note that we derive the backend address/port from `ideLightConfig.json`.

## Developer setups

If you use [VSCode](https://code.visualstudio.com/) you should install their `Prettier` and `ESLint` plugins, then configuallore your workspace settings in `./.vscode/settings.json` like this:

```jsonc
{
  "editor.tabSize": 2,
  "eslint.validate": [
    "javascript",
    "javascriptreact",
    "typescript",
    "typescriptreact"
  ],
  "eslint.options": {
    // NOTE: we disable advanced linting rules during development to make faster incremental build
    // but we can still get the full benefit by enabling it for the IDE
    "configFile": "./.eslintrc-advanced.js"
  },
  "search.exclude": {
    "**/node_modules": true,
    "**/package-lock.json": true
  },
  "[css]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[scss]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[html]": {
    "editor.defaultFormatter": "vscode.html-language-features"
  },
  "[json]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[jsonc]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[javascript]": {
    "editor.defaultFormatter": "vscode.typescript-language-features"
  },
  "[typescript]": {
    "editor.defaultFormatter": "vscode.typescript-language-features"
  },
  "[typescriptreact]": {
    "editor.defaultFormatter": "vscode.typescript-language-features"
  },
  "prettier.singleQuote": true,
  "prettier.trailingComma": "es5",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "javascript.preferences.importModuleSpecifier": "non-relative",
  "typescript.preferences.importModuleSpecifier": "non-relative"
}
```
