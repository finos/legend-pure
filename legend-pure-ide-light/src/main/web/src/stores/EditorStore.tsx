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

import React, { createContext, useContext } from 'react';
import { useLocalObservable } from 'mobx-react-lite';
import { flowResult, makeAutoObservable } from 'mobx';
import type { ApplicationStore, ActionAlertInfo, BlockingAlertInfo } from './ApplicationStore';
import { ActionAlertType, ActionAlertActionType, useApplicationStore } from './ApplicationStore';
import { ACTIVITY_MODE, DEFAULT_SIDE_BAR_SIZE, AUX_PANEL_MODE, DEFAULT_AUX_PANEL_SIZE } from 'Stores/EditorConfig';
import type { Clazz, PlainObject } from 'Utilities/GeneralUtil';
import { guaranteeType, guaranteeNonNullable, assertNonNullable, assertTrue } from 'Utilities/GeneralUtil';
import type { EditorState } from 'Stores/EditorState';
import { FileEditorState } from 'Stores/EditorState';
import { deserialize } from 'serializr';
import { ActionState } from 'Utilities/ActionState';
import { FileCoordinate, PureFile, trimPathLeadingSlash } from 'Models/PureFile';
import { DirectoryTreeState } from 'Stores/DirectoryTreeState';
import { ConceptTreeState } from 'Stores/ConceptTreeState';
import type { InitializationActivity } from 'Models/Initialization';
import { InitializationFailureWithSourceResult, InitializationFailureResult, deserializeInitializationnResult } from 'Models/Initialization';
import type { CandidateWithPackageNotImported, ExecutionActivity, ExecutionResult } from 'Models/Execution';
import { TestExecutionResult, UnmatchedFunctionResult, UnmatchedResult, GetConceptResult, deserializeExecutionResult, ExecutionFailureResult, ExecutionSuccessResult } from 'Models/Execution';
import type { SearchResultEntry } from 'Models/SearchEntry';
import { getSearchResultEntry } from 'Models/SearchEntry';
import type { SearchState } from 'Stores/SearchResultState';
import { UsageResultState, UnmatchedFunctionExecutionResultState, UnmatchExecutionResultState, SearchResultState } from 'Stores/SearchResultState';
import { TestRunnerState } from 'Stores/TestRunnerState';
import type { UsageConcept } from 'Models/Usage';
import { getUsageConceptLabel, Usage } from 'Models/Usage';
import type { CommandResult } from 'Models/Command';
import { CommandFailureResult, deserializeCommandResult } from 'Models/Command';

class SearchCommandState {
  text = '';
  isCaseSensitive = false;
  isRegExp = false;

  constructor() {
    makeAutoObservable(this);
  }

  reset(): void {
    this.setText('');
    this.setCaseSensitive(false);
    this.setRegExp(false);
  }
  setText(value: string): void { this.text = value }
  setCaseSensitive(value: boolean): void { this.isCaseSensitive = value }
  toggleCaseSensitive(): void { this.setCaseSensitive(!this.isCaseSensitive) }
  setRegExp(value: boolean): void { this.isRegExp = value }
  toggleRegExp(): void { this.setRegExp(!this.isRegExp) }
}

