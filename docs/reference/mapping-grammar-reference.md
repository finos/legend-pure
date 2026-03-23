# `###Mapping` Grammar Reference

**Parser class:** `MappingParser` (`legend-pure-dsl-mapping`)
**ANTLR4 grammar:** `MappingParser.g4` / `MappingLexer.g4`

A `Mapping` connects domain model classes (defined in `###Pure`) to a data source.
It answers the question: *"given a class, where does its data come from and how are
the properties populated?"*

For the physical database schema that mappings refer to, see the
[Relational Grammar Reference](relational-grammar-reference.md).
For the `###Pure` grammar (classes, functions, enumerations, etc.) see the
[Pure Language Reference](pure-language-reference.md).
For a complete worked example combining all three sections, see the
[Legend Grammar Reference — Complete Example](legend-grammar-reference.md#7-putting-it-together--a-complete-example).

---

## 1. Mapping structure

```pure
###Mapping
import my::model::*;

Mapping my::mappings::MyMapping
(
    // one or more class mappings
)
```

A single `Mapping` element can contain multiple class mappings, association mappings,
enumeration mappings, and include directives. All elements are scoped within the
parentheses.

---

## 2. Pure (model-to-model) class mapping

Maps one Pure class to another. Used when the source data is already a Pure object
graph rather than a database.

```pure
###Mapping
import my::model::*;

Mapping my::mappings::ProductMapping
(
    TargetProduct : Pure
    {
        ~src SourceProduct          // declare the source class
        id    : $src.id,
        name  : $src.productName,   // rename a property
        price : $src.unitPrice * $src.quantity   // expression mapping
    }
)
```

- `~src <ClassName>` declares the source type. `$src` refers to the current
  source instance inside property expressions.
- Property expressions are Pure expressions — any function from the standard
  library can be used.
- The mapping name defaults to the class name; an explicit ID can be given:
  `TargetProduct [myId] : Pure { ... }`

---

## 3. Relational class mapping

Maps a Pure class to a relational database table or view defined in a
`###Relational` section.

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    Person : Relational
    {
        ~mainTable [myDb]PersonTable
        firstName : [myDb]PersonTable.first_name,
        lastName  : [myDb]PersonTable.last_name,
        age       : [myDb]PersonTable.age
    }
)
```

- `~mainTable [<Database>]<Table>` declares the primary table. Required when the
  mapping touches more than one table.
- Column references use the syntax `[<Database>]<Table>.<column>`.

### Using `scope` for conciser column references

When most properties map to the same table, `scope` removes the need to repeat
the table qualifier:

```pure
Person : Relational
{
    scope([myDb]PersonTable)
    (
        firstName : first_name,
        lastName  : last_name,
        age       : age
    )
}
```

### Navigating joins

Use the `@<JoinName>` syntax to traverse a join defined in the `###Relational`
section:

```pure
Person : Relational
{
    scope([myDb]PersonTable)
    (
        firstName : first_name,
        firm      : [myDb]@PersonFirmJoin   // navigate the join to FirmTable
    )
}
```

### Chaining multiple joins

You can chain multiple joins in sequence using `>`, optionally specifying the join
type (`INNER`, `LEFT OUTER`, `RIGHT OUTER`) at each step. This is used when a
property is reached by traversing more than one join:

```pure
###Relational
Database myDb
(
    Table table1 (name VARCHAR(200) PRIMARY KEY, t2name VARCHAR(200))
    Table table2 (name VARCHAR(200) PRIMARY KEY, t3name VARCHAR(200))
    Table table3 (name VARCHAR(200) PRIMARY KEY)

    Join T1_T2 (table1.t2name = table2.name)
    Join T2_T3 (table2.t3name = table3.name)
)

###Mapping
Mapping my::M
(
    Table1 : Relational
    {
        name   : [myDb]table1.name,
        table3 : [myDb]@T1_T2 > (INNER) [myDb]@T2_T3   // two-hop join chain
    }
    Table3 : Relational
    {
        name : [myDb]table3.name
    }
)
```

The join-type qualifier (`INNER`, `LEFT OUTER`, `RIGHT OUTER`) is placed in
parentheses between each `>` step. If omitted, the join type defaults to the
type declared on the join in the `###Relational` section. Chains of any length
are supported: `[db]@J1 > (INNER) [db]@J2 > (LEFT OUTER) [db]@J3`.

