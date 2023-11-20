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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { DashboardService } from '@core/http/dashboard.service';
import { LoginUIInfo, UIInfo } from '@shared/models/dashboard.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { LoginUIState } from '@core/ui/tenant-ui.models';
import { PageComponent } from '@shared/components/page.component';
import { initialLoginUIState } from '@core/ui/tenant-ui.reducer';
import { ActionLoginUIChange } from '@core/ui/tenant-ui.actions';
import { AttributeService } from '@core/http/attribute.service';
import { TenantId } from '@shared/models/id/tenant-id';
import { selectUserDetails } from '@core/auth/auth.selectors';
@Component({
  selector: 'tb-login-ui',
  templateUrl: './login-ui.component.html',
  styleUrls: ['./login-ui.component.scss']
})
export class LoginUiComponent extends PageComponent implements OnInit, HasDirtyFlag, AfterViewInit {

  isDirty = false;
  bgMaxKBytes = 10240;
  faviconMaxKBytes = 256;
  logoMaxKBytes = 4096;
  loginUiFormGroup: FormGroup;
  tenantId: TenantId;

  constructor(
    protected store: Store<AppState>,
    private translate: TranslateService,
    private dashboardService: DashboardService,
    private attributeService: AttributeService,
    private fb: FormBuilder
  ) {
    super(store);
    this.initForm();
    this.store.pipe(select(selectUserDetails)).subscribe(user => {
      if(user){
        this.tenantId = user.tenantId;
        this.writeFormByHttp();
      }
    });
  }

  ngAfterViewInit() {
  }

  ngOnInit(): void {
    this.loginUiFormGroup.valueChanges.subscribe(data => {
      this.isDirty = true;
      Reflect.ownKeys(data).forEach(key => data[key.toString()] = data[key.toString()] === '' ? null : data[key.toString()]);
      this.store.dispatch(new ActionLoginUIChange(data));
    });
  }

  writeFormByHttp() {
    this.dashboardService.getTenantLoginUIInfo(undefined,this.tenantId.id).subscribe(ui => this.patchFormValue(ui));
  }

  patchFormValue(ui: LoginUIInfo | LoginUIState) {
    this.loginUiFormGroup.get('loginDomainName').patchValue(ui.loginDomainName);
    this.loginUiFormGroup.get('loginAppTitle').patchValue(ui.loginAppTitle);
    this.loginUiFormGroup.get('loginIconImageUrl').patchValue(ui.loginIconImageUrl);
    this.loginUiFormGroup.get('loginBGImage').patchValue(ui.loginBGImage);
    this.loginUiFormGroup.get('loginLogoImageUrl').patchValue(ui.loginLogoImageUrl);
    this.loginUiFormGroup.get('loginLogoImageHeight').patchValue(ui.loginLogoImageHeight);
    this.loginUiFormGroup.get('loginBGColor').patchValue(ui.loginBGColor);
    this.loginUiFormGroup.get('loginFormBGColor').patchValue(ui.loginFormBGColor);
    this.loginUiFormGroup.get('loginFormTextColor').patchValue(ui.loginFormTextColor);
    this.loginUiFormGroup.get('loginFormIconColor').patchValue(ui.loginFormIconColor);
    this.loginUiFormGroup.get('loginFormInputColor').patchValue(ui.loginFormInputColor);
    this.loginUiFormGroup.get('loginButtonColor').patchValue(ui.loginButtonColor);
    this.loginUiFormGroup.get('loginButtonTextColor').patchValue(ui.loginButtonTextColor);
  }

  //恢复到tb原始设置
  reset($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.patchFormValue(initialLoginUIState);
    this.store.dispatch(new ActionLoginUIChange(this.loginUiFormGroup.value));
    this.isDirty = true;
  }

  //撤销本次操作
  cancel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.writeFormByHttp();
  }

  //初始化表单
  initForm() {
    this.loginUiFormGroup = this.fb.group({
      loginDomainName: [null, [Validators.required]],
      loginAppTitle: [null, []],
      loginIconImageUrl: [null, []],
      loginBGImage: [null, []],
      loginLogoImageUrl: [null, []],
      loginLogoImageHeight: [null, []],
      loginBGColor: [null, []],
      loginFormBGColor: [null, []],
      loginFormTextColor: [false, []],
      loginFormIconColor: [false, []],
      loginFormInputColor: [false, []],
      loginButtonColor: [null, []],
      loginButtonTextColor: [null, []]
    });
  }

  submit($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dashboardService.saveTenantLoginUIInfo(this.loginUiFormGroup.value as LoginUIInfo).subscribe();
    this.store.dispatch(new ActionLoginUIChange(this.loginUiFormGroup.value));
    this.isDirty = false;
  }

  formatSlider(value: number) {
    return value + 'px';
  }
}
