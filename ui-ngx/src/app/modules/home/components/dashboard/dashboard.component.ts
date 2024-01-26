///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DoCheck,
  Input,
  IterableDiffers,
  KeyValueDiffers,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Timewindow, toHistoryTimewindow } from '@shared/models/time/time.models';
import { TimeService } from '@core/services/time.service';
import { GridsterComponent, GridsterConfig, GridType } from 'angular-gridster2';
import {
  DashboardCallbacks,
  DashboardWidget,
  DashboardWidgets,
  IDashboardComponent
} from '../../models/dashboard-component.models';
import { ReplaySubject, Subject, Subscription } from 'rxjs';
import { WidgetLayout, WidgetLayouts } from '@shared/models/dashboard.models';
import { DialogService } from '@core/services/dialog.service';
import { animatedScroll, deepClone, isDefined } from '@app/core/utils';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { IAliasController, IStateController } from '@app/core/api/widget-api.models';
import { Widget, WidgetPosition, Datasource, DatasourceData } from '@app/shared/models/widget.models';
import { MatMenuTrigger } from '@angular/material/menu';
import { SafeStyle } from '@angular/platform-browser';
import { distinct } from 'rxjs/operators';
import { ResizeObserver } from '@juggle/resize-observer';
import { UtilsService } from '@core/services/utils.service';
import { WidgetComponentAction, WidgetComponentActionType } from '@home/components/widget/widget-container.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { WidgetContext } from '@home/models/widget-component.models';
import XLSX, { BookType } from 'xlsx';
import _ from 'lodash';
@Component({
  selector: 'tb-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent extends PageComponent implements IDashboardComponent, DoCheck, OnInit, OnDestroy, AfterViewInit, OnChanges {

  authUser: AuthUser;

  @Input()
  widgets: Iterable<Widget>;

  @Input()
  widgetLayouts: WidgetLayouts;

  @Input()
  callbacks: DashboardCallbacks;

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  columns: number;

  @Input()
  margin: number;

  @Input()
  outerMargin: boolean;

  @Input()
  isEdit: boolean;

  @Input()
  autofillHeight: boolean;

  @Input()
  mobileAutofillHeight: boolean;

  @Input()
  mobileRowHeight: number;

  @Input()
  isMobile: boolean;

  @Input()
  isMobileDisabled: boolean;

  @Input()
  isEditActionEnabled: boolean;

  @Input()
  isExportActionEnabled: boolean;

  @Input()
  isRemoveActionEnabled: boolean;

  @Input()
  disableWidgetInteraction = false;

  @Input()
  dashboardStyle: {[klass: string]: any};

  @Input()
  backgroundImage: SafeStyle | string;

  @Input()
  dashboardClass: string;

  @Input()
  ignoreLoading = true;

  @Input()
  dashboardTimewindow: Timewindow;

  @Input()
  parentDashboard?: IDashboardComponent = null;

  @Input()
  popoverComponent?: TbPopoverComponent = null;

  dashboardTimewindowChangedSubject: Subject<Timewindow> = new ReplaySubject<Timewindow>();

  dashboardTimewindowChanged = this.dashboardTimewindowChangedSubject.asObservable().pipe(
    distinct()
  );

  originalDashboardTimewindow: Timewindow;

  gridsterOpts: GridsterConfig;

  isWidgetExpanded = false;
  isMobileSize = false;

  @ViewChild('gridster', {static: true}) gridster: GridsterComponent;

  @ViewChild('dashboardMenuTrigger', {static: true}) dashboardMenuTrigger: MatMenuTrigger;

  dashboardMenuPosition = { x: '0px', y: '0px' };

  dashboardContextMenuEvent: MouseEvent;

  @ViewChild('widgetMenuTrigger', {static: true}) widgetMenuTrigger: MatMenuTrigger;

  widgetMenuPosition = { x: '0px', y: '0px' };

  widgetContextMenuEvent: MouseEvent;

  dashboardWidgets = new DashboardWidgets(this,
    this.differs.find([]).create<Widget>((index, item) => {
      return item;
    }),
    this.kvDiffers.find([]).create<string, WidgetLayout>()
  );

  breakpointObserverSubscription: Subscription;

  private optionsChangeNotificationsPaused = false;

  private gridsterResize$: ResizeObserver;

  constructor(protected store: Store<AppState>,
              public utils: UtilsService,
              private timeService: TimeService,
              private dialogService: DialogService,
              private breakpointObserver: BreakpointObserver,
              private differs: IterableDiffers,
              private kvDiffers: KeyValueDiffers,
              private cd: ChangeDetectorRef,
              private ngZone: NgZone) {
    super(store);
    this.authUser = getCurrentAuthUser(store);
  }

  ngOnInit(): void {
    this.dashboardWidgets.parentDashboard = this.parentDashboard;
    this.dashboardWidgets.popoverComponent = this.popoverComponent;
    if (!this.dashboardTimewindow) {
      this.dashboardTimewindow = this.timeService.defaultTimewindow();
    }
    this.gridsterOpts = {
      gridType: GridType.ScrollVertical,
      keepFixedHeightInMobile: true,
      disableWarnings: false,
      disableAutoPositionOnConflict: false,
      pushItems: false,
      swap: false,
      maxRows: 100,
      minCols: this.columns ? this.columns : 24,
      maxCols: 3000,
      maxItemCols: 1000,
      maxItemRows: 1000,
      maxItemArea: 1000000,
      outerMargin: isDefined(this.outerMargin) ? this.outerMargin : true,
      margin: isDefined(this.margin) ? this.margin : 10,
      minItemCols: 1,
      minItemRows: 1,
      defaultItemCols: 8,
      defaultItemRows: 6,
      resizable: {enabled: this.isEdit},
      draggable: {enabled: this.isEdit},
      itemChangeCallback: item => this.dashboardWidgets.sortWidgets(),
      itemInitCallback: (item, itemComponent) => {
        (itemComponent.item as DashboardWidget).gridsterItemComponent = itemComponent;
      }
    };

    this.updateMobileOpts();

    this.breakpointObserverSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm']).subscribe(
      () => {
        this.updateMobileOpts();
        this.notifyGridsterOptionsChanged();
      }
    );

    this.updateWidgets();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.gridsterResize$) {
      this.gridsterResize$.disconnect();
    }
    if (this.breakpointObserverSubscription) {
      this.breakpointObserverSubscription.unsubscribe();
    }
    this.dashboardTimewindowChangedSubject.complete();
    this.gridster = null;
  }

  ngDoCheck() {
    if (!this.optionsChangeNotificationsPaused) {
      this.dashboardWidgets.doCheck();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    let updateMobileOpts = false;
    let updateLayoutOpts = false;
    let updateEditingOpts = false;
    let updateWidgets = false;
    let updateDashboardTimewindow = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['isMobile', 'isMobileDisabled', 'autofillHeight', 'mobileAutofillHeight', 'mobileRowHeight'].includes(propName)) {
          updateMobileOpts = true;
        } else if (['outerMargin', 'margin', 'columns'].includes(propName)) {
          updateLayoutOpts = true;
        } else if (propName === 'isEdit') {
          updateEditingOpts = true;
        } else if (['widgets', 'widgetLayouts'].includes(propName)) {
          updateWidgets = true;
        } else if (propName === 'dashboardTimewindow') {
          updateDashboardTimewindow = true;
        }
      }
    }
    if (updateWidgets) {
      this.updateWidgets();
    } else if (updateDashboardTimewindow) {
      this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
    }

    if (updateMobileOpts) {
      this.updateMobileOpts();
    }
    if (updateLayoutOpts) {
      this.updateLayoutOpts();
    }
    if (updateEditingOpts) {
      this.updateEditingOpts();
    }
    if (updateMobileOpts || updateLayoutOpts || updateEditingOpts) {
      this.notifyGridsterOptionsChanged();
    }
  }

  private updateWidgets() {
    this.dashboardWidgets.setWidgets(this.widgets, this.widgetLayouts);
    this.dashboardWidgets.doCheck();
  }

  private updateWidgetLayouts() {
    this.dashboardWidgets.widgetLayoutsUpdated();
  }

  ngAfterViewInit(): void {
    this.gridsterResize$ = new ResizeObserver(() => {
      this.onGridsterParentResize();
    });
    this.gridsterResize$.observe(this.gridster.el.parentElement);
  }

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number, persist?: boolean): void {
    this.ngZone.run(() => {
      if (!this.originalDashboardTimewindow && !persist) {
        this.originalDashboardTimewindow = deepClone(this.dashboardTimewindow);
      }
      this.dashboardTimewindow = toHistoryTimewindow(this.dashboardTimewindow,
        startTimeMs, endTimeMs, interval, this.timeService);
      this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
    });
  }

  onResetTimewindow(): void {
    this.ngZone.run(() => {
      if (this.originalDashboardTimewindow) {
        this.dashboardTimewindow = deepClone(this.originalDashboardTimewindow);
        this.originalDashboardTimewindow = null;
        this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
      }
    });
  }

  isAutofillHeight(): boolean {
    if (this.isMobileSize) {
      return isDefined(this.mobileAutofillHeight) ? this.mobileAutofillHeight : false;
    } else {
      return isDefined(this.autofillHeight) ? this.autofillHeight : false;
    }
  }

  openDashboardContextMenu($event: MouseEvent) {
    if (this.callbacks && this.callbacks.prepareDashboardContextMenu) {
      const items = this.callbacks.prepareDashboardContextMenu($event);
      if (items && items.length) {
        $event.preventDefault();
        $event.stopPropagation();
        this.dashboardContextMenuEvent = $event;
        this.dashboardMenuPosition.x = $event.clientX + 'px';
        this.dashboardMenuPosition.y = $event.clientY + 'px';
        this.dashboardMenuTrigger.menuData = { items };
        this.dashboardMenuTrigger.openMenu();
      }
    }
  }

  private openWidgetContextMenu($event: MouseEvent, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.prepareWidgetContextMenu) {
      const items = this.callbacks.prepareWidgetContextMenu($event, widget.widget);
      if (items && items.length) {
        $event.preventDefault();
        $event.stopPropagation();
        this.widgetContextMenuEvent = $event;
        this.widgetMenuPosition.x = $event.clientX + 'px';
        this.widgetMenuPosition.y = $event.clientY + 'px';
        this.widgetMenuTrigger.menuData = { items, widget: widget.widget };
        this.widgetMenuTrigger.openMenu();
      }
    }
  }

  onWidgetFullscreenChanged(expanded: boolean) {
    this.isWidgetExpanded = expanded;
  }

  onWidgetComponentAction(action: WidgetComponentAction, widget: DashboardWidget) {
    const $event = action.event;
    switch (action.actionType) {
      case WidgetComponentActionType.MOUSE_DOWN:
        this.widgetMouseDown($event, widget);
        break;
      case WidgetComponentActionType.CLICKED:
        this.widgetClicked($event, widget);
        break;
      case WidgetComponentActionType.CONTEXT_MENU:
        this.openWidgetContextMenu($event, widget);
        break;
      case WidgetComponentActionType.EDIT:
        this.editWidget($event, widget);
        break;
      case WidgetComponentActionType.EXPORT:
        this.exportWidget($event, widget);
        break;
      case WidgetComponentActionType.REMOVE:
        this.removeWidget($event, widget);
        break;
    }
  }

  private widgetMouseDown($event: Event, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.onWidgetMouseDown) {
      this.callbacks.onWidgetMouseDown($event, widget.widget);
    }
  }

  private widgetClicked($event: Event, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.onWidgetClicked) {
      this.callbacks.onWidgetClicked($event, widget.widget);
    }
  }

  private editWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isEditActionEnabled && this.callbacks && this.callbacks.onEditWidget) {
      this.callbacks.onEditWidget($event, widget.widget);
    }
  }

  private exportWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isExportActionEnabled && this.callbacks && this.callbacks.onExportWidget) {
      this.callbacks.onExportWidget($event, widget.widget);
    }
  }

  exportData($event: Event, ctx: WidgetContext, fileType) {
  	if ($event) {
      $event.stopPropagation();
    }
    const export_data = this.data_format(ctx.datasources, ctx.data);
    this.export(export_data, fileType, ctx.widgetConfig.title);
}

  private removeWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isRemoveActionEnabled && this.callbacks && this.callbacks.onRemoveWidget) {
      this.callbacks.onRemoveWidget($event, widget.widget);
    }
  }

  highlightWidget(widgetId: string, delay?: number) {
    const highlighted = this.dashboardWidgets.highlightWidget(widgetId);
    if (highlighted) {
      this.scrollToWidget(highlighted, delay);
    }
  }

  selectWidget(widgetId: string, delay?: number) {
    const selected = this.dashboardWidgets.selectWidget(widgetId);
    if (selected) {
      this.scrollToWidget(selected, delay);
    }
  }

  getSelectedWidget(): Widget {
    const dashboardWidget = this.dashboardWidgets.getSelectedWidget();
    return dashboardWidget ? dashboardWidget.widget : null;
  }

  getEventGridPosition(event: Event): WidgetPosition {
    const pos: WidgetPosition = {
      row: 0,
      column: 0
    };
    const parentElement = $(this.gridster.el);
    let pageX = 0;
    let pageY = 0;
    if (event instanceof MouseEvent) {
      pageX = event.pageX;
      pageY = event.pageY;
    }
    const offset = parentElement.offset();
    const x = pageX - offset.left + parentElement.scrollLeft();
    const y = pageY - offset.top + parentElement.scrollTop();
    pos.row = this.gridster.pixelsToPositionY(y, Math.floor);
    pos.column = this.gridster.pixelsToPositionX(x, Math.floor);
    return pos;
  }

  resetHighlight() {
    const highlighted = this.dashboardWidgets.resetHighlight();
    if (highlighted) {
      setTimeout(() => {
        this.scrollToWidget(highlighted, 0);
      }, 0);
    }
  }

  private scrollToWidget(widget: DashboardWidget, delay?: number) {
    const parentElement = this.gridster.el as HTMLElement;
    widget.gridsterItemComponent$().subscribe((gridsterItem) => {
      const gridsterItemElement = gridsterItem.el as HTMLElement;
      const offset = (parentElement.clientHeight - gridsterItemElement.clientHeight) / 2;
      let scrollTop;
      if (this.isMobileSize) {
        scrollTop = gridsterItemElement.offsetTop;
      } else {
        scrollTop = scrollTop = gridsterItem.top;
      }
      if (offset > 0) {
        scrollTop -= offset;
      }
      animatedScroll(parentElement, scrollTop, delay);
    });
  }

  private updateMobileOpts(parentHeight?: number) {
    let updateWidgetRowsAndSort = false;
    const isMobileSize = this.checkIsMobileSize();
    if (this.isMobileSize !== isMobileSize) {
      this.isMobileSize = isMobileSize;
      updateWidgetRowsAndSort = true;
    }
    const autofillHeight = this.isAutofillHeight();
    if (autofillHeight) {
      this.gridsterOpts.gridType = this.isMobileSize ? GridType.Fixed : GridType.Fit;
    } else {
      this.gridsterOpts.gridType = this.isMobileSize ? GridType.Fixed : GridType.ScrollVertical;
    }
    const mobileBreakPoint = this.isMobileSize ? 20000 : 0;
    this.gridsterOpts.mobileBreakpoint = mobileBreakPoint;
    const rowSize = this.detectRowSize(this.isMobileSize, autofillHeight, parentHeight);
    if (this.gridsterOpts.fixedRowHeight !== rowSize) {
      this.gridsterOpts.fixedRowHeight = rowSize;
    }
    if (updateWidgetRowsAndSort) {
      this.dashboardWidgets.updateRowsAndSort();
    }
  }

  private onGridsterParentResize() {
    const parentHeight = this.gridster.el.offsetHeight;
    if (this.isMobileSize && this.mobileAutofillHeight && parentHeight) {
      this.updateMobileOpts(parentHeight);
    }
    this.notifyGridsterOptionsChanged();
  }

  private updateLayoutOpts() {
    this.gridsterOpts.minCols = this.columns ? this.columns : 24;
    this.gridsterOpts.outerMargin = isDefined(this.outerMargin) ? this.outerMargin : true;
    this.gridsterOpts.margin = isDefined(this.margin) ? this.margin : 10;
  }

  private updateEditingOpts() {
    this.gridsterOpts.resizable.enabled = this.isEdit;
    this.gridsterOpts.draggable.enabled = this.isEdit;
  }

  public notifyGridsterOptionsChanged() {
    if (!this.optionsChangeNotificationsPaused) {
      if (this.gridster && this.gridster.options) {
        this.gridster.optionsChanged();
      }
    }
  }

  public pauseChangeNotifications() {
    this.optionsChangeNotificationsPaused = true;
  }

  public resumeChangeNotifications() {
    this.optionsChangeNotificationsPaused = false;
  }

  public notifyLayoutUpdated() {
    this.updateWidgetLayouts();
  }

  private detectRowSize(isMobile: boolean, autofillHeight: boolean, parentHeight?: number): number | null {
    let rowHeight = null;
    if (!autofillHeight) {
      if (isMobile) {
        rowHeight = isDefined(this.mobileRowHeight) ? this.mobileRowHeight : 70;
      }
    } else if (autofillHeight && isMobile) {
      if (!parentHeight) {
        parentHeight = this.gridster.el.offsetHeight;
      }
      if (parentHeight) {
        let totalRows = 0;
        for (const widget of this.dashboardWidgets.activeDashboardWidgets) {
          totalRows += widget.rows;
        }
        rowHeight = ( parentHeight - this.gridsterOpts.margin *
          ( totalRows + (this.gridsterOpts.outerMargin ? 1 : -1) ) ) / totalRows;
      }
    }
    return rowHeight;
  }

  private checkIsMobileSize(): boolean {
    const isMobileDisabled = this.isMobileDisabled === true;
    let isMobileSize = this.isMobile === true && !isMobileDisabled;
    if (!isMobileSize && !isMobileDisabled) {
      isMobileSize = !this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    }
    return isMobileSize;
  }

  data_format(datasources: Datasource[], data: DatasourceData[]) {
    let aggregation = [];
    const header = ['timestamp', 'name', 'type'];
    let firstHeader = true;
    datasources.forEach(ds => {
      let entity = [];
      let firstTs = true;
      ds.dataKeys.forEach(dk => {
        if (firstHeader) {
          header.push(dk.name);
        }
        data.forEach(dt => {
          if (dt.dataKey.name === dk.name && dt.datasource.name === ds.entityName) {
            entity.push([dk.name, _.flatMap(dt.data, (arr) => arr[1])]);
            if ((dt.data[0] && dt.data[0][0]) && firstTs) {
              firstTs = false;
              entity.splice(0, 0, ['timestamp', _.flatMap(dt.data, (arr) => arr[0].toString())]);
            }
          }
        });
      });
      firstHeader = false;
      aggregation.push([ds.entityName, ds.entityType, entity]);
    });
    // console.log(aggregation);
    let result = [];
    aggregation.forEach((item, i) => {
      let entityName = item[0];
      let entityType = item[1];
      let v = item[2];

      const dataKeyData = v.filter(item => item[1].length > 0)[0]
      if(dataKeyData){
        for (let i = 0; i < dataKeyData[1].length; i++) {
          let row = [];
          v.forEach((_item, j) => {
            if (j == 0) {
              row[0] = _item[1][i];
              row[1] = entityName;
              row[2] = entityType;
            } else {
              row[j + 2] = _item[1][i] ? _item[1][i] : '';
            }
          });
          result.push(row);
        }
      }
    });
    result.splice(0, 0, header);
    // console.log(result);
    return result;
  }

  export(data: Array<any>, fileType: BookType, title: string): void {
    const ws: XLSX.WorkSheet = XLSX.utils.aoa_to_sheet(data);
    ws['!cols'] = ([
      { wch: 13 }
    ]);
    const output_file_name = title + '-' + Date.parse(new Date().toString()) + '.' + fileType;
    if (fileType === 'csv') {
      const csv = XLSX.utils.sheet_to_csv(ws, { FS: ';', RS: '\n' });
      this.export_csv(csv, output_file_name);
    } else {
      const wb: XLSX.WorkBook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
      XLSX.writeFile(wb, output_file_name, { bookType: fileType, type: 'array'});
    }
  }

  export_csv(data, fileName) {
    const uri = 'data:text/csv;charset=utf-8,\ufeff' + encodeURIComponent(data);
    const downloadLink = document.createElement('a');
    downloadLink.href = uri;
    downloadLink.download = fileName;
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
  }


}