---

## 4. Enumeration mapping

Maps the string/integer values stored in a data source to a Pure `Enum`.

```pure
###Mapping
import my::model::*;

Mapping my::mappings::StatusMapping
(
    // Map integers to enum values
    TradeStatus : EnumerationMapping IntStatusMapping
    {
        PENDING   : 0,
        CONFIRMED : 1,
        SETTLED   : 2,
        CANCELLED : 3
    }

    // Map strings to enum values
    TradeStatus : EnumerationMapping StringStatusMapping
    {
        PENDING   : 'P',
        CONFIRMED : 'C',
        SETTLED   : 'S',
        CANCELLED : 'X'
    }
)
```

### Many source values mapping to one enum value

Multiple source values can be collapsed to a single enum value by listing them
in `[...]`. All source values in the mapping must be of the same type (all
strings, all integers, or all values from the same source enum).

```pure
###Mapping
Mapping my::mappings::StatusMapping
(
    // Several legacy codes all map to the same canonical status value
    TradeStatus : EnumerationMapping LegacyStringMapping
    {
        PENDING   : ['P', 'PEND', 'PENDING'],   // three strings → one enum value
        CONFIRMED : ['C', 'CONF'],
        SETTLED   : 'S',                         // single value — no brackets needed
        CANCELLED : ['X', 'CANC', 'CANCEL']
    }

    // Integer variant: multiple codes → one value
    TradeStatus : EnumerationMapping LegacyIntMapping
    {
        PENDING   : [0, 10, 20],
        CONFIRMED : 1,
        SETTLED   : 2,
        CANCELLED : [3, 4, 5]
    }
)
```

**Constraint:** all source values within a single enumeration mapping must share
the same type (string, integer, or a single source enum). Mixing types (e.g.
`['P', 1]`) causes a compile error:
*"Only one source Type is allowed for an Enumeration Mapping"*.

Reference an enumeration mapping from a class mapping:

```pure
Trade : Relational
{
    scope([myDb]TradeTable)
    (
        status : EnumerationMapping LegacyStringMapping : status_code
    )
}
```

---

## 5. Association mapping

Maps an `Association` (bidirectional relationship) to its relational join.

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonFirmMapping
(
    Person : Relational
    {
        scope([myDb]PersonTable)
        (
            personId : person_id,
            name     : person_name
        )
    }

    Firm : Relational
    {
        scope([myDb]FirmTable)
        (
            firmId : firm_id,
            name   : firm_name
        )
    }

    PersonFirm : Relational          // the Association name
    {
        AssociationMapping
        (
            person : [myDb]@PersonFirmJoin,
            firm   : [myDb]@PersonFirmJoin
        )
    }
)
```

---

## 6. Including another mapping

A mapping can extend another mapping, inheriting all its class mappings:

```pure
###Mapping

Mapping my::mappings::ExtendedMapping
(
    include my::mappings::BaseMapping   // inherit all mappings from BaseMapping

    // override or add class mappings here
    ExtraClass : Pure
    {
        ~src ExtraSource
        id : $src.id
    }
)
```

### Set IDs from included mappings

All set IDs defined in the included mapping are visible in the including
mapping. You can reference them directly in `Inline`, `Otherwise`, `extends`,
and XStore expressions without any re-declaration:

```pure
###Mapping
Mapping my::mappings::BaseMapping
(
    Person[per1] : Relational { ... }   // set ID 'per1' is defined here
    Firm  [fir1] : Relational { ... }   // set ID 'fir1' is defined here
)

