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

### Overview

An **association mapping** maps both directions of a Pure `Association` to a
relational join in a single declaration. It is written as a top-level element
inside a `Mapping`, using the **association name** (not a class name) followed
by `: Relational { AssociationMapping ( ... ) }`.

```pure
###Pure
import my::model::*;

Association my::model::PersonFirm      // bidirectional relationship
{
    firm      : Firm[1];
    employees : Person[*];
}

###Mapping
import my::model::*;

Mapping my::mappings::PersonFirmMapping
(
    Person[per1] : Relational
    {
        scope([myDb]PersonTable)
        (
            personId : person_id,
            name     : person_name
        )
    }

    Firm[fir1] : Relational
    {
        scope([myDb]FirmTable)
        (
            firmId : firm_id,
            name   : firm_name
        )
    }

    PersonFirm : Relational              // ← association name, not a class name
    {
        AssociationMapping
        (
            employees [fir1, per1] : [myDb]@PersonFirmJoin,   // Firm → Person
            firm      [per1, fir1] : [myDb]@PersonFirmJoin    // Person → Firm
        )
    }
)
```

### Syntax reference

```
<AssociationName> : Relational
{
    AssociationMapping
    (
        <propertyName> [<sourceSetId>, <targetSetId>] : [<Database>]@<JoinName>,
        <propertyName> [<sourceSetId>, <targetSetId>] : [<Database>]@<JoinName>
    )
}
```

