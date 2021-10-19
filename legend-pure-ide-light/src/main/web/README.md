# Pure IDE

This is the code for the web application of the Pure IDE (Light) we bapplication. Notice that if you just need to boot the IDE, you can do so by doing a `mvn install` on the root project and start the Pure IDE with `Dropwizard`. Hence, starting the IDE here is mainly for development purpose.

## Getting started

```bash
  yarn install
  yarn setup
  yarn start
```

> Note that we need to match the backend address/port from `ideLightConfig.json` with what we have in `packages/legend-pure-ide-deployment/dev/config.json` (generated from `packages/legend-pure-ide-deployment/scripts/setup.js`).
