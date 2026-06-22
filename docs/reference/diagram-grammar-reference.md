# Diagram Grammar Reference

Diagrams are defined under `###Diagram` and provide visual layout for classes,
associations, and generalisations. They carry no execution semantics — only
layout data used by Legend Studio to render a canvas.

> **⚠️ Two incompatible diagram grammars exist in the Legend stack.**
>
> This document covers the grammar implemented in **`legend-pure`** (the
> compiler and test harness in this repository). The **`legend-engine`**
> project — which powers Legend Studio — ships its own `DiagramParser` that
> completely replaces the one here and uses a **different syntax**. If you are
> writing diagram files for Legend Studio or `legend-engine`, use the
> `legend-engine` grammar instead.
>
> | Feature | `legend-pure` grammar (this doc) | `legend-engine` grammar |
> |---------|----------------------------------|------------------------|
> | Box keyword | `TypeView` | `classView` |
> | Class field | `type=my::Class` | `class: my::Class;` |
> | Size fields | `width=200.0, height=80.0` | `rectangle: (200.0, 80.0);` |
> | Position | `position=(100.0, 100.0)` | `position: (100.0, 100.0);` |
> | Field separator | `,` (comma) | `;` (semicolon) |
> | Block delimiters | `(` … `)` | `{` … `}` |
> | View identifiers | Plain strings (e.g. `TradeView`) | UUIDs (e.g. `4cec85f9-9b66-…`) |
> | Anonymous views | Not supported — all views need an id | `generalizationView { … }` has no id |

## Syntax overview

The grammar uses **`=`** for assignments and **`,`** between fields. Each view
element is wrapped in **parentheses `(...)`**, not curly braces. These are
common sources of confusion when writing diagrams by hand.

```
Diagram my::package::MyDiagram(width=1200.0, height=800.0)
{
    TypeView <id>( field=value, field=value, ... )
    AssociationView <id>( field=value, field=value, ... )
    PropertyView <id>( field=value, field=value, ... )
    GeneralizationView <id>( field=value, field=value, ... )
}
```

Each view element carries a **local identifier** (`<id>`) — a plain string
such as `TradeView` or `Trade_Counterparty_1` — which edge views (`PropertyView`,
`AssociationView`, `GeneralizationView`) reference as `source=` and `target=`.

> **Tip:** `import` is supported. Add `import my::domain::*;` before the
> `###Diagram` section to shorten qualified names throughout the file.

---

## Complete example

```pure
import my::domain::*;

###Diagram
Diagram my::domain::TradeModelDiagram(width=800.0, height=600.0)
{
    TypeView TradeView(
        type=my::domain::Trade,
        stereotypesVisible=true,
        attributesVisible=true,
        attributeStereotypesVisible=true,
        attributeTypesVisible=true,
        color=#FFFFCC,
        lineWidth=1.0,
        position=(100.0, 100.0),
        width=200.0,
        height=80.0)

    TypeView CounterpartyView(
        type=my::domain::Counterparty,
        stereotypesVisible=true,
        attributesVisible=true,
        attributeStereotypesVisible=true,
        attributeTypesVisible=true,
        color=#FFFFCC,
        lineWidth=1.0,
        position=(450.0, 100.0),
        width=200.0,
        height=80.0)

    TypeView ExoticTradeView(
        type=my::domain::ExoticTrade,
        stereotypesVisible=true,
        attributesVisible=true,
        attributeStereotypesVisible=true,
        attributeTypesVisible=true,
        color=#FFFFCC,
        lineWidth=1.0,
        position=(450.0, 300.0),
        width=200.0,
        height=80.0)

    PropertyView Trade_counterparty(
        property=my::domain::Trade.counterparty,
        stereotypesVisible=true,
        nameVisible=true,
        color=#000000,
        lineWidth=1.0,
        lineStyle=SIMPLE,
        points=[(200.0, 140.0), (450.0, 140.0)],
        label='counterparty',
        source=TradeView,
        target=CounterpartyView,
        propertyPosition=(210.0, 130.0),
        multiplicityPosition=(380.0, 130.0))

    AssociationView Trade_product(
        association=my::domain::Trade_Product,
        stereotypesVisible=true,
        nameVisible=false,
        color=#000000,
        lineWidth=1.0,
        lineStyle=SIMPLE,
        points=[(200.0, 160.0), (200.0, 300.0)],
        label='Trade_Product',
        source=TradeView,
        target=CounterpartyView,
        sourcePropertyPosition=(210.0, 200.0),
        sourceMultiplicityPosition=(240.0, 200.0),
        targetPropertyPosition=(210.0, 290.0),
        targetMultiplicityPosition=(240.0, 290.0))

    GeneralizationView ExoticTrade_Trade(
        color=#000000,
        lineWidth=1.0,
        lineStyle=SIMPLE,
        points=[(550.0, 300.0), (200.0, 180.0)],
        label='',
        source=ExoticTradeView,
        target=TradeView)
}
```

