/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { usingConstantValueSchema } from '@finos/legend-shared';
import {
  createModelSchema,
  primitive,
  object,
  list,
  optional,
} from 'serializr';
import { SourceInformation } from './SourceInformation';

// ----------------------------------- Shared PURE serialization model ---------------------------------------
//
// We don't intend to build Pure graph from these serialization models, hence, we never really want to export them
// to use outside of this file; their sole purpose is to get the result from the diagram info endpoints
// to convert to Legend protocol model to use in Legend Studio diagram renderer

class PURE__Steoreotype {
  profile!: string;
  value!: string;
}

createModelSchema(PURE__Steoreotype, {
  profile: primitive(),
  value: primitive(),
});

class PURE__Tag {
  profile!: string;
  value!: string;
}

createModelSchema(PURE__Tag, {
  profile: primitive(),
  value: primitive(),
});

class PURE__TaggedValue {
  tag!: PURE__Tag;
  value!: string;
}

createModelSchema(PURE__TaggedValue, {
  tag: object(PURE__Tag),
  value: primitive(),
});

class PURE__GenericType {
  rawType!: string;
  // typeArguments
}

createModelSchema(PURE__GenericType, {
  rawType: primitive(),
});

class PURE__Property {
  name!: string;
  stereotypes: PURE__Steoreotype[] = [];
  taggedValues: PURE__TaggedValue[] = [];

  // aggregation!: string;
  multiplicity!: string;
  // parameters // this is meant for qualified properties only
  genericType!: PURE__GenericType;
}

createModelSchema(PURE__Property, {
  genericType: object(PURE__GenericType),
  multiplicity: primitive(),
  name: primitive(),
  stereotypes: list(object(PURE__Steoreotype)),
  taggedValues: list(object(PURE__TaggedValue)),
});

// NOTE: technically this is Type, but here we hack it to Class for simplicity
// because the only types supported in Diagram are classes
class PURE__Type {
  package!: string;
  name!: string;
  sourceInformation!: SourceInformation;
  stereotypes: PURE__Steoreotype[] = [];
  taggedValues: PURE__TaggedValue[] = [];

  // typeParameters: string[] = [];
  generalizations: PURE__GenericType[] = [];
  properties: PURE__Property[] = [];
}

createModelSchema(PURE__Type, {
  generalizations: list(object(PURE__GenericType)),
  name: primitive(),
  package: primitive(),
  properties: list(object(PURE__Property)),
  sourceInformation: object(SourceInformation),
  stereotypes: list(object(PURE__Steoreotype)),
  taggedValues: list(object(PURE__TaggedValue)),
});

class PURE__Enumeration {
  package!: string;
  name!: string;
  // sourceInformation!: SourceInformation;

  enumValues: string[] = [];
}

createModelSchema(PURE__Type, {
  name: primitive(),
  package: primitive(),
  enumValues: list(primitive()),
});

// -------------------------------------- Diagram -----------------------------------------

class PURE__Point {
  x!: number;
  y!: number;
}

createModelSchema(PURE__Point, {
  x: primitive(),
  y: primitive(),
});

class PURE__Rectangle {
  height!: number;
  width!: number;
}

createModelSchema(PURE__Rectangle, {
  height: primitive(),
  width: primitive(),
});

class PURE__Geometry {
  style!: string; // unsupported: hardcoded for now
  points: PURE__Point[] = [];
}

createModelSchema(PURE__Geometry, {
  points: list(object(PURE__Point)),
  style: usingConstantValueSchema('SIMPLE'),
});

class PURE__GeneralizationView {
  id!: string;
  source!: string;
  target!: string;
  label!: string; // unsupported: hardcoded for now

  geometry!: PURE__Geometry;
  rendering!: object; // unsupported: hardcoded for now
}

createModelSchema(PURE__GeneralizationView, {
  geometry: object(PURE__Geometry),
  id: primitive(),
  label: usingConstantValueSchema(''),
  rendering: usingConstantValueSchema({ color: '#000000', lineWidth: -1.0 }),
  source: primitive(),
  target: primitive(),
});

class PURE__PropertyViewPropertyPointer {
  name!: string;
  owningType!: string;
}

