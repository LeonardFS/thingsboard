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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import { DashboardService } from '@core/http/dashboard.service';
import { UIInfo } from '@shared/models/dashboard.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionTenantUIChangeAll } from '@core/ui/tenant-ui.actions';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { TenantUIState } from '@core/ui/tenant-ui.models';
import { PageComponent } from '@shared/components/page.component';
import { initialState } from '@core/ui/tenant-ui.reducer';
import { isDefined } from '@app/core/utils';

@Component({
  selector: 'tb-custom-ui',
  templateUrl: './custom-ui.component.html',
  styleUrls: ['./custom-ui.component.scss']
})
export class CustomUiComponent extends PageComponent implements OnInit, HasDirtyFlag,AfterViewInit {
  isDirty = false;
  faviconMaxKBytes = 256;
  logoMaxKBytes = 4096;
  customUiFormGroup: FormGroup;
  initData: any;
  previousData: any;

  constructor(
    protected store: Store<AppState>,
    private translate: TranslateService,
    private dashboardService: DashboardService,
    private fb: FormBuilder
  ) {
    super(store);
    this.initForm();
    this.writeFormByHttp();
  }

  ngAfterViewInit() {
  }

  ngOnInit(): void {
    this.customUiFormGroup.valueChanges.subscribe(data => {
      Reflect.ownKeys(data).forEach(key => data[key.toString()] = data[key.toString()] === '' ? null : data[key.toString()]);
      if(JSON.stringify(this.initData) !== JSON.stringify(data)){
        this.isDirty = true;
        this.previousData = data;
        this.store.dispatch(new ActionTenantUIChangeAll(data));
      }else{
        this.isDirty = false;
        if(JSON.stringify(this.previousData) !== JSON.stringify(data)){
          this.store.dispatch(new ActionTenantUIChangeAll(data));
        }
      }
    });
  }

  writeFormByHttp() {
    this.dashboardService.getTenantUIInfo().subscribe(ui => {
      this.patchFormValue(ui);
      this.initData = this.customUiFormGroup.value;
      this.previousData = this.customUiFormGroup.value;
    });
  }

  patchFormValue(ui: UIInfo | TenantUIState) {
    this.customUiFormGroup.get('applicationTitle').patchValue(ui.applicationTitle);
    this.customUiFormGroup.get('iconImageUrl').patchValue(ui.iconImageUrl);
    this.customUiFormGroup.get('logoImageUrl').patchValue(ui.logoImageUrl);
    this.customUiFormGroup.get('logoImageHeight').patchValue(ui.logoImageHeight);
    this.customUiFormGroup.get('platformMainColor').patchValue(ui.platformMainColor);
    this.customUiFormGroup.get('platformTextMainColor').patchValue(ui.platformTextMainColor);
    this.customUiFormGroup.get('platformButtonColor').patchValue(ui.platformButtonColor);
    this.customUiFormGroup.get('platformMenuColorActive').patchValue(ui.platformMenuColorActive);
    this.customUiFormGroup.get('platformMenuColorHover').patchValue(ui.platformMenuColorHover);
    this.customUiFormGroup.get('showNameVersion').patchValue(ui.showNameVersion);
    this.customUiFormGroup.get('platformName').patchValue(ui.platformName);
    this.customUiFormGroup.get('platformVersion').patchValue(ui.platformVersion);
    this.customUiFormGroup.get('iconsColor').patchValue(ui.iconsColor);
    this.customUiFormGroup.get('customCss').patchValue(ui.customCss);
  }

  reset($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.patchFormValue(initialState);
    this.store.dispatch(new ActionTenantUIChangeAll(this.customUiFormGroup.value));
    this.isDirty = true;
  }

  cancel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.writeFormByHttp();
  }

  initForm() {
    this.customUiFormGroup = this.fb.group({
      applicationTitle: [null, []],
      iconImageUrl: [null, []],
      logoImageUrl: [null, []],
      logoImageHeight: [null, []],
      platformMainColor: [null, []],
      platformTextMainColor: [null, []],
      platformButtonColor: [null, []],
      platformMenuColorActive: [null, []],
      platformMenuColorHover: [null, []],
      showNameVersion: [false, []],
      platformName: [env.appTitle, []],
      platformVersion: [env.tbVersion, []],
      iconsColor: [null, []],
      customCss: ['', []]
    });
    this.initData = this.customUiFormGroup.value;
    this.previousData = this.customUiFormGroup.value;
  }

  submit($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dashboardService.saveTenantUIInfo(this.customUiFormGroup.value as UIInfo).subscribe(res => {
    });
    this.store.dispatch(new ActionTenantUIChangeAll(this.customUiFormGroup.value));
    this.isDirty = false;
  }

  formatSlider(value: number) {
    return value + 'px';
  }

}
