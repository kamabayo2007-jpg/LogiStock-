import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { LogiStockApp } from './app/logistock-app';

bootstrapApplication(LogiStockApp, appConfig).catch((err) => console.error(err));