###Mapping
Mapping my::mappings::ExtendedMapping
(
    include my::mappings::BaseMapping   // per1 and fir1 are now visible here

    // Reference the included set IDs directly in an XStore association mapping
    PersonFirm : XStore
    {
        employees [fir1, per1] : $this.id == $that.firmId,
        firm      [per1, fir1] : $this.firmId == $that.id
    }
)
```

There is **no syntax for renaming or aliasing an inherited set ID**. If you
need a different ID, define a fresh class mapping in the including mapping
(potentially extending the inherited one via `extends [inheritedId]`).

### Store substitution on include

When including a relational mapping, you can swap one `Database` for another
using the `[OriginalDb -> SubstituteDb]` syntax on the `include` directive. The
substitute database must include (or be a superset of) the original:

```pure
###Mapping
Mapping my::mappings::FullMapping
(
    include my::mappings::PersonMapping [PersonDB -> FullDB]
    include my::mappings::FirmMapping   [FirmDB   -> FullDB]

    // Association mapping now uses FullDB joins (both class mappings re-point there)
    Employment : Relational
    {
        AssociationMapping
        (
            employer   [person, firm] : [FullDB]@PersonFirmJoin,
            employees  [firm, person] : [FullDB]@PersonFirmJoin
        )
    }
)
```

- Syntax: `include <MappingPath> [<OriginalDb> -> <SubstituteDb>]`
- The original database must actually be used by the included mapping; the
  compiler validates this and reports an error if it is not.
- Multiple substitutions can be listed for a single include by comma-separating
  them: `[Db1 -> FullDb, Db2 -> FullDb]`.
- Store substitution only applies to relational mappings; Pure / XStore mappings
  do not use stores and need no substitution.

---

## 7. XStore (cross-store) mapping

The **`XStore`** Association mapping allows you to define how two mapped classes relate to each other using **pure model
property expressions** ("Pure Model Joins"), with no store joins or store columns involved. It is specifically designed
for composing mappings where both sides are already mapped (e.g. via `Pure` / model-to-model mappings).

It works at the **association level** (not the class level), and for each property of the association it takes
a **cross-expression** lambda that receives `$this` (the source instance) and `$that` (the target instance),
returning a `Boolean` to express the join condition using model properties only.


```pure
###Mapping

Mapping my::mappings::CrossStoreMapping
(
    PersonFirm : XStore
    {
        person[$this.firmId == $that.firmId] : Person,
        firm  [$this.personId == $that.personId] : Firm
    }
)
```

### Key distinction from relational association mappings

| Feature | Relational `AssociationMapping` | `XStore` |
|---|---------------------------------|---|
| Join condition | SQL join columns from Store     | Model property expression (`$this.prop == $that.prop`) |
| Store dependency | Requires a relational store     | Store-agnostic (model only) |
| Composition | Less flexible across stores     | Explicitly designed for cross-store / model-only composition |
| Lambda params | N/A                             | `$this` and `$that` bound to source/target class types |


### Composing mappings with include + XStore

XStore works seamlessly with **mapping includes**, letting you separate class mappings from association mappings:

```pure
###Mapping
Mapping ModelMapping
(
   Firm[f1] : Pure  { ... }
   Person[e] : Pure { ... }
)

###Mapping
Mapping FirmMapping
(
   include ModelMapping  // re-use the class mappings
   
   Firm_Person : XStore
   {
      firm[e, f1]      : $this.firmId == $that.id,
      employees[f1, e] : $this.id == $that.firmId
   }
)
```
---

## 8. Aggregation-aware mapping

Allows the query engine to transparently redirect queries to pre-aggregated
summary tables when the query's groupBy columns and aggregate functions match.

```pure
###Mapping