createModelSchema(PURE__Rectangle, {
  name: primitive(),
  owningType: primitive(),
});

class PURE__PropertyView {
  id!: string;
  source!: string;
  target!: string;
  label!: string;

  property!: PURE__PropertyViewPropertyPointer;
  geometry!: PURE__Geometry;
  rendering!: object; // unsupported: hardcoded for now
  visibility!: object; // unsupported: hardcoded for now
  view!: object; // unsupported: hardcoded for now
}

createModelSchema(PURE__PropertyView, {
  geometry: object(PURE__Geometry),
  id: primitive(),
  label: usingConstantValueSchema(''),
  property: object(PURE__PropertyViewPropertyPointer),
  rendering: usingConstantValueSchema({ color: '#000000', lineWidth: -1.0 }),
  source: primitive(),
  target: primitive(),
  view: usingConstantValueSchema({
    propertyLocation: { x: 0.0, y: 0.0 },
    multiplicityLocation: { x: 0.0, y: 0.0 },
  }),
  visibility: usingConstantValueSchema({
    visibleName: true,
    visibleStereotype: true,
  }),
});

class PURE__TypeView {
  id!: string;
  type!: string;
  position!: PURE__Point;
  rectangleGeometry!: PURE__Rectangle;

  rendering!: object; // unsupported: hardcoded for now
  typeVisibility!: object; // unsupported: hardcoded for now
  attributeVisibility!: object; // unsupported: hardcoded for now
}

createModelSchema(PURE__TypeView, {
  attributeVisibility: usingConstantValueSchema({
    visibleTypes: true,
    visibleStereotype: true,
  }),
  id: primitive(),
  position: object(PURE__Point),
  rectangleGeometry: object(PURE__Rectangle),
  rendering: usingConstantValueSchema({ color: '#FFFFCC', lineWidth: 1.0 }),
  type: primitive(),
  typeVisibility: usingConstantValueSchema({
    visibleAttributeCompartment: true,
    visibleStereotype: true,
  }),
});

class PURE__Diagram {
  package!: string;
  name!: string;
  sourceInformation!: SourceInformation;
  stereotypes: PURE__Steoreotype[] = [];
  taggedValues: PURE__TaggedValue[] = [];

  rectangleGeometry!: object; // unsupported: hardcoded for now
  // associationViews
  generalizationViews: PURE__GeneralizationView[] = [];
  propertyViews: PURE__PropertyView[] = [];
  typeViews: PURE__TypeView[] = [];
}

createModelSchema(PURE__Diagram, {
  name: primitive(),
  generalizationViews: list(object(PURE__GeneralizationView)),
  package: primitive(),
  propertyViews: list(object(PURE__PropertyView)),
  rectangleGeometry: usingConstantValueSchema({
    height: 0.0,
    width: 0.0,
  }),
  sourceInformation: object(SourceInformation),
  stereotypes: list(object(PURE__Steoreotype)),
  taggedValues: list(object(PURE__TaggedValue)),
  typeViews: list(object(PURE__TypeView)),
});

// ----------------------------------- Diagram Info ---------------------------------------

class DiagramDomainInfo {
  types: PURE__Type[] = [];
  // associations // skip these for now as we don't support association views
  enumerations: PURE__Enumeration[] = [];
}

createModelSchema(DiagramDomainInfo, {
  // associations
  enumerations: list(object(PURE__Enumeration)),
  types: list(object(PURE__Type)),
});

export class DiagramInfo {
  name!: string;
  diagram!: PURE__Diagram;
  subDomain?: DiagramDomainInfo;
}

createModelSchema(DiagramInfo, {
  diagram: object(PURE__Diagram),
  name: primitive(),
  subDomain: optional(object(DiagramDomainInfo)),
});

export class DiagramClassInfo {
  class!: PURE__Type;
  // associations
  // specializations
  enumerations: PURE__Enumeration[] = [];
}

createModelSchema(DiagramClassInfo, {
  // associations
  class: object(PURE__Type),
  enumerations: list(object(PURE__Enumeration)),
  // specializations
});

// ----------------------------------- Protocol converter ---------------------------------------
