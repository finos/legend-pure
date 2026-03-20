# Legend Pure

[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://finosfoundation.atlassian.net/wiki/display/FINOS/Incubating)
[![Maven Central](https://img.shields.io/maven-central/v/org.finos.legend.pure/legend-pure-ide-light.svg)](https://central.sonatype.com/namespace/org.finos.legend.pure)
![Build CI](https://github.com/finos/legend-pure/workflows/Build%20CI/badge.svg)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=legend-pure&metric=security_rating&token=69394360757d5e1356312ddfee658a6b205e2c97)](https://sonarcloud.io/dashboard?id=legend-pure)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=legend-pure&metric=bugs&token=69394360757d5e1356312ddfee658a6b205e2c97)](https://sonarcloud.io/dashboard?id=legend-pure)

Underlying language for Legend

## Documentation

Full developer documentation lives in the [`/docs`](docs/README.md) folder:

- **[5-minute overview & key concepts](docs/README.md)**
- **[Getting Started Guide](docs/guides/getting-started.md)** — prerequisites, IDE setup, troubleshooting
- **[Architecture Overview](docs/architecture/overview.md)** — module structure, Legend ecosystem position
- **[Pure Language Reference](docs/reference/pure-language-reference.md)** — syntax, types, standard library
- **[Legend Grammar Reference](docs/reference/legend-grammar-reference.md)** — index and complete example
- **[Mapping Grammar Reference](docs/reference/mapping-grammar-reference.md)** — `###Mapping` grammar in full
- **[Relational Grammar Reference](docs/reference/relational-grammar-reference.md)** — `###Relational` grammar in full
- **[Compiler Pipeline](docs/architecture/compiler-pipeline.md)** — how Pure source becomes Java bytecode

## Development Setup

Requires **JDK 11 or 17** and **Maven 3.6+**.

```bash
git clone https://github.com/finos/legend-pure.git
cd legend-pure
mvn -T 4 install -DskipTests    # fast first build (4 parallel threads)
mvn -T 4 install                # full build with tests
```

See the [Getting Started Guide](docs/guides/getting-started.md) for IDE setup,
profiles, and troubleshooting.

## Roadmap

Visit our [roadmap](https://github.com/finos/legend#roadmap) to know more about the upcoming features.

## Contributing

Visit Legend [Contribution Guide](https://github.com/finos/legend/blob/master/CONTRIBUTING.md) to learn how to contribute to Legend.

## License

Copyright 2020 Goldman Sachs

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