Mapping my::mappings::SalesMapping
(
    Sales [a] : AggregationAware
    {
        Views :
        [
            (
                ~modelOperation :
                {
                    ~canAggregate    true,
                    ~groupByFunctions ( $this.salesDate ),
                    ~aggregateValues  ( ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() ) )
                },
                ~aggregateMapping : Relational
                {
                    scope([salesDb]sales_by_date)
                    (
                        salesDate : sales_date,
                        revenue   : net_revenue
                    )
                }
            )
        ],
        ~mainMapping : Relational
        {
            scope([salesDb]sales_base)
            (
                salesDate : sales_date,
                revenue   : revenue
            )
        }
    }
)
```

The query engine uses the `~mainMapping` by default. When the query matches the
conditions of an aggregated view (`~canAggregate`, `~groupByFunctions`,
`~aggregateValues`), it switches to that view automatically.

---

## 9. Embedded mapping

An **embedded mapping** maps a complex property inline, without creating a separate
top-level class mapping. The property's sub-properties are mapped inside `(...)` as
if they were columns of the same table row.

```pure
###Mapping
Mapping my::PersonMapping
(
    Person[alias1] : Relational
    {
        name : [db]employeeFirmDenormTable.name,

        firm                                         // complex property
        (
            ~primaryKey ([db]employeeFirmDenormTable.legalName)
            legalName : [db]employeeFirmDenormTable.legalName
        )
    }
)
```

- The `~primaryKey` directive inside the embedded block declares which column(s)
  uniquely identify the embedded object. Required when the owning table contains
  multiple rows for the same parent.
- Embedded mappings are useful when the data is denormalised (all columns in one
  table) but the domain model has nested objects.

---

## 10. `Otherwise` — embedded with join fallback

`Otherwise` extends an embedded mapping: when the embedded columns cannot supply
a value (e.g. they are null), the engine falls back to a separate class mapping
reached via a join.

```pure
###Mapping
Mapping my::PersonMapping
(
    Firm[firm1] : Relational
    {
        legalName : [db]FirmInfoTable.name
    }

    Person[alias1] : Relational
    {
        name : [db]employeeFirmDenormTable.name,

        firm
        (
            ~primaryKey ([db]employeeFirmDenormTable.legalName)
            legalName : [db]employeeFirmDenormTable.legalName
        ) Otherwise ( [firm1]:[db]@PersonFirmJoin )
        //              ↑ target mapping ID  ↑ join to navigate when embedded data is absent
    }
)
```

- `Otherwise ( [<targetMappingId>]:<database>@<Join> )` — the target mapping ID
  must refer to an existing class mapping in the same `Mapping` element (here
  `firm1`).
- The fallback can also point to a direct column instead of a join:
  `Otherwise ( [firm1]:[db]employeeFirmDenormTable.legalName )`.
- Use `Otherwise` when the table usually has the denormalised data but occasionally
  must resolve via a normalised join.

---

## 11. `Inline` — reuse an existing class mapping

`Inline` maps a property by pointing to an already-defined class mapping ID, rather
than writing the column mappings again. It is the read-only counterpart of embedding.

```pure
###Mapping
Mapping my::PersonMapping
(
    Firm[firm1] : Relational         // the mapping to reuse, ID = firm1
    {
        legalName : [db]FirmTable.legal_name
    }

    Person[p1] : Relational
    {
        name : [db]PersonTable.name,
        firm () Inline[firm1]        // reuse the firm1 mapping for the firm property
    }
)
```

- `Inline[<setId>]` refers to any class mapping ID visible in the same `Mapping`.
- The target mapping must implement a class that is the same as or a subtype of the
  property's declared return type. The compiler validates this at compile time.
- Use `Inline` to avoid duplicating column expressions when the same class is mapped
  from multiple parent contexts.

---

## 12. Mapping Set IDs — the full reference

A **set ID** (also called a **class mapping ID** or **set implementation ID**) is
the name that uniquely identifies one class mapping within a `Mapping` element. It
is used wherever one part of a mapping needs to refer to another by name — in
`Inline`, `Otherwise`, `extends`, XStore `[sourceId, targetId]` expressions, and
property-level join disambiguation.

### Default ID — what the compiler generates when you omit `[...]`

When no explicit ID is given, `SetImplementationProcessor` assigns the default:

```pure
<fully-qualified-class-name with :: replaced by _>
```

For example, `my::model::Person` mapped without an ID gets the default set ID
`my_model_Person`. In practice, for a class imported via `import my::model::*` and
referred to as just `Person`, the default becomes simply `Person`.

```pure
###Mapping
Mapping my::M
(
    // No [id] — compiler assigns default set ID "Person"
    Person : Relational
    {
        name : [db]PersonTable.name
    }
)
```

**Important:** if you map the same class twice without explicit IDs the compiler
will assign the same default ID to both, causing a conflict. Always use explicit IDs
when mapping a class more than once.

### Explicit ID — `ClassName[myId] : ...`

Supply a plain identifier in square brackets immediately after the class name:

```pure
###Mapping
Mapping my::M
(
    Person[p_eu] : Relational         // set ID = "p_eu"
    {
        scope([EuDb]PersonTable)
        (
            name : name
        )
    }

    Person[p_us] : Relational         // set ID = "p_us" — second mapping of same class
    {
        scope([UsDb]PersonTable)
        (
            name : name
        )
    }
)
```

IDs must be unique within a `Mapping` element (including across included mappings).

### Root mapping marker — `*ClassName[id]`

The `*` prefix designates a class mapping as the **root** (default) mapping for
that class. When a query targets a class without specifying which mapping to use,
the engine selects the root mapping.

```pure
###Mapping
Mapping my::M
(
    *Person[person1] : Relational     // ← root mapping for Person
    {
        otherInfo : [db]employeeTable.other
    }

    Person[alias1] extends [person1] : Relational   // non-root, extends the root
    {
        name : [db]employeeTable.name
    }
)
```

A mapping with no `*` and no other root mapping in scope is still valid; the `*` is
only needed when multiple mappings exist for the same class and one must be the
default.

### `extends [superSetId]` — inheriting another class mapping

A class mapping can inherit all property mappings from another mapping of the same
class. Only the additional or overriding properties need to be listed in the child
mapping:

```pure
###Mapping
Mapping my::M
(
    *Person[person1] : Relational
    {
        otherInfo : [db]employeeTable.other   // mapped in the parent
    }

    Person[alias1] extends [person1] : Relational
    {
        name : [db]employeeTable.name         // additional property in the child
        // otherInfo is inherited from person1 — no need to repeat it
    }
)
```

- The `superSetId` in `extends [superSetId]` must be the set ID of an existing
  class mapping for the **same class** within the same or an included `Mapping`.
- The child mapping adds or overrides individual property mappings; it inherits
  all others from the parent.

### Property-level `[sourceId, targetId]` — disambiguating association joins

In an association mapping property expression, the optional `[sourceId, targetId]`
qualifier tells the engine **which pair of class mappings** this join connects. This
is required whenever a class has more than one mapping and the association mapping
would otherwise be ambiguous:

```pure
###Mapping
Mapping my::M
(
    Firm[fir1] : Relational { ... }
    Person[per1] : Relational { ... }

    FirmPerson : Relational
    {
        AssociationMapping
        (
            employees [fir1, per1] : [db]@FirmPersonJoin,
            firm      [per1, fir1] : [db]@FirmPersonJoin
        )
    }
)
```

- `employees [fir1, per1]` — navigating from the `fir1` (Firm) mapping to the
  `per1` (Person) mapping via the join.
- `firm [per1, fir1]` — navigating in the opposite direction.
- If only one mapping exists for each class, `[sourceId, targetId]` can be omitted.
  The compiler error message if disambiguation is needed is:
  *"Make sure that you have specified a valid Class mapping id as the source id and
  target id, using the syntax `property[sourceId, targetId]: ...`"*

### Summary of all places set IDs appear

| Context | Syntax | Role |
|---------|--------|------|
| Class mapping declaration | `ClassName[myId] : ...` | Assigns the ID |
| Root marker | `*ClassName[myId] : ...` | Assigns ID and marks as default |
| Inline property mapping | `prop () Inline[myId]` | References the mapping to reuse |
| Otherwise fallback | `Otherwise ( [myId]:[db]@Join )` | Target mapping when embedded data absent |
| Extends | `ClassName[childId] extends [parentId] : ...` | Inherits property mappings |
| Association property | `prop [sourceId, targetId] : [db]@Join` | Disambiguates which class mappings are connected |

---

## 13. Mapping classes with an inheritance relationship

When your domain model uses class inheritance, you have two options for the
subclass: **reuse** the parent class mapping (via `extends`), or **override** it
with a fresh mapping sourced from a different table.

### Option A — subclass reuses the parent class mapping (`extends`)

Use `extends [parentSetId]` on the subclass mapping when both the parent and the
subclass share the same table (or when the subclass adds no new mapped properties of
its own). The subclass mapping inherits all property mappings from the parent and can
optionally add new ones.

```pure
###Pure
Class my::Vehicle
{
    vehicleId   : Integer[1];
    vehicleName : String[1];
}

