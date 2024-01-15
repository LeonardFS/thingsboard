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

export interface TenantUIState {
  applicationTitle?: string;
  iconImageUrl?: string;
  logoImageUrl?: string;
  logoImageHeight?: string;
  platformMainColor?: string;
  platformTextMainColor?: string;
  platformButtonColor?: string;
  platformMenuColorActive?: string;
  platformMenuColorHover?: string;
  showNameVersion: boolean;
  platformName?: string;
  platformVersion?: string;
  iconsColor?: string;
  customCss?: string;
}

export interface LoginUIState{
  loginDomainName?: string;
  loginAppTitle?: string;
  loginIconImageUrl?: string;
  loginBGImage?: string;
  loginLogoImageUrl?: string;
  loginLogoImageHeight?: string;
  loginBGColor?: string;
  loginFormBGColor?: string;
  loginFormTextColor?: string;
  loginFormIconColor?: string;
  loginFormInputColor?: string;
  loginButtonColor?: string;
  loginButtonTextColor?: string;
}
