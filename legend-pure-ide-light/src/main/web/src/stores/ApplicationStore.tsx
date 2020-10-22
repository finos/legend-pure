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
import type { SuperGenericFunction } from 'Utilities/GeneralUtil';
import { guaranteeNonNullable, isString, ApplicationError } from 'Utilities/GeneralUtil';
import { makeAutoObservable } from 'mobx';
import { Logger, LOG_EVENT } from 'Utilities/Logger';
import { useLocalObservable } from 'mobx-react-lite';
import { NetworkClient } from 'Utilities/NetworkClient';
import { PureClient } from 'Stores/PureClient';

export enum ActionAlertType {
  STANDARD = 'STANDARD',
  CAUTION = 'CAUTION',
}

export enum ActionAlertActionType {
  STANDARD = 'STANDARD',
  PROCEED_WITH_CAUTION = 'PROCEED_WITH_CAUTION',
  PROCEED = 'PROCEED',
}

export interface ActionAlertInfo {
  title?: string;
  message: string;
  prompt?: string;
  type?: ActionAlertType;
  onClose?: () => void;
  onEnter?: () => void;
  actions: {
    label: string;
    default?: boolean;
    handler?: () => void; // default to dismiss
    type?: ActionAlertActionType;
  }[];
}

export interface BlockingAlertInfo {
  message: string;
  prompt?: string;
  showLoading?: boolean;
}

export const DEFAULT_NOTIFICATION_HIDE_TIME = 6000; // ms
export const DEFAULT_ERROR_NOTIFICATION_HIDE_TIME = 10000; // ms

export enum NOTIFCATION_SEVERITY {
  ILEGAL_STATE = 'ILEGAL_STATE', // highest priority since this implies bugs - we expect user to never see this
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  SUCCESS = 'SUCCESS',
  INFO = 'INFO',
}

export interface NotificationAction {
  icon: React.ReactNode;
  action: () => void;
}

export class Notification {
  severity: NOTIFCATION_SEVERITY;
  message: string;
  actions: NotificationAction[];
  autoHideDuration?: number;

  constructor(severity: NOTIFCATION_SEVERITY, message: string, actions: NotificationAction[], autoHideDuration: number | undefined) {
    this.severity = severity;
    this.message = message;
    this.actions = actions;
    this.autoHideDuration = autoHideDuration;
  }
}

export class ApplicationStore {
  coreClient: NetworkClient;
  client: PureClient;
  notification?: Notification;
  logger: Logger;
  blockingAlertInfo?: BlockingAlertInfo;
  actionAlertInfo?: ActionAlertInfo;

  constructor() {
    makeAutoObservable(this);
    this.coreClient = new NetworkClient({});
    this.client = new PureClient(this.coreClient);
    this.logger = new Logger();
  }

  setBlockingAlert(alertInfo: BlockingAlertInfo | undefined): void { this.blockingAlertInfo = alertInfo }
  setActionAltertInfo(alertInfo: ActionAlertInfo | undefined): void {
    if (this.actionAlertInfo && alertInfo) { this.notifyIllegalState('Action alert is stacked: new alert is invoked while another one is being displayed') }
    this.actionAlertInfo = alertInfo;
  }

  setNotification(notification: Notification | undefined): void { this.notification = notification }
  notify(message: string, actions?: NotificationAction[], autoHideDuration?: number): void { this.setNotification(new Notification(NOTIFCATION_SEVERITY.INFO, message, actions ?? [], autoHideDuration ?? DEFAULT_NOTIFICATION_HIDE_TIME)) }
  notifySuccess(message: string, actions?: NotificationAction[], autoHideDuration?: number): void { this.setNotification(new Notification(NOTIFCATION_SEVERITY.SUCCESS, message, actions ?? [], autoHideDuration ?? DEFAULT_NOTIFICATION_HIDE_TIME)) }
  notifyWarning(message: string, actions?: NotificationAction[], autoHideDuration?: number): void { this.setNotification(new Notification(NOTIFCATION_SEVERITY.WARNING, message, actions ?? [], autoHideDuration ?? DEFAULT_NOTIFICATION_HIDE_TIME)) }
  notifyIllegalState(message: string, actions?: NotificationAction[], autoHideDuration?: number): void { this.setNotification(new Notification(NOTIFCATION_SEVERITY.ILEGAL_STATE, isString(message) ? `[PLEASE NOTIFY DEVELOPER] ${message}` : message, actions ?? [], autoHideDuration ?? DEFAULT_ERROR_NOTIFICATION_HIDE_TIME)) }

  notifyError(
    content: unknown,
    actions?: NotificationAction[],
    autoHideDuration?: number
  ): void {
    let message: string | undefined;
    if (content instanceof Error || content instanceof ApplicationError) {
      message = content.message;
    } else if (isString(content)) {
      message = content;
    } else {
      message = undefined;
      this.logger.error(LOG_EVENT.ILLEGAL_APPLICATION_STATE_OCCURRED, 'Unable to display error in notification', message);
      this.notifyIllegalState('Unable to display error');
    }
    if (message) {
      this.logger.error(LOG_EVENT.NONE, content);
      this.setNotification(new Notification(NOTIFCATION_SEVERITY.ERROR, message, actions ?? [], autoHideDuration ?? DEFAULT_ERROR_NOTIFICATION_HIDE_TIME));
    }
  }

  // WIP: to be removed when we complete feature set
  notifyUnsupportedFeature(message: string): void {
    this.notifyWarning(`Unsupported feature: ${message}`);
  }

  /**
   * This function creates a more user-friendly way to throw error in the UI. Rather than crashing the whole app, we will
   * just notify and replacing the value should get with an alternative (e.g. `undefined`). A good use-case for this
   * is where we would not expect an error to throw (i.e. `IllegalStateError`), but we want to be sure that if the error
   * ever occurs, it still shows very apparently in the UI, as such, printing out in the console is not good enough,
   * but crashing the app is bad too, so this is a good balance.
   */
  notifyAndReturnAlternativeOnError = <T extends SuperGenericFunction, W>(fn: T, alternative?: W): ReturnType<T> | W | undefined => {
    try {
      return fn();
    } catch (error) {
      this.notifyIllegalState(error.message);
      return alternative;
    }
  }

  /**
   * When we call store/state functions from the component, we should handle error thrown at these functions instead
   * of throwing them to the UI. This enforces that by throwing `IllegalStateError`
   */
  alertIllegalUnhandledError = (error: Error): void => {
    this.logger.error(LOG_EVENT.ILLEGAL_APPLICATION_STATE_OCCURRED, 'Encountered unhandled rejection in component', error);
    this.notifyIllegalState(error.message);
  }

  /**
   * Guarantee that the action being used by the component does not throw unhandled errors
   */
  guaranteeSafeAction = (action: () => Promise<void>): () => Promise<void> => (): Promise<void> => action().catch(this.alertIllegalUnhandledError);
}

const ApplicationStoreContext = createContext<ApplicationStore | undefined>(undefined);

export const ApplicationStoreProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const applicationStore = useLocalObservable(() => new ApplicationStore());
  return <ApplicationStoreContext.Provider value={applicationStore}>{children}</ApplicationStoreContext.Provider>;
};

export const useApplicationStore = (): ApplicationStore =>
  guaranteeNonNullable(useContext(ApplicationStoreContext), 'useApplicationStore() hook must be used inside ApplicationStore context provider');