Class my::RoadVehicle extends Vehicle
{
    axleCount : Integer[1];
}

###Relational
Database my::VehicleDb
(
    Table VehicleTable (vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20), axleCount INT)
)

###Mapping
import my::*;

Mapping my::VehicleMapping
(
    // Parent class mapping — defines set ID 'test_Vehicle'
    Vehicle[test_Vehicle] : Relational
    {
        vehicleId   : [VehicleDb]VehicleTable.vehicleId,
        vehicleName : [VehicleDb]VehicleTable.vehicleName
    }

    // Subclass mapping — extends the parent, adds axleCount
    RoadVehicle extends [test_Vehicle] : Relational
    {
        axleCount : [VehicleDb]VehicleTable.axleCount
        // vehicleId and vehicleName are inherited from test_Vehicle
    }
)
```

- `RoadVehicle extends [test_Vehicle]` inherits all property mappings from the
  `test_Vehicle` class mapping. Only the extra `axleCount` column needs to be
  declared.
- The `extends [superSetId]` annotation requires that the referenced set ID maps
  **the same class or a superclass** of the extending mapping's class. The
  compiler enforces this with: *"Invalid extends mapping. Class ... extends more
  than one class. Extends mappings are only currently only allowed with single
  inheritance relationships"*.
- The subclass mapping can have its own explicit set ID:
  `RoadVehicle[rv1] extends [test_Vehicle] : Relational { ... }`.

### Option B — subclass sourced from a different table (fresh mapping)

When the subclass is stored in a separate table (joined back to the parent table),
define a completely independent mapping for the subclass — no `extends` needed.
Use `includes` to compose mappings that live in separate files:

```pure
###Pure
Class my::Animal
{
    id   : Integer[1];
    name : String[1];
}