export class EditorStore {
  applicationStore: ApplicationStore;
  directoryTreeState: DirectoryTreeState;
  conceptTreeState: ConceptTreeState;
  initState = new ActionState();
  // Tabs
  currentEditorState?: EditorState;
  openedEditorStates: EditorState[] = [];
  // App states
  isInExpandedMode = true;
  backdrop = false;
  blockGlobalHotkeys = false;
  // Aux Panel
  isMaxAuxPanelSizeSet = false;
  activeAuxPanelMode = AUX_PANEL_MODE.CONSOLE;
  maxAuxPanelSize = DEFAULT_AUX_PANEL_SIZE;
  auxPanelSize = DEFAULT_AUX_PANEL_SIZE;
  previousAuxPanelSize = 0;
  // Side Bar
  activeActivity?: ACTIVITY_MODE = ACTIVITY_MODE.CONCEPT;
  sideBarSize = DEFAULT_SIDE_BAR_SIZE;
  sideBarSizeBeforeHidden = DEFAULT_SIDE_BAR_SIZE;
  // Console
  consoleText?: string;
  // Execution
  executionState = new ActionState();
  // Navigation
  navigationStack: FileCoordinate[] = []; // TODO?: we might want to limit the number of items in this stack
  // File search
  openFileSearchCommand = false;
  fileSearchCommandLoadingState = new ActionState();
  fileSearchCommandResults: string[] = [];
  fileSearchCommandState = new SearchCommandState();
  // Text search
  openTextSearchCommand = false;
  textSearchCommandLoadingState = new ActionState();
  textSearchCommandState = new SearchCommandState();
  // Search Panel
  searchState?: SearchState;
  // Test
  testRunState = new ActionState();
  testRunnerState?: TestRunnerState;

  constructor(applicationStore: ApplicationStore) {
    makeAutoObservable(this);
    this.applicationStore = applicationStore;
    this.directoryTreeState = new DirectoryTreeState(this);
    this.conceptTreeState = new ConceptTreeState(this);
  }

  get isAuxPanelMaximized(): boolean { return this.auxPanelSize === this.maxAuxPanelSize }

  setBlockGlobalHotkeys(val: boolean): void { this.blockGlobalHotkeys = val }
  setCurrentEditorState(val: EditorState | undefined): void { this.currentEditorState = val }
  setBackdrop(val: boolean): void { this.backdrop = val }
  setExpandedMode(val: boolean): void { this.isInExpandedMode = val }
  setOpenFileSearchCommand(val: boolean): void { this.openFileSearchCommand = val }
  setOpenTextSearchCommand(val: boolean): void { this.openTextSearchCommand = val }
  setAuxPanelSize(val: number): void { this.auxPanelSize = val }
  setActiveAuxPanelMode(val: AUX_PANEL_MODE): void { this.activeAuxPanelMode = val }
  setSideBarSize(val: number): void { this.sideBarSize = val }
  setActionAltertInfo(alertInfo: ActionAlertInfo | undefined): void { this.applicationStore.setActionAltertInfo(alertInfo) }
  setConsoleText(value: string | undefined): void { this.consoleText = value }
  setBlockingAlert(alertInfo: BlockingAlertInfo | undefined): void {
    this.setBlockGlobalHotkeys(Boolean(alertInfo)); // block global hotkeys if alert is shown
    this.applicationStore.setBlockingAlert(alertInfo);
  }
  setSearchState(val: SearchState | undefined): void { this.searchState = val }
  setTestRunnerState(val: TestRunnerState | undefined): void { this.testRunnerState = val }

  cleanUp(): void {
    // dismiss all the alerts as these are parts of application, if we don't do this, we might
    // end up blocking other parts of the app
    // e.g. trying going to an unknown workspace, we will be redirected to the home page
    // but the blocking alert for not-found workspace will still block the app
    this.setBlockingAlert(undefined);
    this.setActionAltertInfo(undefined);
  }

