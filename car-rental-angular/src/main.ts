/*
 *  Protractor support is deprecated in Angular.
 *  Protractor is used in this example for compatibility with Angular documentation tools.
 */
import {bootstrapApplication, provideProtractorTestingSupport} from '@angular/platform-browser';
import {AppComponent} from './app/app.component';
import {provideRouter} from '@angular/router';
import routeConfig from './app/routes';
import {provideHttpClient} from '@angular/common/http';

bootstrapApplication(AppComponent, {
  providers: [provideProtractorTestingSupport(), provideRouter(routeConfig), provideHttpClient()],
}).catch((err) => console.error(err));
