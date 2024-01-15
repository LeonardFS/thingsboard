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


import {environment as env} from '@env/environment.prod';
import { LoginUIState, TenantUIState } from '@core/ui/tenant-ui.models';
import { LoginUIActions, LoginUIActionTypes, TenantUIActions, TenantUIActionTypes } from '@core/ui/tenant-ui.actions';

export const initialState: TenantUIState = {
  applicationTitle: 'ThingsBoard',
  iconImageUrl: null,
  logoImageUrl: null,
  logoImageHeight: null,
  platformMainColor: null,
  platformTextMainColor: null,
  platformMenuColorActive: null,
  platformMenuColorHover: null,
  platformButtonColor: null,
  iconsColor: null,
  customCss: null,
  showNameVersion: false,
  platformName: env.appTitle,
  platformVersion: env.tbVersion
};

export function tenantUIReducer(
  state: TenantUIState = initialState,
  action: TenantUIActions
): TenantUIState {
  switch (action.type) {
    case TenantUIActionTypes.CHANGE:
      return { ...state, ...action.state  };
    default:
      return state;
  }
}

export const initialLoginUIState: LoginUIState = {
  loginDomainName: null,
  loginAppTitle: null,
  loginIconImageUrl: null,
  loginBGImage: null,
  loginLogoImageUrl: null,
  loginLogoImageHeight: null,
  loginBGColor: null,
  loginFormBGColor: null,
  loginFormTextColor: null,
  loginFormIconColor: null,
  loginFormInputColor: null,
  loginButtonColor: null,
  loginButtonTextColor: null,
};

export function loginUIReducer(
  state: LoginUIState = initialLoginUIState,
  action: LoginUIActions
): LoginUIState {
  switch (action.type) {
    case LoginUIActionTypes.CHANGE:
      return { ...state, ...action.state  };
    default:
      return state;
  }
}