  /**
   * This is the entry of the app logic where initialization of editor states happens
   * Here, we ensure the order of calls after checking existence of current project and workspace
   * If either of them does not exist, we cannot proceed.
   */
  *initialize(this: EditorStore, fullInit: boolean, func: (() => Promise<void>) | undefined, mode: string | undefined, fastCompile: string | undefined): Generator<Promise<unknown>, void, unknown> {
    if (!this.initState.isInInitialState) {
      this.applicationStore.notifyIllegalState('Editor store is re-initialized');
      return;
    }
    // set PURE IDE mode
    this.applicationStore.client.mode = mode;
    this.applicationStore.client.compilerMode = fastCompile;
    // initialize editor
    this.initState.inProgress();
    try {
      const initializationPromise = this.applicationStore.client.initialize(!fullInit);
      this.setBlockingAlert({ message: 'Loading Pure IDE...', prompt: 'Please be patient as we are building the initial application state', showLoading: true });
      yield this.pullInitializationActivity();
      this.setBlockingAlert(undefined);
      const openWelcomeFilePromise = flowResult(this.loadFile('/welcome.pure'));
      const directoryTreeInitPromise = this.directoryTreeState.initialize();
      const conceptTreeInitPromise = this.conceptTreeState.initialize();
      const result = deserializeInitializationnResult((yield initializationPromise) as Record<PropertyKey, unknown>);
      if (result.text) {
        this.setConsoleText(result.text);
        this.openAuxPanel(AUX_PANEL_MODE.CONSOLE, true);
      }
      if (result instanceof InitializationFailureResult) {
        if (result.sessionError) {
          this.setBlockingAlert({ message: 'Session corrupted', prompt: result.sessionError });
        } else if (result instanceof InitializationFailureWithSourceResult) {
          yield flowResult(this.loadFile(result.source, new FileCoordinate(result.source, result.line, result.column, (result.text ?? '').split('\n').filter(Boolean)[0])));
        }
      } else {
        if (func) {
          yield func();
        }
        yield Promise.all([
          openWelcomeFilePromise,
          directoryTreeInitPromise,
          conceptTreeInitPromise,
        ]);
      }
    } catch (e) {
      this.applicationStore.notifyError(e);
      this.initState.conclude(false);
      this.setBlockingAlert({ message: 'Failed to initialize IDE', prompt: 'Make sure the IDE server is working, otherwise try to restart it' });
      return;
    }
    this.initState.conclude(true);
  }

  *checkIfSessionWakingUp(this: EditorStore, message?: string): Generator<Promise<unknown> | undefined, void, unknown> {
    this.setBlockingAlert({ message: message ?? 'Checking IDE session...', showLoading: true });
    yield this.pullInitializationActivity((activity: InitializationActivity) => {
      if (activity.text) {
        this.setBlockingAlert({ message: message ?? 'Checking IDE session...', prompt: activity.text, showLoading: true });
      }
    });
    this.setBlockingAlert(undefined);
  }

  async pullInitializationActivity(fn?: (activity: InitializationActivity) => void): Promise<void> {
    const result = (await this.applicationStore.client.getInitializationActivity()) as InitializationActivity;
    if (result.initializing) {
      return new Promise((resolve, reject) =>
        setTimeout(() => {
          try {
            resolve(this.pullInitializationActivity());
          } catch (e) {
            reject(e);
          }
        }, 1000)
      );
    }
    return Promise.resolve();
  }

  getCurrentEditorState<T extends EditorState>(clazz: Clazz<T>): T {
    return guaranteeType(this.currentEditorState, clazz, `Expected current editor state to be of type '${clazz.name}' (this is caused by calling this method at the wrong place)`);
  }

  openAuxPanel(auxPanelMode: AUX_PANEL_MODE, resetHeightIfTooSmall: boolean): void {
    this.activeAuxPanelMode = auxPanelMode;
    if (this.auxPanelSize === 0) {
      this.toggleAuxPanel();
    } else if (this.auxPanelSize < DEFAULT_AUX_PANEL_SIZE && resetHeightIfTooSmall) {
      this.auxPanelSize = DEFAULT_AUX_PANEL_SIZE;
    }
  }

  toggleAuxPanel(): void {
    if (this.auxPanelSize === 0) {
      this.auxPanelSize = this.previousAuxPanelSize;
    } else {
      this.previousAuxPanelSize = this.auxPanelSize || DEFAULT_AUX_PANEL_SIZE;
      this.auxPanelSize = 0;
    }
  }

  toggleExpandAuxPanel(): void {
    if (this.auxPanelSize === this.maxAuxPanelSize) {
      this.auxPanelSize = this.previousAuxPanelSize === this.maxAuxPanelSize ? DEFAULT_AUX_PANEL_SIZE : this.previousAuxPanelSize;
    } else {
      this.previousAuxPanelSize = this.auxPanelSize;
      this.auxPanelSize = this.maxAuxPanelSize;
    }
  }