Class my::Dog extends Animal
{
    breed : String[1];
}

###Relational
Database my::AnimalDb
(
    Table AnimalTable (id INT PRIMARY KEY, name VARCHAR(50))
    Table DogTable    (id INT PRIMARY KEY, animalId INT, breed VARCHAR(50))
    Join AnimalDog (DogTable.animalId = AnimalTable.id)
)

###Mapping
import my::*;

Mapping my::AnimalMapping
(
    // Base class mapping
    Animal[animal] : Relational
    {
        id   : [AnimalDb]AnimalTable.id,
        name : [AnimalDb]AnimalTable.name
    }

    // Subclass has its own dedicated table — completely independent mapping
    // Properties from the parent table are reached via join
    Dog[dog] : Relational
    {
        ~mainTable [AnimalDb]DogTable
        id    : [AnimalDb]@AnimalDog > [AnimalDb]AnimalTable.id,
        name  : [AnimalDb]@AnimalDog > [AnimalDb]AnimalTable.name,
        breed : [AnimalDb]DogTable.breed
    }
)
```

In this pattern:
- Each class has its own independent set ID (`animal`, `dog`).
- The subclass mapping navigates the join to read inherited columns from the parent
  table.
- There is no `extends` link between the two mappings; the mapping engine resolves
  the correct mapping for each type at query time.

### Composing with `include` — subclass in a separate mapping file

If the parent class mapping lives in a different `Mapping` element (e.g. a shared
base), include it and reference its set ID in `extends`:

```pure
###Mapping
Mapping my::BaseMapping
(
    Vehicle[test_Vehicle] : Relational
    {
        vehicleId   : [VehicleDb]VehicleTable.vehicleId,
        vehicleName : [VehicleDb]VehicleTable.vehicleName
    }
)

###Mapping
Mapping my::ExtendedMapping
(
    include my::BaseMapping   // test_Vehicle set ID is now visible here

    RoadVehicle extends [test_Vehicle] : Relational
    {
        axleCount : [VehicleDb]VehicleTable.axleCount
    }
)
```

---

## 14. Filter on a class mapping

A class mapping can declare a `~filter` that restricts which rows are returned for
that class, independently of any filter defined on the database view.

```pure
###Mapping
Mapping my::OrgMapping
(
    Org : Relational
    {
        ~filter [myDB]@OrgTableOtherTable[myDB]myFilter2
        //       ↑ join path to reach the table the filter is declared on
        //                                  ↑ filter name from ###Relational
        name     : [myDB]orgTable.name,
        parent   : [myDB]@OrgOrgParent,
        children : [myDB]@OrgParentOrg
    }
)
```

The filter syntax is: `~filter <database>(<joinPath>)? <filterName>`

- `<database>` identifies which `Database` element contains the filter.
- The optional join path (`@Join1 > @Join2`) navigates to the table the filter
  is defined on, when it is not the main table of the mapping.
- The filter name must match a `Filter` declared in the `###Relational` section.

