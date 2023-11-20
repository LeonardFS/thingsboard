///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Action } from '@ngrx/store';
import { TenantUIState } from '@core/ui/tenant-ui.models';
import { LoginUIState } from '@core/ui/tenant-ui.models';

export enum TenantUIActionTypes {
  CHANGE = '[TenantUI] Change',
}

export class ActionTenantUIChangeAll implements Action {
  readonly type = TenantUIActionTypes.CHANGE;

  constructor(readonly state: TenantUIState) {
  }
}

export type TenantUIActions =
  | ActionTenantUIChangeAll;

  export enum LoginUIActionTypes {
    CHANGE = '[LoginUI] Change LOGIN UI',
  }
  export class ActionLoginUIChange implements Action {
    readonly type = LoginUIActionTypes.CHANGE;

    constructor(readonly state: LoginUIState) {
    }
  }

  export type LoginUIActions =
    | ActionLoginUIChange;