---

## Diagram geometry

An optional `(width=W, height=H)` clause on the `Diagram` declaration sets the
canvas size. Both fields must be present together — supplying only one is a
parse error. If the clause is omitted entirely the canvas defaults to
`width=0.0, height=0.0`.

```pure
// With geometry
Diagram my::pkg::D(width=1200.0, height=800.0) { ... }

// Without geometry — valid, defaults to 0.0 x 0.0
Diagram my::pkg::D { ... }

// Either order is accepted
Diagram my::pkg::D(height=800.0, width=1200.0) { ... }
```

---

## TypeView

Renders one class box on the canvas.

```pure
TypeView <id>(
    type=<FullyQualifiedClassName>,
    stereotypesVisible=<boolean>,
    attributesVisible=<boolean>,
    attributeStereotypesVisible=<boolean>,
    attributeTypesVisible=<boolean>,
    color=<#RRGGBB>,
    lineWidth=<float>,
    position=(<x>, <y>),
    width=<float>,
    height=<float>)
```

### TypeView field reference

| Field | Required | Notes |
|-------|----------|-------|
| `type` | **Yes** | Fully-qualified (or imported) class name |
| `stereotypesVisible` | **Yes** | Show stereotype labels on the class box |
| `attributesVisible` | **Yes** | Show the attribute compartment |
| `attributeStereotypesVisible` | **Yes** | Show stereotypes on individual attributes |
| `attributeTypesVisible` | **Yes** | Show type annotations on attributes |
| `color` | **Yes** | Fill colour in `#RRGGBB` hex format, e.g. `#FFFFCC` |
| `lineWidth` | **Yes** | Border thickness; `-1.0` uses the Studio default |
| `position` | **Yes** | `(x, y)` top-left corner of the box on the canvas |
| `width` | **Yes** | Box width in pixels |
| `height` | **Yes** | Box height in pixels |

All ten fields are required. The error message `Missing value for property 'X'
on TypeView <id>` names the first absent field found.

> **Local identifier:** The `<id>` after `TypeView` (e.g. `TradeView`) is
> scoped to the enclosing `Diagram`. It is referenced by edge views via
> `source=` and `target=`. Any valid identifier string works — UUIDs are not
> required. Legend Studio typically generates names like `ClassName_N`
> (e.g. `Trade_1`).

---

## PropertyView

Renders a directed edge for a **property declared directly on a class** (not
via an `Association` element).

```pure
PropertyView <id>(
    property=<OwnerClass>.<propertyName>,
    stereotypesVisible=<boolean>,
    nameVisible=<boolean>,
    color=<#RRGGBB>,
    lineWidth=<float>,
    lineStyle=<SIMPLE|RIGHT_ANGLE>,
    points=[(<x1>, <y1>), (<x2>, <y2>), ...],
    label=<'string'>,
    source=<typeViewId>,
    target=<typeViewId>,
    propertyPosition=(<x>, <y>),
    multiplicityPosition=(<x>, <y>))
```