  setMaxAuxPanelSize(val: number): void {
    if (this.isMaxAuxPanelSizeSet) {
      if (this.previousAuxPanelSize === this.maxAuxPanelSize) { this.previousAuxPanelSize = val }
      if (this.auxPanelSize === this.maxAuxPanelSize) { this.auxPanelSize = val }
    }
    this.maxAuxPanelSize = val;
    this.isMaxAuxPanelSizeSet = true;
  }

  setActiveActivity(
    activity: ACTIVITY_MODE,
    options?: { keepShowingIfMatchedCurrent?: boolean }
  ): void {
    if (this.sideBarSize === 0) {
      this.sideBarSize = this.sideBarSizeBeforeHidden;
    } else if (activity === this.activeActivity && !options?.keepShowingIfMatchedCurrent) {
      this.sideBarSizeBeforeHidden = this.sideBarSize || DEFAULT_SIDE_BAR_SIZE;
      this.sideBarSize = 0;
    }
    this.activeActivity = activity;
  }

  closeState(editorState: EditorState): void {
    const elementIndex = this.openedEditorStates.findIndex(e => e === editorState);
    assertTrue(elementIndex !== -1, `Can't close a tab which is not opened`);
    this.openedEditorStates.splice(elementIndex, 1);
    if (this.currentEditorState === editorState) {
      if (this.openedEditorStates.length) {
        const openIndex = elementIndex - 1;
        this.setCurrentEditorState(openIndex >= 0 ? this.openedEditorStates[openIndex] : this.openedEditorStates[0]);
      } else {
        this.setCurrentEditorState(undefined);
      }
    }
  }

  closeAllOtherStates(editorState: EditorState): void {
    assertNonNullable(this.openedEditorStates.find(e => e === editorState), 'Editor tab should be currently opened');
    this.currentEditorState = editorState;
    this.openedEditorStates = [editorState];
  }

  closeAllStates(): void {
    this.currentEditorState = undefined;
    this.openedEditorStates = [];
  }

  openState(editorState: EditorState): void {
    if (editorState instanceof FileEditorState) {
      this.openFile(editorState.file, editorState.path);
    }
  }

  *loadFile(path: string, coordinate?: FileCoordinate): Generator<Promise<unknown>, void, unknown> {
    const existingFileState = this.openedEditorStates.find(editorState => editorState instanceof FileEditorState && editorState.path === path);
    if (existingFileState instanceof FileEditorState) {
      if (coordinate) {
        existingFileState.setCoordinate(coordinate);
      }
      this.openFile(existingFileState.file, existingFileState.path);
    } else {
      const file = deserialize(PureFile, yield this.applicationStore.client.getFile(path));
      yield flowResult(this.checkIfSessionWakingUp());
      this.openFile(file, path, coordinate);
    }
  }

  openFile(file: PureFile, path: string, coordinate?: FileCoordinate): void {
    const existingFileState = this.openedEditorStates.find(editorState => editorState instanceof FileEditorState && editorState.path === path);
    const fileState = existingFileState ?? new FileEditorState(this, file, path, coordinate);
    if (!existingFileState) {
      this.openedEditorStates.push(fileState);
    }
    this.setCurrentEditorState(fileState);
  }

  *reloadFile(path: string): Generator<Promise<unknown>, void, unknown> {
    const existingFileState = this.openedEditorStates.find(editorState => editorState instanceof FileEditorState && editorState.path === path);
    if (existingFileState instanceof FileEditorState) {
      const file = deserialize(PureFile, yield this.applicationStore.client.getFile(path));
      existingFileState.setFile(file);
      existingFileState.setCoordinate(undefined);
    }
  }