---

## 15. Relation class mapping

Maps a Pure class to a **`Relation`** — the columnar, typed relation type introduced
in `legend-pure-m3-core` (`meta::pure::metamodel::relation::Relation`). Instead of
referencing a database table directly, you point at a zero-argument Pure function
that returns a `Relation<Any>[1]`. The compiler resolves the column types from the
relation's type parameter and validates each property mapping against them.

**Metamodel type:** `meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation`
**Parser:** `RelationMappingParser` (`legend-pure-m2-dsl-mapping-grammar`)

### Basic syntax

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~func my::personRelation__Relation_1_   // zero-argument function returning Relation
        firstName : FIRSTNAME,                  // property : COLUMN_NAME
        age       : AGE
    }
)
```

- The keyword is `Relation` (not `Relational` — that is the database-table mapping).
- `~func` is **required** and must be the first line inside the braces. Omitting it
  is a compile error: `expected: '~func'`.
- `~func` accepts **only a reference to a named Pure function** defined in a
  `###Pure` section — either as a function descriptor
  (`package::name__ReturnType_mult_`) or as a qualified name (`package::name`).
- Inline lambdas and `#SQL` expressions cannot be written directly after `~func`.
  This is an implementation constraint, not a fundamental design prohibition: the
  compiler resolves `~func` via an `ImportStub` name lookup and then walks the
  resolved `FunctionDefinition`'s `expressionSequence` to extract the return
  `RelationType` for column validation. Supporting an inline expression would
  require embedding and type-inferring it during the graph-build pass — work that
  has not yet been implemented.
- The referenced function **must** take zero arguments and return `Relation<Any>[1]`
  (or a concrete `Relation<(COL:Type, ...)>[1]`). Functions with parameters are
  rejected at compile time.
- Column names are unquoted identifiers. Quoted names (columns whose names contain
  spaces or special characters) use single quotes: `legalName: 'LEGAL NAME'`.

### Wrapping a `#SQL` expression or store accessor

The standard workaround for using a `#SQL` expression (defined in `legend-engine`)
or any store-specific accessor is to wrap it in a named concrete function and point
`~func` at that wrapper. The function body can contain any expression that returns
a `Relation` — including a `#SQL` block or a call to a `native function`:

```pure
###Pure
import meta::pure::metamodel::relation::*;

// Wrapper function — the body can hold a #SQL expression, a native function
// call, or any other Relation-producing expression
function my::personRelation(): Relation<(FIRSTNAME:String, AGE:Integer)>[1]
{
    #SQL { SELECT FIRSTNAME, AGE FROM #I{my::ingestDataset::PEOPLE }#
}

###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~func my::personRelation__Relation_1_
        firstName : FIRSTNAME,
        age       : AGE
    }
)
```

The same pattern works when the underlying data source is a `native function`
(e.g. a store accessor registered by a runtime extension):

```pure
###Pure
import meta::pure::metamodel::relation::*;

// native function declared elsewhere (e.g. in a store extension module)
native function my::store::rawPersonTable(): Relation<(FIRSTNAME:String, AGE:Integer, FIRMID:Integer)>[1];

// concrete wrapper — this is what ~func references
function my::personRelation(): Relation<(FIRSTNAME:String, AGE:Integer)>[1]
{
    my::store::rawPersonTable()
}

###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~func my::personRelation__Relation_1_
        firstName : FIRSTNAME,
        age       : AGE
    }
)
```

> **Testing tip:** wrapping your data-shaping logic in a concrete named function
> has a practical benefit beyond satisfying the `~func` constraint — you can call
> `my::personRelation()` directly in a Pure unit test and assert on the returned
> rows independently of the mapping. This makes the data-shaping logic separately
> testable from the property binding.

### Column-name-only property mappings