| Part | Description |
|------|-------------|
| `<AssociationName>` | The fully-qualified (or imported) name of the `Association` defined in `###Pure`. |
| `AssociationMapping ( ... )` | Required keyword block — the `: Relational { }` wrapper alone does not declare a class mapping; the `AssociationMapping` keyword identifies this as an association mapping. |
| `<propertyName>` | One of the two property names declared on the `Association`. Both must be listed. |
| `[<sourceSetId>, <targetSetId>]` | Optional when each class has exactly one mapping. Required when either class is mapped more than once (see [§12](#12-mapping-set-ids--the-full-reference)). |
| `[<Database>]@<JoinName>` | The relational join to traverse. Both directions typically use the same join; the engine walks it in the appropriate direction for each property. |

### The `[sourceId, targetId]` qualifier

Each property entry can carry an optional `[sourceId, targetId]` qualifier that
tells the engine which pair of class mappings this entry connects:

```pure
employees [fir1, per1] : [myDb]@PersonFirmJoin
//         ↑      ↑
//         source  target
//         (Firm)  (Person)
```

- `sourceId` — the set ID of the class mapping for the **owning side** (the
  class that carries this property).
- `targetId` — the set ID of the class mapping for the **return type** of this
  property.

**When `[sourceId, targetId]` can be omitted:** if every class involved in the
association has exactly one class mapping within scope (including included
mappings), the compiler assigns the default set IDs automatically and uses them
to wire up the association.

**How the default set ID is calculated:** take the fully-qualified class name and
replace every `::` separator with `_`:

| Class definition | Default set ID |
|------------------|----------------|
| `Class Person` (root package) | `Person` |
| `Class other::Person` | `other_Person` |
| `Class my::model::Person` | `my_model_Person` |

This is a **compile-time derivation from the class's location in the package
tree** — `import` statements have no effect on it. If you write
`import my::model::*` and refer to the class as just `Person`, its default set ID
is still `my_model_Person`.

```pure
// Class other::Person and other::Firm — each mapped exactly once.
// Compiler derives sourceId "other_Firm" and targetId "other_Person"
// for employees, and the reverse for firm.
PersonFirm : Relational
{
    AssociationMapping
    (
        employees : [myDb]@PersonFirmJoin,   // equivalent to employees [other_Firm, other_Person]
        firm      : [myDb]@PersonFirmJoin    // equivalent to firm      [other_Person, other_Firm]
    )
}
```

**When `[sourceId, targetId]` is required:** any time the default set ID (the
FQN-with-`_` derivation) does not match an actual class mapping ID in scope.
This happens in two situations:

**Case 1 — you gave your class mapping a custom ID.**
Even with a single mapping per class, an explicit `[myId]` means the default
derived ID no longer matches. If you write `Person[per1]`, the default would
be `other_Person`, which does not exist. The qualifier is required:

```pure
Mapping my::M
(
    Firm  [fir1] : Relational { scope([myDb]FirmTable)   ( legalName : legal_name ) }
    Person[per1] : Relational { scope([myDb]PersonTable) ( name : name ) }

    // WRONG — compiler derives IDs "other_Firm" and "other_Person", neither exists
    PersonFirm : Relational
    {
        AssociationMapping
        (
            employees : [myDb]@PersonFirmJoin,   // compile error
            firm      : [myDb]@PersonFirmJoin    // compile error
        )
    }

    // CORRECT — reference the actual custom IDs
    PersonFirm : Relational
    {
        AssociationMapping
        (
            employees [fir1, per1] : [myDb]@PersonFirmJoin,
            firm      [per1, fir1] : [myDb]@PersonFirmJoin
        )
    }
)
```

**Case 2 — the same class is mapped more than once.**
Each class mapping gets its own custom ID; the association mapping needs one
entry per (source, target) pair that must be navigable. A common scenario is
mapping the same domain class against two separate regional databases:

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonFirmMapping
(
    // Firm is mapped once globally
    Firm[fir1] : Relational
    {
        scope([GlobalDb]FirmTable) ( legalName : legal_name )
    }

    // Person is mapped separately per region
    Person[per_emea] : Relational
    {
        scope([EmeaDb]PersonTable) ( name : name )
    }
    Person[per_americas] : Relational
    {
        scope([AmericasDb]PersonTable) ( name : name )
    }

    // One entry per (source, target) pair that must be navigable.
    // Four entries total: two directions × two Person mappings.
    PersonFirm : Relational
    {
        AssociationMapping
        (
            employees [fir1, per_emea]         : [EmeaDb]@PersonFirmJoin,
            firm      [per_emea, fir1]          : [EmeaDb]@PersonFirmJoin,
            employees [fir1, per_americas]      : [AmericasDb]@PersonFirmJoin,
            firm      [per_americas, fir1]      : [AmericasDb]@PersonFirmJoin
        )
    }
)
```

The exact compiler error when a set ID cannot be found is:
*"Unable to find source class mapping (id:other_Person) for property 'employees'
in Association mapping 'PersonFirm'. Make sure that you have specified a valid
Class mapping id as the source id and target id, using the syntax
`property[sourceId, targetId]: ...`."*

### Association mapping vs. inline join in a class mapping

Both approaches can navigate a join, but they serve different purposes:

| | **Inline join** (property in class mapping) | **`AssociationMapping`** |
|---|---|---|
| Domain model | Works whether or not an `Association` exists | Requires a named `Association` in `###Pure` |
| Directionality | Maps **one direction** only | Maps **both directions** in one block |
| Where it lives | Inside the class mapping that owns the property | Top-level element beside the class mappings |
| Symmetry | You manually repeat the join expression in each class mapping | One block keeps both directions consistent |
| Conflict | Cannot be combined with `AssociationMapping` for the same property | Cannot be combined with an inline mapping of the same property |

**Use an inline join** when:
- The relationship is modelled as a plain property on the class (no `Association`
  declaration), **or**
- You only need to navigate one direction of a relationship, **or**
- You want the join expression co-located with the other column mappings for that
  class.

```pure
// Inline join — only maps Person.firm; the reverse Firm.employees is not mapped
Person : Relational
{
    scope([myDb]PersonTable)
    (
        name : name,
        firm : [myDb]@PersonFirmJoin   // inline join for one direction
    )
}
```

**Use `AssociationMapping`** when:
- The domain model declares an explicit bidirectional `Association`, **and**
- Both directions (`employees` on `Firm` and `firm` on `Person`) must be
  navigable in queries, **or**
- You want to separate relationship-mapping concerns from primitive-property
  mapping (especially useful when composing mappings via `include`).

### Compiler constraints

1. **A property cannot be mapped twice.** If `firm` is already mapped inline in
   the `Person` class mapping, listing `firm` in an `AssociationMapping` block
   causes a compile error:
   *"Property 'firm' is mapped twice, once in Association mapping 'PersonFirm'
   and once in Class mapping 'Person'. Only one mapping is allowed."*

2. **The target set ID must not be an embedded mapping.** Embedded class mappings
   (see [§9](#9-embedded-mapping)) may only appear as the **source**, not the
   target, of an association mapping property entry:
   *"Invalid target class mapping for property 'X' in Association mapping 'Y'.
   Target 'Z' is an embedded class mapping, embedded mappings are only allowed
   to be the source in an Association Mapping."*

3. **Both property names must be listed.** The association has exactly two
   properties; the mapping block must contain an entry for each.

### Composing association mappings with `include`

The class mappings for both sides of the association do not need to live in the
same `Mapping` element as the `AssociationMapping`. Use `include` to bring in
the class mappings, then declare the association mapping in the including
mapping:

```pure
###Mapping
Mapping my::mappings::PersonMapping
(
    Person[per1] : Relational
    {
        name : [myDb]PersonTable.name
    }
)

###Mapping
Mapping my::mappings::FirmMapping
(
    Firm[fir1] : Relational
    {
        legalName : [myDb]FirmTable.legal_name
    }
)

###Mapping
Mapping my::mappings::FullMapping
(
    include my::mappings::PersonMapping   // per1 is now visible
    include my::mappings::FirmMapping     // fir1 is now visible

    PersonFirm : Relational
    {
        AssociationMapping
        (
            employees [fir1, per1] : [myDb]@PersonFirmJoin,
            firm      [per1, fir1] : [myDb]@PersonFirmJoin
        )
    }
)
```

The `[sourceId, targetId]` qualifiers are required here because the class
mappings come from included mappings and the compiler must unambiguously resolve
which set implementations are being connected.

> **Note:** for cross-store or model-to-model associations, use an `XStore`
> association mapping instead of `Relational AssociationMapping` — see
> [§7 XStore](#7-xstore-cross-store-mapping).

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
(or an inline expression) that returns a `Relation<Any>[1]`. The compiler infers
the row type from the source's last expression and validates each property mapping
against it.

**Metamodel type:** `meta::pure::mapping::relation::RelationFunctionInstanceSetImplementation`
**Parser:** `RelationMappingParser` (`legend-pure-m2-dsl-mapping-grammar`)

### 15.1 Basic syntax

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~func my::personRelation__Relation_1_   // zero-argument function returning Relation
        firstName : FIRSTNAME,                  // property : COLUMN_NAME (bare-column form)
        age       : $src.AGE                    // property : Pure expression over $src
    }
)
```

- The keyword is `Relation` (not `Relational` — that is the database-table mapping).
- The class mapping starts with **either** `~func` **or** `~src` (see §15.2 and
  §15.3). Both forms are mutually exclusive; supplying both is a grammar error:
  *"expected: one of {'~func', '~src'} found: ..."* — the same error surfaces
  when neither is supplied.
- The source (whichever form) **must** take zero arguments and return
  `Relation<Any>[1]` (or a concrete `Relation<(COL:Type, ...)>[1]`). Non-Relation
  returns are rejected at validation with:
  *"Relation mapping function should return a Relation! Found a &lt;Type&gt; instead."*
- Each property RHS is either a **bare column name** (§15.4) or a **Pure
  expression over the row variable `$src`** (§15.5). Both forms compile to the
  same underlying lambda shape (`valueFn: LambdaFunction<{Nil[1]->Any[*]}>[1]`),
  so bare-column is simply parser sugar for `$src.<column>`.

### 15.2 `~func` — reference a named Pure function

`~func` accepts a **reference to a named Pure function** defined in a `###Pure`
section — either as a function descriptor (`package::name__ReturnType_mult_`) or
as a qualified name (`package::name`):

```pure
###Pure
import meta::pure::metamodel::relation::*;

function my::personRelation(): Relation<(FIRSTNAME:String[1], AGE:Integer[1])>[1]
{
    #SQL { SELECT FIRSTNAME, AGE FROM #I{my::ingestDataset::PEOPLE}# }#
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

The referenced function's body can contain any expression that returns a
`Relation` — a `#SQL { ... }` block (defined in `legend-engine`), a call to a
`native function` registered by a store extension, or a chain of relation
operations (`->filter`, `->extend`, `->join`, ...):

```pure
###Pure
import meta::pure::metamodel::relation::*;

// native function declared elsewhere (e.g. in a store extension module)
native function my::store::rawPersonTable(): Relation<(FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1])>[1];

// concrete wrapper — this is what ~func references
function my::personRelation(): Relation<(FIRSTNAME:String[1], AGE:Integer[1])>[1]
{
    my::store::rawPersonTable()
}
```

> **Testing tip:** wrapping your data-shaping logic in a named function lets you
> call it directly (`my::personRelation()`) in a Pure unit test and assert on the
> returned rows independently of the mapping — separately testable from the
> property binding.

### 15.3 `~src` — inline zero-arg expression

`~src` accepts an **inline zero-arg Pure expression** that evaluates to a
`Relation<T>[1]`, avoiding the need for a top-level named function when the
data-shaping logic is a single line:

```pure
###Mapping
import my::model::*;

Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~src #SQL { SELECT FIRSTNAME, AGE FROM #I{my::people}# }#
        firstName : FIRSTNAME,
        age       : AGE
    }
)
```

or pointing at a store accessor:

```pure
###Mapping
Mapping my::mappings::PersonMapping
(
    *Person[person]: Relation
    {
        ~src my::store::rawPersonTable()
        firstName : FIRSTNAME
    }
)
```

Notes:

- The graph builder wraps the expression in a synthetic zero-arg lambda
  (`{| <expr> }`) so post-processing, validation, and unbind treat both forms
  identically. The **metamodel has a single `relationFunction` slot** regardless
  of which surface was used.
- Writing `~src` with a lambda literal body (`~src {| <expr> }`) is rejected —
  the wrapping produces a function whose last expression is a `LambdaFunction`,
  not a `Relation`. Use `~src <expr>` for inline sources; use `~func` when you
  already have a named function.
- A non-Relation return type (e.g. `~src 1`) is rejected with the same
  validation error as `~func`.
- `~src` bodies frequently reference the relational store via the schema-aware
  accessor `#>{db.schema.table}#` — see §15.7.

### 15.4 Property RHS — bare column name

When the column name matches the relation's column set, the compiler infers the
value type from the relation's row type and validates it against the property
type:

```pure
Person[person]: Relation
{
    ~func my::personRelation__Relation_1_
    firstName : FIRSTNAME,   // must exist as a column in the Relation
    age       : AGE
}
```

- Bare column names are lowered at parse time to `{| $src.<col> }`, so §15.5
  applies to the resulting lambda body.
- Quoted column names — for columns whose name contains spaces or special
  characters — use single quotes: `firstName: 'FIRST NAME'`. Both unquoted and
  quoted forms are supported.

### 15.5 Property RHS — Pure expression over `$src`

The RHS of a property mapping can also be **any Pure expression over an
implicit row variable `$src`**. `$src` is bound to the row type of the relation
(the `T` in `Relation<T>[1]` on the source's last expression), so its
properties are the columns of the row.

```pure
*Person[person]: Relation
{
    ~func my::personRelation__Relation_1_
    firstName : $src.FIRSTNAME,                                        // explicit column accessor
    fullName  : $src.FIRSTNAME + ' ' + $src.LASTNAME,                  // concat
    ageBucket : if($src.AGE > 65, |'senior', |'other'),                // conditional
    aliases   : $src.LEGALNAME->split(','),                            // to-many result
    country   : EnumerationMapping CountryEnum : $src.COUNTRY_CODE     // transformer over expression
}
```

- The bare-column form (§15.4) is exactly equivalent to `$src.<col>` — writing
  `firstName: FIRSTNAME` and `firstName: $src.FIRSTNAME` produces the same
  compiled graph.
- Quoted columns work in the expression form too: `$src.'FIRST NAME'`.
- Any function from the Pure standard library is fair game inside the
  expression, provided the result's generic type is compatible with the
  property (see §15.6).
- **Enumeration transformers** (`EnumerationMapping <name> : <expr>`) sit on top
  of the expression result — the transformer bridges the expression's source
  type to the property's enum type, so the direct subtype check between the two
  is skipped for that entry.

### 15.6 Compile-time validations

| Condition | Error |
|-----------|-------|
| Column name not found in the Relation | `The system can't find the column FOO in the Relation (...)` |
| Value type not a subtype of the property type | `Mismatching property and relation expression types. Property '<name>' is of type '<propertyType>', but the expression mapped to it is of type '<exprType>'.` |
| Property multiplicity does not subsume the expression's multiplicity | `Multiplicity Error: The property 'X' has a multiplicity range of [1] when the given expression has a multiplicity range of [*]` |
| Source is not a `Relation<...>` | `Relation mapping function should return a Relation! Found a <Type> instead.` |
| Source function has parameters | Rejected at parse / validation time |
| Both `~func` and `~src` supplied | Rejected at parse time |

Two rules that applied in earlier releases have been **relaxed**:

- Properties are no longer restricted to primitive types. Any property type is
  accepted as long as the expression's result generic type is a subtype of it.
  This lifts the earlier "primitive-only" restriction — a `String` value fed to
  an `Address`-typed property now fails only on the type-subtype check, not on
  a categorical primitive check.
- Property multiplicity is no longer restricted to `[1]` / `[0..1]`. Any
  multiplicity is accepted as long as it **subsumes** the expression's result
  multiplicity. Concretely: `[1..*]` accepts `[1]` / `[1..*]` results but not
  `[*]` (lower bounds differ); `[*]` accepts anything.

### 15.7 Schema-qualified relation store accessors

`~src` (and any other `Relation`-producing expression) frequently references
tables inside a database `Schema`. The relation store accessor
`#>{db.schema.table}#` accepts a three-segment path in addition to the earlier
two-segment `#>{db.table}#` (which continues to resolve against the implicit
default schema):

```pure
###Pure
function my::people(): Relation<Any>[1]
{
    #>{my::mainDb.HR.PersonTable}#->filter(r| $r.active)
}
###Relational
Database my::mainDb
(
    Schema HR ( Table PersonTable(id INT, active BIT) )
)
```

- Schema lookup traverses the database and its transitively `include`d
  databases.
- Errors are surfaced explicitly:
  - Unknown schema: `The schema 'X' can't be found in the database 'db'`.
  - Unknown table in schema: `The table 'X' can't be found in the schema 'S' in the database 'db'`.
  - Wrong number of segments: `RelationStoreAccessor path must be of the form 'db.table' or 'db.schema.table' (got N segments)`.

### 15.8 Local properties (`+`)

Local properties work in `Relation` mappings exactly as in `Relational` mappings
— see [§16 Local properties](#16-local-properties-) for the full description.
The RHS follows §15.4 / §15.5: bare column name **or** Pure expression over
`$src`.

```pure
Firm[f1] : Relation
{
    ~func my::firmRelation__Relation_1_
    +id        : String[1] : ID,                          // bare-column form
    +greeting  : String[1] : 'Hi, ' + $src.LEGALNAME,     // expression form
    legalName  : LEGALNAME
}
```

### 15.9 Embedded mappings under `Relation`

Embedded class mappings (see [§9](#9-embedded-mapping)) work identically inside
a `Relation` class mapping. The embedded block inherits the parent's
`relationFunction` and its sub-property RHS follows the same bare-column /
`$src`-expression rules:

```pure
*Person[person]: Relation
{
    ~src my::personFunctionTyped()
    firstName : FIRSTNAME,
    address                                // embedded — inherits parent's relation function
    (
        city : $src.CITY
    )
}
```

### 15.10 Difference from `Relational` mapping

| | `Relational` | `Relation` |
|---|---|---|
| Data source | `###Relational` database table / view / join | Any Pure expression returning `Relation<Any>[1]` |
| Source declaration | `~mainTable [db]Table` | `~func <fnRef>` **or** `~src <expression>` |
| Store dependency | Requires a `Database` definition | None — store-agnostic (the source expression *may* reference a store, but the mapping itself does not require one) |
| Property mapping | Column expression `[db]Table.column`, join traversal, operations | Bare column name **or** any Pure expression over `$src` |
| Non-primitive properties | Supported via joins | Supported when a Pure expression evaluates to a compatible non-primitive |

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
- `<columnExpression>` is a full column reference for `Relational` mappings, or
  either a bare column name or a Pure expression over `$src` for `Relation`
  mappings (see below).
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

The column expression is either a bare column name from the relation returned
by `~func` / `~src`, or a Pure expression over `$src` (see [§15.5](#155-property-rhs--pure-expression-over-src)):

```pure
###Mapping
Mapping my::FirmMapping
(
    Firm[f1] : Relation
    {
        ~func my::firmRelation__Relation_1_
        +id       : String[1] : ID,                              // bare-column form
        +greeting : String[1] : 'Hi, ' + $src.LEGALNAME,         // expression form
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

