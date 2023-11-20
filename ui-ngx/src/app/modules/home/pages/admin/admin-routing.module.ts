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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';

import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { GeneralSettingsComponent } from '@modules/home/pages/admin/general-settings.component';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { OAuth2SettingsComponent } from '@home/pages/admin/oauth2-settings.component';
import { Observable } from 'rxjs';
import { OAuth2Service } from '@core/http/oauth2.service';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { HomeSettingsComponent } from '@home/pages/admin/home-settings.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { ResourcesLibraryTableConfigResolver } from '@home/pages/admin/resource/resources-library-table-config.resolve';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { QueuesTableConfigResolver } from '@home/pages/admin/queue/queues-table-config.resolver';
import { RepositoryAdminSettingsComponent } from '@home/pages/admin/repository-admin-settings.component';
import { AutoCommitAdminSettingsComponent } from '@home/pages/admin/auto-commit-admin-settings.component';
import { TwoFactorAuthSettingsComponent } from '@home/pages/admin/two-factor-auth-settings.component';
import { widgetsLibraryRoutes } from '@home/pages/widget/widget-library-routing.module';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { auditLogsRoutes } from '@home/pages/audit-log/audit-log-routing.module';
import { CustomUiComponent } from '../custom-ui/custom-ui.component';
import { LoginUiComponent } from '../login-ui/login-ui.component';

@Injectable()
export class OAuth2LoginProcessingUrlResolver implements Resolve<string> {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
  }
}

const routes: Routes = [
  {
    path: 'resources',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'admin.resources',
        icon: 'folder'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: '/resources/widgets-library'
        }
      },
      ...widgetsLibraryRoutes,
      {
        path: 'resources-library',
        data: {
          breadcrumb: {
            label: 'resource.resources-library',
            icon: 'mdi:rhombus-split'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library',
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'mdi:rhombus-split'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library'
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          }
        ]
      }
    ]
  },
  {
    path: 'settings',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'admin.settings',
        icon: 'settings'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: {
            SYS_ADMIN: '/settings/general',
            TENANT_ADMIN: '/settings/home'
          }
        }
      },
      {
        path: 'general',
        component: GeneralSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.general-settings',
          breadcrumb: {
            label: 'admin.general',
            icon: 'settings_applications'
          }
        }
      },
      {
        path: 'outgoing-mail',
        component: MailServerComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.outgoing-mail-settings',
          breadcrumb: {
            label: 'admin.outgoing-mail',
            icon: 'mail'
          }
        }
      },
      {
        path: 'notifications',
        component: SmsProviderComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.notifications-settings',
          breadcrumb: {
            label: 'admin.notifications',
            icon: 'mdi:message-badge'
          }
        }
      },
      {
        path: 'security-settings',
        component: SecuritySettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.security-settings',
          breadcrumb: {
            label: 'admin.security-settings',
            icon: 'security'
          }
        }
      },
      {
        path: 'oauth2',
        component: OAuth2SettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.oauth2.oauth2',
          breadcrumb: {
            label: 'admin.oauth2.oauth2',
            icon: 'security'
          }
        },
        resolve: {
          loginProcessingUrl: OAuth2LoginProcessingUrlResolver
        }
      },
      {
        path: 'home',
        component: HomeSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.home-settings',
          breadcrumb: {
            label: 'admin.home-settings',
            icon: 'settings_applications'
          }
        }
      },
      {
        path: 'custom-ui',
        component: CustomUiComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'system-settings.custom-ui',
          breadcrumb: {
            label: 'system-settings.custom-ui',
            icon: 'color_lens'
          }
        }
      },
      {
        path: 'login-ui',
        component: LoginUiComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'system-settings.login-ui',
          breadcrumb: {
            label: 'system-settings.login-ui',
            icon: 'picture_in_picture_alt'
          }
        }
      },
      {
        path: 'resources-library',
        data: {
          breadcrumb: {
            label: 'resource.resources-library',
            icon: 'folder'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library',
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'folder'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library'
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'queues',
        data: {
          breadcrumb: {
            label: 'admin.queues',
            icon: 'swap_calls'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.SYS_ADMIN],
              title: 'admin.queues'
            },
            resolve: {
              entitiesTableConfig: QueuesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'swap_calls'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN],
              title: 'admin.queues'
            },
            resolve: {
              entitiesTableConfig: QueuesTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'home',
        component: HomeSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.home-settings',
          breadcrumb: {
            label: 'admin.home',
            icon: 'settings_applications'
          }
        }
      },
      {
        path: 'repository',
        component: RepositoryAdminSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.repository-settings',
          breadcrumb: {
            label: 'admin.repository',
            icon: 'manage_history'
          }
        }
      },
      {
        path: 'auto-commit',
        component: AutoCommitAdminSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.auto-commit-settings',
          breadcrumb: {
            label: 'admin.auto-commit',
            icon: 'settings_backup_restore'
          }
        }
      },
      {
        path: 'security-settings',
        redirectTo: '/security-settings/general'
      },
      {
        path: 'oauth2',
        redirectTo: '/security-settings/oauth2'
      },
      {
        path: 'resources-library',
        pathMatch: 'full',
        redirectTo: '/resources/resources-library'
      },
      {
        path: 'resources-library/:entityId',
        redirectTo: '/resources/resources-library/:entityId'
      },
      {
        path: '2fa',
        redirectTo: '/security-settings/2fa'
      },
      {
        path: 'sms-provider',
        redirectTo: '/settings/notifications'
      }
    ]
  },
  {
    path: 'security-settings',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'security.security',
        icon: 'security'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: {
            SYS_ADMIN: '/security-settings/general',
            TENANT_ADMIN: '/security-settings/auditLogs'
          }
        }
      },
      {
        path: 'general',
        component: SecuritySettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.general',
          breadcrumb: {
            label: 'admin.general',
            icon: 'settings_applications'
          }
        }
      },
      {
        path: '2fa',
        component: TwoFactorAuthSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.2fa.2fa',
          breadcrumb: {
            label: 'admin.2fa.2fa',
            icon: 'mdi:two-factor-authentication'
          }
        }
      },
      {
        path: 'oauth2',
        component: OAuth2SettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.oauth2.oauth2',
          breadcrumb: {
            label: 'admin.oauth2.oauth2',
            icon: 'mdi:shield-account'
          }
        },
        resolve: {
          loginProcessingUrl: OAuth2LoginProcessingUrlResolver
        }
      },
      ...auditLogsRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    OAuth2LoginProcessingUrlResolver,
    ResourcesLibraryTableConfigResolver,
    QueuesTableConfigResolver
  ]
})
export class AdminRoutingModule { }
