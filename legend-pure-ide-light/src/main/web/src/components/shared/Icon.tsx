/**
 * Copyright 2020 Goldman Sachs
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

import React from 'react';
import { FaQuestion, FaShapes } from 'react-icons/fa';
import { FiPackage } from 'react-icons/fi';
import { BiAtom } from 'react-icons/bi';

export const PrimitiveTypeIcon: React.FC = () => <div className="icon icon--primitive color--primitive">p</div>;
export const PackageIcon: React.FC = () => <div className="icon color--package"><FiPackage /></div>;
export const ClassIcon: React.FC = () => <div className="icon color--class">C</div>;
export const AssociationIcon: React.FC = () => <div className="icon color--association">A</div>;
export const EnumValueIcon: React.FC = () => <div className="icon icon--enum-value color--enum-value">e</div>;
export const EnumerationIcon: React.FC = () => <div className="icon color--enumeration">E</div>;
export const MeasureIcon: React.FC = () => <div className="icon color--measure">M</div>;
export const UnitIcon: React.FC = () => <div className="icon color--unit">u</div>;
export const ProfileIcon: React.FC = () => <div className="icon color--profile">P</div>;
export const FunctionIcon: React.FC = () => <div className="icon icon--function color--function">(x)</div>;
export const NativeFunctionIcon: React.FC = () => <div className="icon icon--function color--native-function">(x)</div>;
export const ElementIcon: React.FC = () => <div className="icon icon--property color--property"><BiAtom /></div>;
export const DiagramIcon: React.FC = () => <div className="icon color--diagram"><FaShapes /></div>;
export const UnknownTypeIcon: React.FC = () => <div><FaQuestion /></div>;
