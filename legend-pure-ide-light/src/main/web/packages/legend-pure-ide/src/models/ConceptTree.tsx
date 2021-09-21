/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  AssociationIcon,
  ClassIcon,
  DiagramIcon,
  ElementIcon,
  EnumerationIcon,
  FunctionIcon,
  NativeFunctionIcon,
  MeasureIcon,
  PackageIcon,
  ProfileIcon,
  UnitIcon,
  UnknownTypeIcon,
} from '../components/shared/Icon';
import type { Clazz } from 'serializr';
import {
  createModelSchema,
  primitive,
  custom,
  SKIP,
  deserialize,
} from 'serializr';

import { guaranteeType } from '../utils/GeneralUtil';
import type { TreeNodeData } from '../utils/TreeUtil';

export enum ConceptType {
  // PRIMITIVE = 'Primitive',
  PACKAGE = 'Package',
  PROFILE = 'Profile',
  CLASS = 'Class',
  ASSOCIATION = 'Association',
  PROPERTY = 'Property',
  ENUMERATION = 'Enumeration',
  ENUM_VALUE = 'Enum',
  MEASURE = 'Measure',
  UNIT = 'Unit',
  FUNCTION = 'ConcreteFunctionDefinition',
  NATIVE_FUNCTION = 'NativeFunction',
  DIAGRAM = 'Diagram',
}

export const getConceptIcon = (type: string): React.ReactNode => {
  switch (type) {
    case ConceptType.PACKAGE:
      return <PackageIcon />;
    case ConceptType.PROFILE:
      return <ProfileIcon />;
    case ConceptType.CLASS:
      return <ClassIcon />;
    case ConceptType.ASSOCIATION:
      return <AssociationIcon />;
    case ConceptType.PROPERTY:
      return <ElementIcon />;
    case ConceptType.ENUMERATION:
      return <EnumerationIcon />;
    case ConceptType.MEASURE:
      return <MeasureIcon />;
    case ConceptType.UNIT:
      return <UnitIcon />;
    case ConceptType.FUNCTION:
      return <FunctionIcon />;
    case ConceptType.NATIVE_FUNCTION:
      return <NativeFunctionIcon />;
    case ConceptType.DIAGRAM:
      return <DiagramIcon />;
    default:
      return <UnknownTypeIcon />;
  }
};

abstract class ConceptAttribute {
  pureId!: string;
  pureType!: string;
  // test?: string; // boolean

  get id(): string {
    return this.pureId;
  }
}

export class PackageConceptAttribute extends ConceptAttribute {
  declare pureId: string;
  declare pureType: string;
  deprecated!: boolean;
}

createModelSchema(PackageConceptAttribute, {
  pureId: primitive(),
  pureType: primitive(),
  deprecated: primitive(),
});

export class PropertyConceptAttribute extends ConceptAttribute {
  declare pureId: string;
  declare pureType: string;
  RO!: string; // boolean
  classPath!: string;
  file!: string;
  line!: string; // number
  column!: string; // number

  override get id(): string {
    return `${this.classPath}.${this.pureId}`;
  }
}

createModelSchema(PropertyConceptAttribute, {
  pureId: primitive(),
  pureType: primitive(),
  RO: primitive(),
  classPath: primitive(),
  file: primitive(),
  line: primitive(),
  column: primitive(),
});

export class ElementConceptAttribute extends ConceptAttribute {
  declare pureId: string;
  declare pureType: string;
  RO!: string; // boolean
  notpublic!: boolean;
  user!: string; // boolean
  file!: string;
  line!: string; // number
  column!: string; // number
}

createModelSchema(ElementConceptAttribute, {
  pureId: primitive(),
  pureType: primitive(),
  RO: primitive(),
  notpublic: primitive(),
  user: primitive(),
  file: primitive(),
  line: primitive(),
  column: primitive(),
});

export class ConceptNode {
  li_attr!: ConceptAttribute;
  id!: string;
  text!: string;
  icon?: string;
  children?: boolean;
  state?: string;

  getNodeAttribute<T extends ConceptAttribute>(clazz: Clazz<T>): T {
    return guaranteeType(
      this.li_attr,
      clazz,
      `Expected concept node attribute to be of type '${clazz.name}'`,
    );
  }
}

createModelSchema(ConceptNode, {
  li_attr: custom(
    () => SKIP,
    (value) => {
      if (value.classPath) {
        return deserialize(PropertyConceptAttribute, value);
      } else if (value.file) {
        return deserialize(ElementConceptAttribute, value);
      } else {
        return deserialize(PackageConceptAttribute, value);
      }
    },
  ),
  id: primitive(),
  text: primitive(),
  icon: primitive(),
  children: primitive(),
  state: primitive(),
});

export interface ConceptTreeNode extends TreeNodeData {
  data: ConceptNode;
}
