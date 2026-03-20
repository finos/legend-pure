# Legend Grammar Reference

This page is the **index and combined example** for the grammar sections
defined in this repository — `###Mapping`, `###Relational`, `###Diagram`, and
the three inline DSLs (Graph, Path, TDS) — alongside `###Pure` in Legend Pure
source files.

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
| `Otherwise` — embedded with join fallback | [§10](mapping-grammar-reference.md#10-otherwise-embedded-with-join-fallback) |
| `Inline` — reuse an existing class mapping | [§11](mapping-grammar-reference.md#11-inline-reuse-an-existing-class-mapping) |
| Mapping Set IDs — full reference | [§12](mapping-grammar-reference.md#12-mapping-set-ids-the-full-reference) |
| Filter on a class mapping | [§13](mapping-grammar-reference.md#13-filter-on-a-class-mapping) |
| Local properties (`+`) | [§14](mapping-grammar-reference.md#14-local-properties) |

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

## 3. Quick-reference — `###Diagram`

**Parser class:** `DiagramParser` (`legend-pure-dsl-diagram`)

`###Diagram` is a **top-level section** (like `###Pure` and `###Relational`).
Each diagram is named, carries an optional canvas size, and contains four kinds
of views.

### 3.1 Diagram structure

```pure
###Diagram
Diagram my::package::MyDiagram(width=1200.0, height=800.0)
{
    TypeView          <id>(...)
    AssociationView   <id>(...)
    PropertyView      <id>(...)
    GeneralizationView <id>(...)
}
```

The `(width=…, height=…)` geometry clause is **optional**; omitting it
defaults to `0.0 × 0.0`.  Either dimension may come first:
`(height=800.0, width=1200.0)` is also valid.

### 3.2 TypeView

Renders a class box on the canvas.

```pure
TypeView MyClass_1
(
    type                        = my::package::MyClass,
    stereotypesVisible          = true,
    attributesVisible           = true,
    attributeStereotypesVisible = true,
    attributeTypesVisible       = true,
    color                       = #FFFFCC,
    lineWidth                   = 1.0,
    position                    = (100.0, 200.0),
    width                       = 250.0,
    height                      = 60.0
)
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `type` | qualified name | **yes** | The class being visualised |
| `stereotypesVisible` | `Boolean` | **yes** | Show stereotype tag |
| `attributesVisible` | `Boolean` | **yes** | Show attributes compartment |
| `attributeStereotypesVisible` | `Boolean` | **yes** | Show attribute stereotypes |
| `attributeTypesVisible` | `Boolean` | **yes** | Show attribute types |
| `color` | `#RRGGBB` | **yes** | Background fill colour |
| `lineWidth` | `Float` | **yes** | Border line width |
| `position` | `(x, y)` | **yes** | Top-left corner coordinates |
| `width` | `Float` | **yes** | Box width |
| `height` | `Float` | **yes** | Box height |

### 3.3 AssociationView

Renders an association line between two TypeViews.

```pure
AssociationView MyAssoc_1
(
    association          = my::package::MyAssociation,
    stereotypesVisible   = true,
    nameVisible          = true,
    color                = #000000,
    lineWidth            = 1.0,
    lineStyle            = SIMPLE,
    points               = [(100.0, 130.0), (300.0, 130.0)],
    label                = '',
    source               = MyClass_1,
    target               = MyClass_2,
    sourcePropertyPosition = (105.0, 120.0),
    sourceMultiplicityPosition = (105.0, 140.0),
    targetPropertyPosition = (290.0, 120.0),
    targetMultiplicityPosition = (290.0, 140.0)
)
```

`lineStyle` is one of `SIMPLE` or `RIGHT_ANGLE`.

### 3.4 PropertyView

Renders a single property as a directed line.

```pure
PropertyView MyProp_1
(
    property        = my::package::MyClass.myProperty,
    stereotypesVisible = true,
    nameVisible     = false,
    color           = #000000,
    lineWidth       = 1.0,
    lineStyle       = SIMPLE,
    points          = [(100.0, 130.0), (300.0, 130.0)],
    label           = 'Employment',
    source          = MyClass_1,
    target          = MyClass_2,
    propertyPosition    = (105.0, 125.0),
    multiplicityPosition = (105.0, 135.0)
)
```

### 3.5 GeneralizationView

Renders an inheritance arrow between two TypeViews.

```pure
GeneralizationView MyGen_1
(
    color     = #000000,
    lineWidth = 1.0,
    lineStyle = SIMPLE,
    points    = [(132.5, 77.0), (155.2, 77.0)],
    label     = '',
    source    = SubClass_1,
    target    = SuperClass_2
)
```

---

## 4. Inline DSL — Graph fetch (`#{ }#`)

**DSL class:** `GraphDSL` (`legend-pure-dsl-graph`)  
**Trigger:** `#{` … `}#` — used **inside a Pure expression**, not as a section header.

The Graph DSL lets you declare a `RootGraphFetchTree` that selects which
properties (and sub-properties) to fetch from a model object.

### 4.1 Basic graph

```pure
#{
    MyClass {
        property1,
        property2
    }
}#
```

### 4.2 Nested sub-trees

```pure
#{
    Firm {
        employees {
            address
        }
    }
}#
```

### 4.3 Aliases

Prefix any property with a string literal and `:` to give it an alias:

```pure
#{
    Product {
        'displayName' : name,
        synonyms {
            value
        }
    }
}#
```

### 4.4 Qualified properties with parameters

Pass literal, enum, variable, or collection arguments inside `( )`:

```pure
#{
    Product {
        synonymsByType(ProductSynonymType.CUSIP) {
            value
        }
    }
}#
```

Supported parameter kinds:

| Syntax | Example |
|--------|---------|
| Scalar literal | `'hello'`, `42`, `3.14`, `true`, `%2024-01-01` |
| Enum reference | `MyEnum.VALUE` |
| Variable | `$myVar` |
| Collection | `[1, 2, 3]` |
| `%latest` | `%latest` |

### 4.5 Subtype narrowing

Use `->subType(@SubClass)` to narrow a property's type:

```pure
#{
    Firm {
        address->subType(@HeadquartersAddress)
    }
}#
```

---

## 5. Inline DSL — Path (`#/ /#`)

**DSL class:** `NavigationPath` (`legend-pure-dsl-path`)  
**Trigger:** `#/` … `#` — used **inside a Pure expression**.

A Path expresses a chain of property navigations starting from a root type.
The result type is `meta::pure::metamodel::path::Path<U, V|m>`.

### 5.1 Simple path

```pure
#/MyClass/property1#
```

### 5.2 Multi-hop path

```pure
#/MyClass/property1/property2#
```

### 5.3 Generic root type

```pure
#/Firm<Any>/employees/address#
```

### 5.4 Qualified property with parameters

```pure
#/Product/synonymsByType(ProductSynonymType.CUSIP)/value#
```

Supported parameter kinds are the same as for the Graph DSL (see §4.4 above).

### 5.5 Named path

Append `!<name>` after the last segment to give the path an identifier:

```pure
#/Product/synonymsByType(ProductSynonymType.CUSIP)/value!cusipValue#
```

---

## 6. Inline DSL — TDS (`#TDS … #`)

**DSL class:** `TDSExtension` (`legend-pure-dsl-tds`)  
**Trigger:** `#TDS` … `#` — used **inside a Pure expression**.

The TDS DSL embeds a comma-separated tabular dataset (similar to a CSV)
directly in Pure source code.  The result type is
`meta::pure::metamodel::relation::TDS<T>` where `T` is a `RelationType`.

### 6.1 Basic TDS — column names inferred from data

```pure
#TDS
  name, age, score
  Alice, 30, 9.5
  Bob,   25, 8.0
#
```

Column types are **inferred automatically** from the data values.

### 6.2 TDS with explicit column types

```pure
#TDS
  name:String, age:Integer, score:Float
  Alice, 30, 9.5
  Bob,   25, 8.0
#
```

### 6.3 TDS with explicit types and multiplicities

```pure
#TDS
  name:String[1], age:Integer[0..1], score:Float[1]
  Alice, 30, 9.5
  Bob,   , 8.0
#
```

Supported multiplicities are `[1]` (mandatory) and `[0..1]` (optional).  A
missing value in an `[0..1]` column is represented by an empty CSV field or
the literal `null`.

### 6.4 Quoted column names

Column names that contain spaces or other special characters must be quoted:

```pure
#TDS
  'first name':String, 'last name':String
  Alice, Smith
  Bob,   Jones
#
```

### 6.5 Precise primitive types (parameterised)

```pure
import meta::pure::precisePrimitives::*;
#TDS
  value:Float[1], amount:Numeric(10,4)[0..1]
  1.0, 12.3456
  2.0,
#
```

### 6.6 Using a TDS in a function

```pure
import meta::pure::metamodel::relation::*;

function myData():TDS<(name:String, age:Integer)>[1]
{
    #TDS
      name:String, age:Integer
      Alice, 30
      Bob,   25
    #
}
```

---

## 7. Putting it together — a complete example

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

## 8. Grammar sections not in this repository

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
