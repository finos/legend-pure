# Pure M3 Compiled Graph Instances

There are four types of instances in the Pure M3 compiled graph:

* concrete packageable elements
* component instances
* packages
* primitive values

## Concrete Packageable Elements

A **concrete packageable element** is a PackageableElement that is concretely defined in source code. It is either top
level in the model (such as Package, Root, or the primitive types) or is in the package tree.

Every concrete element must have a non-empty name. For top level elements, the name must be unique among all top level
elements. For elements in the package tree, the name must be unique among all children of its containing package.

Each concrete element has a unique package path. For top level elements, this is its name. For elements in the package
tree, this is the path of its containing package (omitting Root) followed by its name (conjoined with ::).

Every concrete element has source information, which identifies the file and location within the file where it is
defined. No two concrete elements may have overlapping source information.

Examples of concrete elements are classes, profiles, enumerations, and associations.

## Component Instances

A **component instance** does not have its own distinct individual existence, but instead exists as a component or part
of a concrete element. Each component instance is a component of exactly one concrete element, called its "containing"
or "owning" element.

The link between the component instance and its containing element may be direct or indirect. For example, a property of
a class will be linked directly to the class, but the generic type of the property may be only indirectly linked.
Nonetheless, both the property and the generic type are component instances of the class.

While each component instance is owned by exactly one concrete element, many instances (both internal and external to
the element) may have references to it. For example, a property of a class may appear in expressions in many different
functions.

Component instances may or may not have source information. If a component instance is referenced externally to its
containing element, it must have source information. If the component instance has a well defined location in source
code, it should have source information which reflects this. Otherwise, source information is optional. If a component
instance does have source information, it must be subsumed by the source information of its containing element. Distinct
component instances may have overlapping source information, but in that case one must subsume the other.

Examples of component instances are properties, stereotypes, enum values, function expressions, and generic types.

## Packages

Some **packages** are concrete, such as Root, but most are not. Concrete packages exist in their own right, whether they
have children or not. They are subject to all the requirements of concrete elements in general.

Non-concrete, or virtual, packages exist only when some element is a child (directly or indirectly) of the package. They
do not have source information. However, they are subject to the same name requirements as concrete elements.

## Primitive Values

**Primitive values** are instances of primitive types (String, Integer, Boolean, etc). Primitive values are never
concrete. They have no source information and no substantive properties.