  *execute(this: EditorStore, url: string, extraParams: Record<PropertyKey, unknown>, checkExecutionStatus: boolean, manageResult: (result: ExecutionResult) => Promise<void>): Generator<Promise<unknown>, void, unknown> {
    if (!this.initState.hasSucceeded) {
      this.applicationStore.notifyWarning(`Can't execute while initializing application`);
      return;
    }
    if (this.executionState.isInProgress) {
      this.applicationStore.notifyWarning('Another execution is already in progress!');
      return;
    }
    // reset search state before execution
    if (!(this.searchState instanceof SearchResultState)) {
      this.setSearchState(undefined);
    }
    this.executionState.inProgress();
    try {
      this.setBlockingAlert({ message: 'Executing...', prompt: 'Please do not refresh the application', showLoading: true });
      const openedFiles = this.openedEditorStates
        .filter((editorState): editorState is FileEditorState => editorState instanceof FileEditorState)
        .map(fileEditorState => ({
          path: fileEditorState.path,
          // TODO: investigate why if we send `\r\n` the server will duplicate the new line character
          code: fileEditorState.file.content.replace(/\r\n/g, '\n'),
        }));
      const executionPromise = this.applicationStore.client.execute(openedFiles, url, extraParams);
      if (checkExecutionStatus) {
        yield this.pullExecutionStatus();
      }
      this.setBlockingAlert({ message: 'Executing...', prompt: 'Please do not refresh the application', showLoading: true });
      const result = deserializeExecutionResult((yield executionPromise) as Record<PropertyKey, unknown>);
      this.setBlockingAlert(undefined);
      this.setConsoleText(result.text);
      if (result instanceof ExecutionFailureResult) {
        this.applicationStore.notifyWarning('Execution failed!');
        if (result.sessionError) {
          this.setBlockingAlert({ message: 'Session corrupted', prompt: result.sessionError });
        } else {
          yield flowResult(manageResult(result));
        }
      } else if (result instanceof ExecutionSuccessResult) {
        this.applicationStore.notifySuccess('Execution succeeded!');
        if (result.reinit) {
          this.setBlockingAlert({ message: 'Reinitializing...', prompt: 'Please do not refresh the application', showLoading: true });
          this.initState.initial();
          yield flowResult(this.initialize(false, () => flowResult(this.execute(url, extraParams, checkExecutionStatus, manageResult)), this.applicationStore.client.mode, this.applicationStore.client.compilerMode));
        } else {
          yield flowResult(manageResult(result));
        }
      } else {
        yield flowResult(manageResult(result));
      }
    } finally {
      this.executionState.initial();
    }
  }

  // NOTE: currently backend do not suppor this operation, so we temporarily disable it, but
  // in theory, this will pull up a blocking modal to show the execution status to user
  async pullExecutionStatus(): Promise<void> {
    const result = await this.applicationStore.client.getExecutionActivity() as ExecutionActivity;
    this.setBlockingAlert({ message: 'Executing...', prompt: result.text ? result.text : 'Please do not refresh the application', showLoading: true });
    if (result.executing) {
      return new Promise((resolve, reject) =>
        setTimeout(() => {
          try {
            resolve(this.pullExecutionStatus());
          } catch (e) {
            reject(e);
          }
          // NOTE: tune this slightly lower for better experience, also for sub-second execution, setting a high number
          // might create the illusion that the system is slow
        }, 500)
      );
    }
    return Promise.resolve();
  }

