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

import { Component, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store, select } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Constants } from '@shared/models/constants';
import { Router } from '@angular/router';
import { OAuth2ClientInfo } from '@shared/models/oauth2.models';
import { ActionLoginUIChange, ActionTenantUIChangeAll } from '@core/ui/tenant-ui.actions';
import { DashboardService } from '@core/http/dashboard.service';
import { selectAuthUser,selectIsUserLoaded } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, skip } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
  selector: 'tb-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent extends PageComponent implements OnInit {

  bgImage: string;
  bgColor: string;
  buttonBGColor: string;
  buttonTextColor: string;
  logo : string | SafeUrl;
  formBGColor: string;
  formTextColor: string;
  formIconColor: string;
  formInputColor: string;
  logoHeight: string;

  loginFormGroup = this.fb.group({
    username: '',
    password: ''
  });
  oauth2Clients: Array<OAuth2ClientInfo> = null;

  constructor(protected store: Store<AppState>,
              private authService: AuthService,
              public fb: UntypedFormBuilder,
              private router: Router,
              private dashboardService: DashboardService,
              private sanitizer: DomSanitizer) {
    super(store);
    this.logo = 'assets/logo_title_white.svg';
    this.dashboardService.getTenantLoginUIInfo(document.domain).subscribe(ui => {
      if(ui){
        this.bgImage = ui.loginBGImage ? ui.loginBGImage : '';
        this.bgColor = ui.loginBGColor ? ui.loginBGColor : '';
        this.buttonBGColor = ui.loginButtonColor ? ui.loginButtonColor : '';
        this.buttonTextColor = ui.loginButtonTextColor ? ui.loginButtonTextColor :'';
        this.logo = ui.loginLogoImageUrl ? this.sanitizer.bypassSecurityTrustUrl(ui.loginLogoImageUrl) :this.logo;
        this.formBGColor = ui.loginFormBGColor ? ui.loginFormBGColor : '';
        this.formTextColor = ui.loginFormTextColor ? ui.loginFormTextColor : '';
        this.formIconColor = ui.loginFormIconColor ? ui.loginFormIconColor : '';
        this.formInputColor = ui.loginFormInputColor ? ui.loginFormInputColor : '';
        this.logoHeight = ui.loginLogoImageHeight ? ui.loginLogoImageHeight : '60';
        this.store.dispatch(new ActionLoginUIChange(ui));
      }
    });
  }

  ngOnInit() {
    this.oauth2Clients = this.authService.oauth2Clients;
  }

  getBgImage(){
    if(this.bgImage){
      return 'url(' + this.bgImage + ')';
    }
    return null;
  }

  login(): void {
    if (this.loginFormGroup.valid) {
      this.authService.login(this.loginFormGroup.value).subscribe(
        () => {
          combineLatest([
            this.store.pipe(select(selectAuthUser)),
            this.store.pipe(select(selectIsUserLoaded))]
          ).pipe(
            map(results => ({ authUser: results[0], isUserLoaded: results[1] })),
            distinctUntilChanged(),
            filter((data) => data.isUserLoaded),
            skip(1)
          ).subscribe((data) => {
            if(data.authUser.authority === Authority.TENANT_ADMIN){
              this.dashboardService.getTenantUIInfo().subscribe(ui => {
                this.store.dispatch(new ActionTenantUIChangeAll(ui));
              });
            }
          });
        },
        (error: HttpErrorResponse) => {
          if (error && error.error && error.error.errorCode) {
            if (error.error.errorCode === Constants.serverErrorCode.credentialsExpired) {
              this.router.navigateByUrl(`login/resetExpiredPassword?resetToken=${error.error.resetToken}`);
            }
          }
        }
      );
    } else {
      Object.keys(this.loginFormGroup.controls).forEach(field => {
        const control = this.loginFormGroup.get(field);
        control.markAsTouched({onlySelf: true});
      });
    }
  }

  getOAuth2Uri(oauth2Client: OAuth2ClientInfo): string {
    let result = "";
    if (this.authService.redirectUrl) {
      result += "?prevUri=" + this.authService.redirectUrl;
    }
    return oauth2Client.url + result;
  }
}
