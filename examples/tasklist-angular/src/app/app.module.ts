import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { registerLocaleData } from '@angular/common';
import { FormsModule } from '@angular/forms';
import localeFr from '@angular/common/locales/fr';
import localeDe from '@angular/common/locales/de';
import localeEn from '@angular/common/locales/en';

import { ApiModule } from 'tasklist/api.module';
import { AppComponent } from 'app/app.component';
import { TasklistComponent } from 'app/components/tasklist/tasklist.component';
import { TaskHelperService } from 'app/services/task.helper.service';
import { ProfileHelperService } from 'app/services/profile.helper.service';
import { FieldNamePipe } from 'app/services/field-name.pipe';
import { FilterService } from 'app/services/filter.service';
import { SortableColumnComponent } from 'app/components/sorter/sortable-column.component';
import { ExternalUrlDirective } from 'app/components/external-url.directive';
import { PageNotFoundComponent } from 'app/components/page-not-found/page-not-found.component';
import { AppRoutingModule } from './app-routing.module';
import { ProcessHelperService } from './services/process.helper.service';
import { ProcesslistComponent } from './components/process-list/process-list.component';


registerLocaleData(localeFr, 'fr');
registerLocaleData(localeDe, 'de');
registerLocaleData(localeEn, 'en');


@NgModule({
  declarations: [
    AppComponent,
    FieldNamePipe,
    SortableColumnComponent,
    TasklistComponent,
    ProcesslistComponent,
    ExternalUrlDirective,
    PageNotFoundComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    AppRoutingModule,
    NgbModule,
    ApiModule
  ],
  providers: [
    TaskHelperService,
    ProfileHelperService,
    ProcessHelperService,
    FilterService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