### PropertyView field reference

| Field | Required | Notes |
|-------|----------|-------|
| `property` | **Yes** | `OwnerClass.propertyName` — the class that declares the property, then a `.`, then the property name |
| `stereotypesVisible` | **Yes** | |
| `nameVisible` | **Yes** | Show the property name label on the edge |
| `color` | **Yes** | Edge colour |
| `lineWidth` | **Yes** | `-1.0` uses the default |
| `lineStyle` | **Yes** | `SIMPLE` (straight/diagonal) or `RIGHT_ANGLE` (orthogonal) |
| `points` | **Yes** | Waypoint list — see [§ points](#points--edge-routing-waypoints) |
| `label` | **Yes** | Label shown alongside the edge; use `''` for none |
| `source` | **Yes** | Local `<id>` of the source `TypeView` |
| `target` | **Yes** | Local `<id>` of the target `TypeView` |
| `propertyPosition` | **Yes** | Canvas position of the property-name label anchor |
| `multiplicityPosition` | **Yes** | Canvas position of the multiplicity label anchor |

---

## AssociationView

Renders a bidirectional edge for a **named `Association`** declared in
`###Pure`. Use this instead of `PropertyView` when the relationship is
modelled with an explicit `Association` element.

```pure
AssociationView <id>(
    association=<FullyQualifiedAssociationName>,
    stereotypesVisible=<boolean>,
    nameVisible=<boolean>,
    color=<#RRGGBB>,
    lineWidth=<float>,
    lineStyle=<SIMPLE|RIGHT_ANGLE>,
    points=[(<x1>, <y1>), (<x2>, <y2>), ...],
    label=<'string'>,
    source=<typeViewId>,
    target=<typeViewId>,
    sourcePropertyPosition=(<x>, <y>),
    sourceMultiplicityPosition=(<x>, <y>),
    targetPropertyPosition=(<x>, <y>),
    targetMultiplicityPosition=(<x>, <y>))
```

### AssociationView field reference

| Field | Required | Notes |
|-------|----------|-------|
| `association` | **Yes** | Fully-qualified (or imported) `Association` name |
| `stereotypesVisible` | **Yes** | |
| `nameVisible` | **Yes** | Show the association name label |
| `color` | **Yes** | |
| `lineWidth` | **Yes** | |
| `lineStyle` | **Yes** | `SIMPLE` or `RIGHT_ANGLE` |
| `points` | **Yes** | Waypoint list — see [§ points](#points--edge-routing-waypoints) |
| `label` | **Yes** | Use `''` for no label |
| `source` | **Yes** | `TypeView` id for one end |
| `target` | **Yes** | `TypeView` id for the other end |
| `sourcePropertyPosition` | **Yes** | Property-name label position near the source end |
| `sourceMultiplicityPosition` | **Yes** | Multiplicity label position near the source end |
| `targetPropertyPosition` | **Yes** | Property-name label position near the target end |
| `targetMultiplicityPosition` | **Yes** | Multiplicity label position near the target end |

### PropertyView vs AssociationView

| | `PropertyView` | `AssociationView` |
|---|---|---|
| When to use | Property declared on a class with no `Association` | Explicit `Association` element in `###Pure` |
| Reference field | `property=OwnerClass.propName` | `association=my::pkg::MyAssociation` |
| Label anchors | One pair: `propertyPosition`, `multiplicityPosition` | Two pairs (one per end): `sourcePropertyPosition`, etc. |

---

## GeneralizationView

Renders an inheritance arrow from a subclass to its superclass.

```pure
GeneralizationView <id>(
    color=<#RRGGBB>,
    lineWidth=<float>,
    lineStyle=<SIMPLE|RIGHT_ANGLE>,
    points=[(<x1>, <y1>), (<x2>, <y2>), ...],
    label=<'string'>,
    source=<typeViewId>,
    target=<typeViewId>)
```

`source` is the **subclass** `TypeView`; `target` is the **superclass**
`TypeView`. The arrowhead is drawn at the target (superclass) end.

### GeneralizationView field reference

| Field | Required | Notes |
|-------|----------|-------|
| `color` | **Yes** | |
| `lineWidth` | **Yes** | |
| `lineStyle` | **Yes** | `SIMPLE` or `RIGHT_ANGLE` |
| `points` | **Yes** | Waypoint list — see [§ points](#points--edge-routing-waypoints) |
| `label` | **Yes** | Usually `''` |
| `source` | **Yes** | `TypeView` id of the **subclass** |
| `target` | **Yes** | `TypeView` id of the **superclass** |

---

## `points` — edge routing waypoints

`points` controls the routing path of an edge between source and target.

```pure
// No intermediate waypoints — engine draws a direct line
points=[]

// Two waypoints define one intermediate segment
points=[(200.0, 140.0), (450.0, 140.0)]

// Multi-segment routing (e.g. routing around other boxes)
points=[(459.5, 152.6), (459.5, 229.6), (660.5, 229.6), (660.5, 306.6)]
```

**Minimum:** `[]` (empty) or exactly **two or more** `(x, y)` pairs. A list
with a single pair — `[(x, y)]` — is a **parse error**. The grammar requires
either no content or a first pair followed by at least one more.

**`lineStyle` interaction:** `RIGHT_ANGLE` routing expects waypoints placed
at the corner of each bend. `SIMPLE` routing draws straight segments between
each consecutive pair of waypoints.

---

## Common errors

| Error message | Cause | Fix |
|---------------|-------|-----|
| `Missing value for property 'type' on TypeView <id>` | `type=` field absent | Add `type=my::pkg::ClassName` |
| `Missing value for property 'lineWidth' on TypeView <id>` | `lineWidth=` absent | Add `lineWidth=1.0` |
| `Missing value for property 'points' on PropertyView <id>` | `points=` absent | Add `points=[]` or a two-or-more pair list |
| `Missing value for property 'color' on GeneralizationView <id>` | `color=` absent | Add `color=#000000` |
| `Missing value for property 'association' on AssociationView <id>` | `association=` absent | Add `association=my::pkg::MyAssociation` |
| `expected: one of {WIDTH, HEIGHT} found: ')'` | Diagram geometry clause is empty `()` | Supply both `width=` and `height=` or omit the clause entirely |
| `expected: ',' found: ')'` | Only one of `width`/`height` in the geometry clause | Both must appear together |
| `The element 'X' already exists in the package 'Y'` | Two `Diagram` elements share the same qualified name | Rename or merge |
| `token recognition error at: '='` | Unknown field name used, or a field name typo | Check spelling against the field reference tables above |

---

## File layout

Each `###Diagram` section belongs in its own `.pure` file. Mirror the package
path in the file path. Place diagrams in a dedicated sub-package to keep them
separate from model and mapping files.

**Example:** `Diagram my::domain::diagram::TradeModelDiagram` →
`src/main/pure/my/domain/diagram/TradeModelDiagram.pure`

Multiple `Diagram` elements can share one `###Diagram` section if they belong
to the same package, but one diagram per file is easier to maintain.

---

## Getting started without coordinates

Coordinate values are tedious to compute by hand. The recommended workflow:

1. **Declare only `TypeView` blocks** with approximate `position`, `width`, and
   `height` values. `TypeView` blocks compile without any edge views present.
2. Open the diagram in **Legend Studio** — it renders the class boxes and lets
   you drag them into position.
3. **Export** the serialised Pure from Studio. It fills in all coordinate
   values for edge views automatically.
4. Commit the Studio-generated source.

If you need edge views to compile before Studio layout, use `points=[]` and
set all `*Position` fields to `(0.0, 0.0)` as placeholders.
