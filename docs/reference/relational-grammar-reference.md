# `###Relational` Grammar Reference

**Parser class:** `RelationalParser` (`legend-pure-store-relational`)
**ANTLR4 grammar:** `RelationalParser.g4` / `RelationalLexer.g4`

A `###Relational` section defines the **physical database schema** — the tables,
columns, primary keys, joins, views, and filters that the `###Mapping` section
refers to. It does not contain any query logic; it is a structural description only.

For how mappings consume this schema (column references, join navigation, filters),
see the [Mapping Grammar Reference](mapping-grammar-reference.md).
For a complete worked example combining all three sections, see the
[Legend Grammar Reference — Complete Example](legend-grammar-reference.md#7-putting-it-together-a-complete-example).

---

## 1. Database structure

```pure
###Relational
Database my::stores::MyDatabase
(
    // Tables, joins, views, filters go here
)
```

A `Database` element is the top-level container. Its fully-qualified name is used
as the `[DatabaseName]` qualifier in mapping column references.

---

## 2. Tables and columns

```pure
###Relational
Database my::stores::TradeDatabase
(
    Table TradeTable
    (
        trade_id    INT         PRIMARY KEY,
        trade_date  DATE,
        status_code INT,
        amount      FLOAT,
        book_name   VARCHAR(100)
    )

    Table BookTable
    (
        book_id   INT         PRIMARY KEY,
        book_name VARCHAR(100)
    )
)
```

**Supported column types:**

| SQL type | Legend keyword |
|----------|---------------|
| Integer | `INT`, `INTEGER`, `BIGINT`, `SMALLINT`, `TINYINT` |
| Decimal | `FLOAT`, `DOUBLE`, `NUMERIC`, `DECIMAL` |
| String | `VARCHAR(n)`, `CHAR(n)`, `TEXT` |
| Boolean | `BIT`, `BOOLEAN` |
| Date | `DATE` |
| Timestamp | `TIMESTAMP` |
| Binary | `BINARY`, `VARBINARY` |

---

## 3. Joins

Joins are named and declared at the `Database` level. They are referenced by name
from `###Mapping` sections using the `@<JoinName>` syntax.

The condition inside `Join Name(...)` is a **relational operation expression** — the
same expression language used in `Filter`, `View` filters, and view `~groupBy`
conditions. It supports comparison operators, boolean combinators, function calls,
constants, and the special `{target}` alias for self-joins.

### Simple equi-join

```pure
###Relational
Database my::TradeDatabase
(
    Table TradeTable (trade_id INT PRIMARY KEY, book_id INT)
    Table BookTable  (book_id  INT PRIMARY KEY, book_name VARCHAR(100))

    Join TradeBook (TradeTable.book_id = BookTable.book_id)
)
```

### Multi-column join — `and` / `or`

Combine multiple column predicates with `and` and `or` (lowercase keywords):

```pure
###Relational
Database my::TradeDatabase
(
    Table TradeTable (trade_id INT PRIMARY KEY, book_id INT, region VARCHAR(10))
    Table BookTable  (book_id INT PRIMARY KEY, region VARCHAR(10))

    // Both columns must match
    Join TradeBookByIdAndRegion
    (
        TradeTable.book_id = BookTable.book_id
        and TradeTable.region = BookTable.region
    )
)
```

`and` binds more tightly than `or`. Use parentheses to group when mixing both:

```pure
Join TradeBookComplex
(
    (TradeTable.book_id = BookTable.book_id and TradeTable.region = 'LON')
    or
    (TradeTable.book_id = BookTable.book_id and TradeTable.region = 'NYC')
)
```

### Comparison operators

All standard comparison operators are available inside join (and filter) conditions:

| Operator | Meaning | DynaFunction name |
|----------|---------|-------------------|
| `=` | Equal | `equal` |
| `!=` | Not equal | `notEqual` |
| `<>` | Not equal (ANSI) | `notEqualAnsi` |
| `<` | Less than | `lessThan` |
| `<=` | Less than or equal | `lessThanEqual` |
| `>` | Greater than | `greaterThan` |
| `>=` | Greater than or equal | `greaterThanEqual` |
| `is null` | Column is null | `isNull` |
| `is not null` | Column is not null | `isNotNull` |

```pure
// Restrict a join to non-null keys
Join PersonFirmActive
(
    PersonTable.firm_id = FirmTable.firm_id
    and PersonTable.firm_id is not null
)
```

### Functions on columns

Any `VALID_STRING` identifier followed by `(args...)` is treated as a **function
call** (`DynaFunction`). Arguments can be columns, constants, or nested function
calls. This is the primary mechanism for applying SQL functions inside join and
filter expressions.

Commonly used functions:

| Function | Example | Description |
|----------|---------|-------------|
| `concat` | `concat('prefix_', table.col)` | Concatenate a constant and a column |
| `in` | `in(table.col, [1, 2, 3])` | Column value is in a list |
| `substring` | `substring(table.col, 0, 3)` | Extract substring |
| `position` | `position(',', table.col)` | Find character position |
| `upper` / `lower` | `upper(table.col)` | Case conversion |
| `trim` | `trim(table.col)` | Trim whitespace |
| `toString` | `toString(table.col)` | Cast column to string |

**Concatenating a constant and a column value:**

```pure
###Relational
Database my::VehicleDatabase
(
    Table VehicleTable (id INT PRIMARY KEY, vehicleName VARCHAR(100))

    // Join condition using concat() to match a prefixed name
    Join VehiclePrefix
    (
        concat('roadVehicle_', VehicleTable.vehicleName) = SummaryTable.prefixedName
    )
)
```

**`in()` — column value in a list:**

```pure
// Multi-value equi-join with in() function
Join FirmPersonRegion
(
    FirmTable.id = PersonTable.firm_id
    and in(FirmTable.region, ['LON', 'NYC', 'TYO'])
)

// Same pattern with integers
Join FirmPersonActive
(
    FirmTable.id = PersonTable.firm_id
    and in(FirmTable.status_code, [1, 2, 3])
)
```

These are tested directly in `RelationalGraphBuilderTest` — the grammar strings
`firmTable.ID = personTable.FIRMID and in(firmTable.ID, [2,3,4])` and
`firmTable.ID = personTable.FIRMID and in(firmTable.LEGALNAME, ['Google', 'Apple'])`
are verified to parse to the correct `DynaFunction` tree.

**Substring in a join condition:**

```pure
Join PersonNamePrefix
(
    substring(PersonTable.full_name, 0, 3) = LookupTable.code
)
```

### `{target}` — self-referential alias for self-joins

`{target}` is a special alias that refers to the *other side* of a join on the
**same table**. It is required whenever a table joins to itself (e.g. a parent–child
hierarchy), because the same table name would otherwise be ambiguous.

```pure
###Relational
Database my::OrgDatabase
(
    Table OrgTable (id INT PRIMARY KEY, parent_id INT, name VARCHAR(200))

    // Self-join: navigate from a child row to its parent row
    Join OrgToParent (OrgTable.parent_id = {target}.id)

    // Self-join in the other direction
    Join OrgToChildren (OrgTable.id = {target}.parent_id)
)
```

`{target}` always refers to the row on the *right-hand side* of the join, which
the engine aliases separately from the left-hand side when generating SQL.

### Multi-hop join navigation in mappings

Joins declared in `###Relational` are navigated in `###Mapping` using `@`:

```pure
// Single hop
firm : [db]@PersonFirmJoin

// Multi-hop: > chains joins in sequence
firmCity : [db]@PersonFirmJoin > @FirmCityJoin
```

---

## 4. Including another database

A database can include all tables and joins from another database, enabling
schema composition and store substitution:

```pure
###Relational
Database my::stores::ConsolidatedDatabase
(
    include my::stores::PersonDatabase
    include my::stores::FirmDatabase

    // Additional tables and joins specific to this database
    Table AuditTable (audit_id INT PRIMARY KEY, event VARCHAR(200))
    Join PersonAudit (PersonTable.person_id = AuditTable.person_id)
)
```

This is the foundation of the **store substitution** pattern, where a mapping
declared against `PersonDatabase` can be re-used with `ConsolidatedDatabase` at
runtime.

---

## 5. Views

A view is a named query inside the database definition. It can be referenced in
mappings exactly like a table.

```pure
###Relational
Database my::stores::TradeDatabase
(
    Table TradeTable (trade_id INT PRIMARY KEY, status INT, amount FLOAT)

    View ActiveTradeView
    (
        trade_id : TradeTable.trade_id PRIMARY KEY,
        amount   : TradeTable.amount
        ~filter  [TradeTable] status = 1
    )
)
```

---

## 6. Filters

A filter restricts which rows are visible through the database. It is declared
on a table and referenced from a view or mapping.

```pure
###Relational
Database my::stores::TradeDatabase
(
    Table TradeTable (trade_id INT PRIMARY KEY, is_deleted BIT, region VARCHAR(10))

    Filter NonDeletedFilter (TradeTable.is_deleted = 0)
    Filter LondonFilter     (TradeTable.region = 'LON')
)
```

---

*See also: [Mapping Grammar Reference](mapping-grammar-reference.md) ·
[Pure Language Reference](pure-language-reference.md) ·
[Complete Example](legend-grammar-reference.md#7-putting-it-together-a-complete-example) ·
[Compiler Pipeline](../architecture/compiler-pipeline.md) ·
[Contributor Workflow — Adding a new DSL](../guides/contributor-workflow.md)*

