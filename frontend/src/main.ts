import { bootstrapApplication } from '@angular/platform-browser';
import { defineCustomElements } from 'ionicons/loader';
import { addIcons } from 'ionicons';
import { copyOutline } from 'ionicons/icons';
import { appConfig } from './app/app.config';
import { App } from './app/app';

addIcons({
  'copy-outline': copyOutline
});

defineCustomElements(window);

bootstrapApplication(App, appConfig)
  .catch(err => console.error(err));
