///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LoginUIState, TenantUIState } from '@core/ui/tenant-ui.models';


export const selectTenantUIState = createFeatureSelector<AppState, TenantUIState>(
  'tenantUI'
);

export const selectTenantUI = createSelector(
  selectTenantUIState,
  (state: TenantUIState) => state
);

export const selectPlatformName = createSelector(
  selectTenantUIState,
  (state: TenantUIState) => state.platformName
);

export const selectPlatformVersion = createSelector(
  selectTenantUIState,
  (state: TenantUIState) => state.platformVersion
);

export const selectShowNameVersion = createSelector(
  selectTenantUIState,
  (state: TenantUIState) => state.showNameVersion
);

export const selectLoginUIState = createFeatureSelector<AppState, LoginUIState>(
  'loginUI'
);
export const selectLoginUI = createSelector(
  selectLoginUIState,
  (state: LoginUIState) => state
);
