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

import type { TreeData } from 'Utilities/TreeUtil';
import { deserialize } from 'serializr';
import { TreeState } from 'Stores/TreeState';
import type { DirectoryTreeNode } from 'Models/DirectoryTree';
import { DirectoryNode } from 'Models/DirectoryTree';
import { action, flow, flowResult, makeObservable, observable } from 'mobx';
import type { EditorStore } from 'Stores/EditorStore';
import { assertTrue, guaranteeNonNullable } from 'Utilities/GeneralUtil';
import type { FileCoordinate } from 'Models/PureFile';
import { ACTIVITY_MODE } from 'Stores/EditorConfig';

const getParentPath = (path: string): string | undefined => {
  const trimmedPath = path.trim();
  const idx = trimmedPath.lastIndexOf('/');
  if (idx <= 0) {
    return undefined;
  }
  return trimmedPath.substring(0, idx);
};

const isFilePath = (path: string): boolean => path.endsWith('.pure');
const pathToId = (path: string): string => `file_${path}`;

export class DirectoryTreeState extends TreeState<DirectoryTreeNode, DirectoryNode> {
  nodeForCreateNewFile?: DirectoryTreeNode;
  nodeForCreateNewDirectory?: DirectoryTreeNode;

  constructor(editorStore: EditorStore) {
    super(editorStore);
    makeObservable(this, {
      nodeForCreateNewFile: observable,
      nodeForCreateNewDirectory: observable,
      setNodeForCreateNewFile: action,
      setNodeForCreateNewDirectory: action,
      revealPath: flow
    });
  }

  setNodeForCreateNewFile = (value: DirectoryTreeNode | undefined): void => {
    assertTrue(!value || value.data.isFolderNode, 'Node selected for creating a new file from must be a directory');
    this.nodeForCreateNewFile = value;
  }

  setNodeForCreateNewDirectory = (value: DirectoryTreeNode | undefined): void => {
    assertTrue(!value || value.data.isFolderNode, 'Node selected for creating a new directory from must be a directory');
    this.nodeForCreateNewDirectory = value;
  }

  async getRootNodes(): Promise<DirectoryNode[]> {
    return (await this.editorStore.applicationStore.client.getDirectoryChildren()).map(node => deserialize(DirectoryNode, node));
  }

  buildTreeData(rootNodes: DirectoryNode[]): TreeData<DirectoryTreeNode> {
    const rootIds: string[] = [];
    const nodes = new Map<string, DirectoryTreeNode>();
    rootNodes.forEach(node => {
      const id = node.li_attr.id;
      rootIds.push(id);
      nodes.set(id, {
        data: node,
        id,
        label: node.text,
      });
    });
    return { rootIds, nodes };
  }

  async getChildNodes(node: DirectoryTreeNode): Promise<DirectoryNode[]> {
    return (await this.editorStore.applicationStore.client.getDirectoryChildren(node.data.li_attr.path)).map(node => deserialize(DirectoryNode, node));
  }

  processChildNodes(node: DirectoryTreeNode, childNodes: DirectoryNode[]): void {
    const treeData = this.getTreeData();
    const childrenIds: string[] = [];
    childNodes.forEach(childNode => {
      const id = childNode.li_attr.id;
      childrenIds.push(id);
      treeData.nodes.set(id, {
        data: childNode,
        id,
        label: childNode.text,
      });
    });
    node.childrenIds = childrenIds;
  }

  *openNode(this: TreeState<DirectoryTreeNode, DirectoryNode>, node: DirectoryTreeNode): Generator<Promise<unknown>, void, unknown> {
    if (node.data.isFileNode) {
      yield flowResult(this.editorStore.loadFile(node.data.li_attr.path));
    }
  }

  *revealPath(this: DirectoryTreeState, path: string, forceOpenDirectoryTreePanel: boolean, coordinate?: FileCoordinate): Generator<Promise<unknown>, void, unknown> {
    if (forceOpenDirectoryTreePanel) {
      this.editorStore.setActiveActivity(ACTIVITY_MODE.FILE, { keepShowingIfMatchedCurrent: true });
    }
    const paths: string[] = [];
    let currentPath: string | undefined = path;
    while (currentPath) {
      paths.unshift(currentPath);
      currentPath = getParentPath(currentPath);
    }
    for (const _path of paths) {
      if (!isFilePath(_path)) {
        const node = guaranteeNonNullable(this.getTreeData().nodes.get(pathToId(_path)), `Can't find directory node with path '${_path}'`);
        yield flowResult(this.expandNode(node));
      } else {
        yield flowResult(this.editorStore.loadFile(_path, coordinate));
      }
    }
    const fileNode = guaranteeNonNullable(this.getTreeData().nodes.get(pathToId(path)), `Can't find file node with path '${path}'`);
    this.setSelectedNode(fileNode);
  }
}