When the column name matches the relation's column set, the compiler infers the
type from the relation's type parameter and validates it against the property type:

```pure
Person[person]: Relation
{
    ~func my::personRelation__Relation_1_
    firstName : FIRSTNAME,   // must exist as a column in the Relation
    age       : AGE
}
```

**Compile-time validations:**

| Condition | Error |
|-----------|-------|
| Column name not found in the Relation | `The system can't find the column FOO in the Relation (...)` |
| Column type does not match property type | `Mismatching property and relation column types. Property type is X, but relation column it is mapped to has type Y.` |
| Property type is non-primitive (e.g. another class) | `Relation mapping is only supported for primitive properties, but the property 'address' has type Address.` |
| Property multiplicity is `[*]` | `Properties in relation mappings can only have multiplicity 1 or 0..1` |

### Local properties (`+`)

Local properties work in `Relation` mappings exactly as in `Relational` mappings —
see [§16 Local properties](#16-local-properties-) for the full description. The
only difference is the column expression: a bare column name rather than a
`[db]Table.column` reference.

### Difference from `Relational` mapping

| | `Relational` | `Relation` |
|---|---|---|
| Data source | `###Relational` database table / view / join | Any Pure function returning `Relation<Any>[1]` |
| Source declaration | `~mainTable [db]Table` | `~func my::package::fn__Relation_1_` |
| Store dependency | Requires a `Database` definition | None — store-agnostic |
| Property mapping | Column expression `[db]Table.column` | Bare column name `COLUMN` |
| Complex expressions | Join traversal, operation expressions | Not supported — column name only |
| Non-primitive properties | Supported via joins | Not supported |

---

## 16. Local properties (`+`)

A **local property** declares a new property directly on the mapping, without adding
it to the underlying Pure domain class. It is only visible within this mapping and
is typically used as a key for cross-store joins.

`localMappingProperty` is defined on the **base `PropertyMapping` metamodel class**
(`meta::pure::mapping::PropertyMapping`), so it is a universal concept. However, the
`+` syntax is only implemented in the parsers for two mapping types: **`Relational`**
and **`Relation`**.

Syntax: `+<propertyName> : <Type>[<multiplicity>] : <columnExpression>`

- The `+` prefix distinguishes a local property from a regular mapped property.
- `<columnExpression>` is a column reference for `Relational` mappings or a bare
  column name for `Relation` mappings (see below).
- Local properties are available via `$this` and `$that` in XStore association
  expressions.
- They do not appear in the domain model and cannot be queried directly by
  consumers of the mapping.

### In a `Relational` mapping

The column expression is a full relational column reference:

```pure
###Mapping
Mapping my::FirmMapping
(
    Firm[f1] : Relational
    {
        +id     : String[1] : [db]FirmTable.id,    // local property — not on Firm class
        legalName : [db]FirmTable.legal_name
    }

    Person[e] : Relational
    {
        +firmId : String[1] : [db]PersonTable.firmId,
        lastName : [db]PersonTable.lastName
    }

    Firm_Person : XStore
    {
        firm      [e,  f1] : $this.firmId == $that.id,
        employees [f1, e]  : $this.id == $that.firmId
    }
)
```

### In a `Relation` mapping

The column expression is a bare column name from the relation returned by `~func`:

```pure
###Mapping
Mapping my::FirmMapping
(
    Firm[f1] : Relation
    {
        ~func my::firmRelation():Relation<Any>[1]
        +id       : String[1]    : ID,             // local property — not on Firm class
        legalName : LEGALNAME
    }

    Person[e] : Relation
    {
        ~func my::personRelation__Relation_1_
        +firmId : Integer[0..1] : FIRMID
    }

    Firm_Person : XStore
    {
        firm      [e,  f1] : $this.firmId == $that.id,
        employees [f1, e]  : $this.id == $that.firmId
    }
)
```

---

*See also: [Relational Grammar Reference](relational-grammar-reference.md) ·
[Pure Language Reference](pure-language-reference.md) ·
[Complete Example](legend-grammar-reference.md#7-putting-it-together--a-complete-example) ·
[Compiler Pipeline](../architecture/compiler-pipeline.md) ·
[Contributor Workflow — Adding a new DSL](../guides/contributor-workflow.md)*

