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
[Legend Grammar Reference — Complete Example](legend-grammar-reference.md#3-putting-it-together--a-complete-example).

---

## 1. Mapping structure

```pure
###Mapping
import my::model::*;

Mapping my::mappings::MyMapping
(
    // one or more class mappings
)
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

Reference an enumeration mapping from a class mapping:

```pure
Trade : Relational
{
    scope([myDb]TradeTable)
    (
        status : EnumerationMapping IntStatusMapping : status_code
    )
}
```pure

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
```pure

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
```pure

---

## 7. XStore (cross-store) mapping

Maps associations that span two different stores. The property expression can
reference a key from either side:

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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
```pure

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

## 13. Filter on a class mapping

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
```pure

The filter syntax is: `~filter <database>(<joinPath>)? <filterName>`

- `<database>` identifies which `Database` element contains the filter.
- The optional join path (`@Join1 > @Join2`) navigates to the table the filter
  is defined on, when it is not the main table of the mapping.
- The filter name must match a `Filter` declared in the `###Relational` section.

---

## 14. Local properties (`+`)

A **local property** declares a new property directly on the mapping, without adding
it to the underlying Pure domain class. It is only visible within this mapping and
is typically used as a key for cross-store joins.

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
        +firmId : String[1] : [db]PersonTable.firmId,  // local property
        lastName : [db]PersonTable.lastName
    }

    Firm_Person : XStore
    {
        firm      [e,  f1] : $this.firmId == $that.id,
        employees [f1, e]  : $this.id == $that.firmId
    }
)
```pure

Syntax: `+<propertyName> : <Type>[<multiplicity>] : <columnExpression>`

- The `+` prefix distinguishes a local property from a regular mapped property.
- Local properties are available via `$this` and `$that` in XStore association
  expressions.
- They do not appear in the domain model and cannot be queried directly by
  consumers of the mapping.

---

*See also: [Relational Grammar Reference](relational-grammar-reference.md) ·
[Pure Language Reference](pure-language-reference.md) ·
[Complete Example](legend-grammar-reference.md#3-putting-it-together--a-complete-example) ·
[Compiler Pipeline](../architecture/compiler-pipeline.md) ·
[Contributor Workflow — Adding a new DSL](../guides/contributor-workflow.md)*

