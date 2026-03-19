# Legend Grammar Reference

This page is the **index and combined example** for the `###Mapping` and
`###Relational` grammar sections — the two most commonly encountered sections
alongside `###Pure` in Legend Pure source files.

Detailed per-section references live in their own pages:

| Grammar | Reference |
|---------|-----------|
| `###Mapping` | [Mapping Grammar Reference](mapping-grammar-reference.md) |
| `###Relational` | [Relational Grammar Reference](relational-grammar-reference.md) |
| `###Pure` | [Pure Language Reference](pure-language-reference.md) |

For how the `###` section system works, see
[Section 0 of the Pure Language Reference](pure-language-reference.md#0-the-grammar-section-system).

---

## 1. Quick-reference — `###Mapping`

**Parser class:** `MappingParser` (`legend-pure-dsl-mapping`)

| Topic | Link |
|-------|------|
| Mapping structure | [§1](mapping-grammar-reference.md#1-mapping-structure) |
| Pure (model-to-model) class mapping | [§2](mapping-grammar-reference.md#2-pure-model-to-model-class-mapping) |
| Relational class mapping, `scope`, joins | [§3](mapping-grammar-reference.md#3-relational-class-mapping) |
| Enumeration mapping | [§4](mapping-grammar-reference.md#4-enumeration-mapping) |
| Association mapping | [§5](mapping-grammar-reference.md#5-association-mapping) |
| Including another mapping | [§6](mapping-grammar-reference.md#6-including-another-mapping) |
| XStore (cross-store) mapping | [§7](mapping-grammar-reference.md#7-xstore-cross-store-mapping) |
| Aggregation-aware mapping | [§8](mapping-grammar-reference.md#8-aggregation-aware-mapping) |
| Embedded mapping | [§9](mapping-grammar-reference.md#9-embedded-mapping) |
| `Otherwise` — embedded with join fallback | [§10](mapping-grammar-reference.md#10-otherwise--embedded-with-join-fallback) |
| `Inline` — reuse an existing class mapping | [§11](mapping-grammar-reference.md#11-inline--reuse-an-existing-class-mapping) |
| Mapping Set IDs — full reference | [§12](mapping-grammar-reference.md#12-mapping-set-ids--the-full-reference) |
| Filter on a class mapping | [§13](mapping-grammar-reference.md#13-filter-on-a-class-mapping) |
| Local properties (`+`) | [§14](mapping-grammar-reference.md#14-local-properties-) |

---

## 2. Quick-reference — `###Relational`

**Parser class:** `RelationalParser` (`legend-pure-store-relational`)

| Topic | Link |
|-------|------|
| Database structure | [§1](relational-grammar-reference.md#1-database-structure) |
| Tables and columns | [§2](relational-grammar-reference.md#2-tables-and-columns) |
| Joins (equi, multi-column, functions, self-joins) | [§3](relational-grammar-reference.md#3-joins) |
| Including another database | [§4](relational-grammar-reference.md#4-including-another-database) |
| Views | [§5](relational-grammar-reference.md#5-views) |
| Filters | [§6](relational-grammar-reference.md#6-filters) |

---

## 3. Putting it together — a complete example

The following shows a minimal but complete `###Pure` + `###Relational` +
`###Mapping` file:

```pure
###Pure
import my::*;

Class my::Person
{
    personId   : Integer[1];
    firstName  : String[1];
    lastName   : String[1];
    firmId     : Integer[1];
}

Class my::Firm
{
    firmId   : Integer[1];
    legalName: String[1];
}

Association my::PersonFirm
{
    employees : Person[*];
    firm      : Firm[1];
}

Enum my::EmploymentStatus
{
    ACTIVE,
    INACTIVE
}

###Relational
Database my::PersonFirmDb
(
    Table PersonTable
    (
        person_id  INT         PRIMARY KEY,
        first_name VARCHAR(100),
        last_name  VARCHAR(100),
        firm_id    INT,
        status     VARCHAR(10)
    )

    Table FirmTable
    (
        firm_id    INT         PRIMARY KEY,
        legal_name VARCHAR(200)
    )

    Join PersonFirm (PersonTable.firm_id = FirmTable.firm_id)
)

###Mapping
import my::*;

Mapping my::PersonFirmMapping
(
    EmploymentStatus : EnumerationMapping StatusMapping
    {
        ACTIVE   : 'A',
        INACTIVE : 'I'
    }

    Person : Relational
    {
        scope([PersonFirmDb]PersonTable)
        (
            personId  : person_id,
            firstName : first_name,
            lastName  : last_name,
            firmId    : firm_id
        )
    }

    Firm : Relational
    {
        scope([PersonFirmDb]FirmTable)
        (
            firmId    : firm_id,
            legalName : legal_name
        )
    }

    PersonFirm : Relational
    {
        AssociationMapping
        (
            employees : [PersonFirmDb]@PersonFirm,
            firm      : [PersonFirmDb]@PersonFirm
        )
    }
)
```

---

## 4. Grammar sections not in this repository

The following `###` section headers are defined in `legend-engine` (which
consumes `legend-pure` as a dependency) and are not part of this repository:

| Section | Purpose |
|---------|---------|
| `###Connection` | Database connection configuration (JDBC URL, auth, pool settings) |
| `###Runtime` | Binds a mapping to a connection for execution |
| `###Service` | Defines a queryable Legend service endpoint |
| `###DataSpace` | Packages a model, mapping, and runtime for consumption |
| `###FlatData` | Schema for delimited/fixed-width flat file stores |

---

*See also: [Pure Language Reference](pure-language-reference.md) ·
[Compiler Pipeline](../architecture/compiler-pipeline.md) ·
[Contributor Workflow — Adding a new DSL](../guides/contributor-workflow.md)*