  *executeGo(this: EditorStore): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.execute('executeGo', {}, true, (result: ExecutionResult) => flowResult(this.manageExecuteGoResult(result))));
  }

  *manageExecuteGoResult(result: ExecutionResult): Generator<Promise<unknown>, void, unknown> {
    const refreshTreesPromise = flowResult(this.refreshTrees());
    if (result instanceof ExecutionFailureResult) {
      yield flowResult(this.loadFile(result.source, new FileCoordinate(result.source, result.line, result.column, result.text.split('\n').filter(Boolean)[0])));
      if (result instanceof UnmatchedFunctionResult) {
        this.setSearchState(new UnmatchedFunctionExecutionResultState(this, result));
        this.openAuxPanel(AUX_PANEL_MODE.SEARCH_RESULT, true);
      } else if (result instanceof UnmatchedResult) {
        this.setSearchState(new UnmatchExecutionResultState(this, result));
        this.openAuxPanel(AUX_PANEL_MODE.SEARCH_RESULT, true);
      }
    } else if (result instanceof ExecutionSuccessResult) {
      if (result.modifiedFiles.length) {
        for (const path of result.modifiedFiles) {
          yield flowResult(this.reloadFile(path));
        }
      }
    }
    yield refreshTreesPromise;
  }

  *executeTests(path: string, relevantTestsOnly?: boolean): Generator<Promise<unknown>, void, unknown> {
    if (this.testRunState.isInProgress) {
      this.applicationStore.notifyWarning('Test runner is working. Please try again later');
      return;
    }
    this.testRunState.inProgress();
    yield flowResult(this.execute('executeTests', {
      path,
      relevantTestsOnly,
    }, false, async (result: ExecutionResult) => {
      const refreshTreesPromise = flowResult(this.refreshTrees());
      if (result instanceof ExecutionFailureResult) {
        await flowResult(this.loadFile(result.source, new FileCoordinate(result.source, result.line, result.column, result.text.split('\n').filter(Boolean)[0])));
        this.openAuxPanel(AUX_PANEL_MODE.CONSOLE, true);
        this.testRunState.conclude(false);
      } else if (result instanceof TestExecutionResult) {
        this.openAuxPanel(AUX_PANEL_MODE.TEST_RUNNER, true);
        const testRunnerState = new TestRunnerState(this, result);
        this.setTestRunnerState(testRunnerState);
        await flowResult(testRunnerState.buildTestTreeData());
        // make sure we refresh tree so it is shown in the explorer panel
        // NOTE: we could potentially expand the tree here, but this operation is expensive since we have all nodes observable
        // so it will lag the UI if we have too many nodes open
        testRunnerState.refreshTree();
        await flowResult(testRunnerState.pollTestRunnerResult());
        this.testRunState.conclude(true);
      }
      // do nothing?
      await refreshTreesPromise;
    }));
  }

  *executeFullTestSuite(relevantTestsOnly?: boolean): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.executeTests('::', relevantTestsOnly));
  }

  *executeNavigation(this: EditorStore, coordinate: FileCoordinate): Generator<Promise<unknown>, void, unknown> {
    this.navigationStack.push(coordinate);
    yield flowResult(this.execute('getConcept', {
      file: coordinate.file,
      line: coordinate.line,
      column: coordinate.column,
    }, false, async (result: ExecutionResult) => {
      if (result instanceof GetConceptResult) {
        await flowResult(this.loadFile(result.jumpTo.source, new FileCoordinate(result.jumpTo.source, result.jumpTo.line, result.jumpTo.column)));
      }
    }));
  }

  *navigateBack(this: EditorStore): Generator<Promise<unknown>, void, unknown> {
    if (this.navigationStack.length === 0) {
      this.applicationStore.notifyWarning(`Can't navigate back any further - navigation stack is empty`);
      return;
    }
    if (this.navigationStack.length > 0) {
      const coordinate = this.navigationStack.pop();
      if (coordinate) {
        yield flowResult(this.loadFile(coordinate.file, coordinate));
      }
    }
  }

  *executeSaveAndReset(this: EditorStore, fullInit: boolean): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.execute('executeSaveAndReset', {}, true, async (result: ExecutionResult) => {
      this.initState.initial();
      await flowResult(this.initialize(fullInit, undefined, this.applicationStore.client.mode, this.applicationStore.client.compilerMode));
      this.setActiveActivity(ACTIVITY_MODE.CONCEPT, { keepShowingIfMatchedCurrent: true });
    }));
  }

  *fullReCompile(this: EditorStore, fullInit: boolean): Generator<Promise<unknown>, void, unknown> {
    this.setActionAltertInfo({
      message: 'Are you sure you want to perform a full re-compile?',
      prompt: 'This may take a long time to complete',
      type: ActionAlertType.CAUTION,
      onEnter: (): void => this.setBlockGlobalHotkeys(true),
      onClose: (): void => this.setBlockGlobalHotkeys(false),
      actions: [
        {
          label: 'Perform full re-compile',
          type: ActionAlertActionType.PROCEED_WITH_CAUTION,
          handler: (): Promise<void> => flowResult(this.executeSaveAndReset(fullInit))
        },
        {
          label: 'Abort',
          type: ActionAlertActionType.PROCEED,
          default: true,
        }
      ],
    });
  }

  *refreshTrees(this: EditorStore): Generator<Promise<unknown>, void, unknown> {
    yield Promise.all([
      this.directoryTreeState.refreshTreeData(),
      this.conceptTreeState.refreshTreeData(),
    ]);
  }

  *updateFileUsingSuggestionCandidate(this: EditorStore, candidate: CandidateWithPackageNotImported): Generator<Promise<unknown>, void, unknown> {
    this.setSearchState(undefined);
    yield flowResult(this.updateFile(candidate.fileToBeModified, candidate.lineToBeModified, candidate.columnToBeModified, candidate.add, candidate.messageToBeModified));
    this.openAuxPanel(AUX_PANEL_MODE.CONSOLE, true);
  }

  *updateFile(this: EditorStore, path: string, line: number, column: number, add: boolean, message: string): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.execute('updateSource', {
      updatePath: path,
      updateSources: [{
        path,
        line,
        column,
        add,
        message,
      }],
    }, false, (result: ExecutionResult) => flowResult(this.manageExecuteGoResult(result))));
  }

  *searchFile(this: EditorStore): Generator<Promise<unknown>, void, unknown> {
    if (this.fileSearchCommandLoadingState.isInProgress) {
      return;
    }
    this.fileSearchCommandLoadingState.inProgress();
    this.fileSearchCommandResults = (yield this.applicationStore.client.findFiles(this.fileSearchCommandState.text, this.fileSearchCommandState.isRegExp)) as string[];
    this.fileSearchCommandLoadingState.conclude(true);
  }

  *searchText(this: EditorStore): Generator<Promise<unknown>, void, unknown> {
    if (this.textSearchCommandLoadingState.isInProgress) {
      return;
    }
    this.textSearchCommandLoadingState.inProgress();
    this.openAuxPanel(AUX_PANEL_MODE.SEARCH_RESULT, true);
    try {
      const results = ((yield this.applicationStore.client.searchText(this.textSearchCommandState.text, this.textSearchCommandState.isCaseSensitive, this.textSearchCommandState.isRegExp)) as PlainObject<SearchResultEntry>[]).map(result => getSearchResultEntry(result));
      this.setSearchState(new SearchResultState(this, results));
      this.textSearchCommandLoadingState.conclude(true);
    } catch (e) {
      this.applicationStore.notifyError(e);
      this.textSearchCommandLoadingState.conclude(false);
    }
  }

  *findUsages(this: EditorStore, coordinate: FileCoordinate): Generator<Promise<unknown>, void, unknown> {
    const errorMessage = 'Error finding references. Please make sure that the code compiles and that you are looking for references of non primitive types!';
    let concept: UsageConcept;
    try {
      concept = ((yield this.applicationStore.client.getConceptPath(coordinate.file, coordinate.line, coordinate.column)) as UsageConcept);
    } catch {
      this.applicationStore.notifyWarning(errorMessage);
      return;
    }
    try {
      this.setBlockingAlert({ message: 'Finding concept usages...', prompt: `Finding references of ${getUsageConceptLabel(concept)}`, showLoading: true });
      const usages = ((yield this.applicationStore.client.getUsages(concept.owner
        ? (concept.type
          ? 'meta::ide::findusages::findUsagesForEnum_String_1__String_1__SourceInformation_MANY_'
          : 'meta::ide::findusages::findUsagesForProperty_String_1__String_1__SourceInformation_MANY_')
        : 'meta::ide::findusages::findUsagesForPath_String_1__SourceInformation_MANY_', (concept.owner ? [`'${concept.owner}'`] : []).concat(`'${concept.path}'`))) as PlainObject<Usage>[])
        .map(usage => deserialize(Usage, usage));
      this.setSearchState(new UsageResultState(this, concept, usages));
      this.openAuxPanel(AUX_PANEL_MODE.SEARCH_RESULT, true);
    } catch {
      this.applicationStore.notifyWarning(errorMessage);
    } finally {
      this.setBlockingAlert(undefined);
    }
  }

  *command(this: EditorStore, cmd: () => Promise<PlainObject<CommandResult>>): Generator<Promise<unknown>, boolean, unknown> {
    try {
      const result = deserializeCommandResult((yield cmd()) as Record<PropertyKey, unknown>);
      if (result instanceof CommandFailureResult) {
        if (result.errorDialog) {
          this.applicationStore.notifyWarning(`Error: ${result.text}`);
        } else {
          this.setConsoleText(result.text);
        }
        return false;
      }
      return true;
    } catch (e) {
      this.applicationStore.notifyError(e);
      return false;
    }
  }

  *createNewDirectory(this: EditorStore, path: string): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.command(() => this.applicationStore.client.createFolder(trimPathLeadingSlash(path))));
    yield flowResult(this.directoryTreeState.refreshTreeData());
  }

  *createNewFile(this: EditorStore, path: string): Generator<Promise<unknown>, void, unknown> {
    const result = (yield flowResult(this.command(() => this.applicationStore.client.createFile(trimPathLeadingSlash(path))))) as boolean;
    yield flowResult(this.directoryTreeState.refreshTreeData());
    if (result) {
      yield flowResult(this.loadFile(path));
    }
  }

  private *_deleteDirectoryOrFile(this: EditorStore, path: string): Generator<Promise<unknown>, void, unknown> {
    yield flowResult(this.command(() => this.applicationStore.client.deleteDirectoryOrFile(trimPathLeadingSlash(path))));
    const editorStatesToClose = this.openedEditorStates.filter(state => state instanceof FileEditorState && state.path.startsWith(path));
    editorStatesToClose.forEach(state => this.closeState(state));
    yield flowResult(this.directoryTreeState.refreshTreeData());
  }

  *deleteDirectoryOrFile(this: EditorStore, path: string, isDirectory: boolean, hasChildContent: boolean): Generator<Promise<unknown>, void, unknown> {
    this.setActionAltertInfo({
      message: `Are you sure you would like to delete this ${isDirectory ? 'directory' : 'file'}?`,
      prompt: hasChildContent ? 'Beware! This directory is not empty, this action is not undo-able, you have to manually revert using VCS' : 'Beware! This action is not undo-able, you have to manually revert using VCS',
      type: ActionAlertType.CAUTION,
      onEnter: (): void => this.setBlockGlobalHotkeys(true),
      onClose: (): void => this.setBlockGlobalHotkeys(false),
      actions: [
        {
          label: 'Delete anyway',
          type: ActionAlertActionType.PROCEED_WITH_CAUTION,
          handler: (): Promise<void> => flowResult(this._deleteDirectoryOrFile(path)).catch(this.applicationStore.alertIllegalUnhandledError)
        },
        {
          label: 'Abort',
          type: ActionAlertActionType.PROCEED,
          default: true,
        }
      ],
    });
  }

  createGlobalHotKeyAction = (handler: (event?: KeyboardEvent | undefined) => void): (event: KeyboardEvent | undefined) => void => (event: KeyboardEvent | undefined): void => {
    event?.preventDefault();
    if (!this.blockGlobalHotkeys) { handler(event) }
  }
}

const EditorStoreContext = createContext<EditorStore | undefined>(undefined);

export const EditorStoreProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const applicationStore = useApplicationStore();
  const store = useLocalObservable(() => new EditorStore(applicationStore));
  return <EditorStoreContext.Provider value={store}>{children}</EditorStoreContext.Provider>;
};

export const useEditorStore = (): EditorStore =>
  guaranteeNonNullable(useContext(EditorStoreContext), 'useEditorStore() hook must be used inside EditorStore context provider');
