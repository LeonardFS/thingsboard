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

import 'hammerjs';

import { Component, OnInit } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService } from '@ngx-translate/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, skip } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { svgIcons, svgIconsUrl } from '@shared/models/icon.models';
import cssjs from '@core/css/css';
import { hashCode } from '@core/utils';
import { DashboardService } from '@core/http/dashboard.service';
import { ActionTenantUIChangeAll } from '@core/ui/tenant-ui.actions';
import { selectLoginUI, selectTenantUI } from '@core/ui/tenant-ui.selectors';
import { TitleService } from '@core/services/title.service';
import { Router } from '@angular/router';
import { TenantUIState } from '@core/ui/tenant-ui.models';
import { Authority } from '@shared/models/authority.enum';
//getCurrentAuthUser依赖是新增,其他俩个是原有的
import { getCurrentAuthUser, selectIsAuthenticated, selectIsUserLoaded } from '@core/auth/auth.selectors';



@Component({
  selector: 'tb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService,
              private dashboardService: DashboardService,
                private titleService: TitleService,
                private router: Router) {

    console.log(`ThingsBoard Version: ${env.tbVersion}`);

    this.matIconRegistry.addSvgIconResolver((name, namespace) => {
      if (namespace === 'mdi') {
        return this.domSanitizer.bypassSecurityTrustResourceUrl(`./assets/mdi/${name}.svg`);
      } else {
        return null;
      }
    });

    for (const svgIcon of Object.keys(svgIcons)) {
      this.matIconRegistry.addSvgIconLiteral(
        svgIcon,
        this.domSanitizer.bypassSecurityTrustHtml(
          svgIcons[svgIcon]
        )
      );
    }

    for (const svgIcon of Object.keys(svgIconsUrl)) {
      this.matIconRegistry.addSvgIcon(svgIcon, this.domSanitizer.bypassSecurityTrustResourceUrl(svgIconsUrl[svgIcon]));
    }

    this.storageService.testLocalStorage();

    this.setupTranslate();
    this.setupAuth();
  }

  setupTranslate() {
    if (!env.production) {
      console.log(`Supported Langs: ${env.supportedLangs}`);
    }
    this.translate.addLangs(env.supportedLangs);
    if (!env.production) {
      console.log(`Default Lang: ${env.defaultLang}`);
    }
    this.translate.setDefaultLang(env.defaultLang);
  }

  setupAuth() {
    combineLatest([
      this.store.pipe(select(selectIsAuthenticated)),
      this.store.pipe(select(selectIsUserLoaded))]
    ).pipe(
      map(results => ({isAuthenticated: results[0], isUserLoaded: results[1]})),
      distinctUntilChanged(),
      filter((data) => data.isUserLoaded ),
      skip(1),
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
    this.authService.reloadUser();
  }

  ngOnInit() {
     //订阅ui状态更改
     this.store.pipe(select(selectTenantUI)).subscribe(ui => {
      this.changeCss(ui);
      if (ui) {
        this.changeIcon(ui.iconImageUrl);
        this.changeTitle(ui.applicationTitle);
      }
    });
    //初始化ui信息
    this.store.pipe(select(selectIsAuthenticated)).subscribe( isAuthed => {
      if(isAuthed){
        if(getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN){
          this.dashboardService.getTenantUIInfo().subscribe(ui => {
            this.store.dispatch(new ActionTenantUIChangeAll(ui));
          });
        }
      }
    });

    this.store.pipe(select(selectLoginUI)).subscribe(ui => {
      if (ui) {
        this.changeIcon(ui.loginIconImageUrl);
        this.changeTitle(ui.loginAppTitle);
      }
    });
  }

  onActivateComponent($event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

  changeCss(ui: TenantUIState) {
    let css = '.tb-default .mat-toolbar.mat-primary{';
    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'global-ui-css-' + hashCode(css);
    cssParser.cssPreviewNamespace = namespace;
    if (ui.platformMainColor) {
      css = css + 'background-color: ' + ui.platformMainColor + ' !important;';
    }
    if (ui.platformTextMainColor) {
      css = css + 'color: ' + ui.platformTextMainColor + ' !important;';
    }
    css = css + '}';
    if (ui.platformButtonColor) {
      css = css + '.tb-default .mat-raised-button.mat-primary{background-color: ' + ui.platformButtonColor + ' !important;}';
    }
    if (ui.platformMenuColorActive) {
      css = css + '.tb-default li.ng-star-inserted .mat-button.tb-active {background-color: ' + ui.platformMenuColorActive + ';}';
    }
    if (ui.platformMenuColorHover) {
      css = css + '.tb-default li.ng-star-inserted .mat-button:hover {background-color: ' + ui.platformMenuColorHover + ';}';
    }
    cssParser.createStyleElement(namespace, css, 'nonamespace');
  }

  changeTitle(title: string) {
    if (title) {
      env.appTitle = title;
    } else {
      env.appTitle = 'ThingsBoard';
    }
    this.titleService.setTitle(
      this.router.routerState.snapshot.root,
      this.translate
    );
  }

  //改变应用图标
  changeIcon(icon: string) {
    const __el = document.getElementById('custom-icon');
    if (__el) {
      __el.parentNode.removeChild(__el);
    }
    const link = document.createElement('link');
    link.type = 'image/x-icon';
    link.id = 'custom-icon';
    link.rel = 'icon';
    if (icon) {
      link.href = icon;
    } else {
      link.href = ' thingsboard.ico';
    }
    document.getElementsByTagName('head')[0].appendChild(link);
  }

}
