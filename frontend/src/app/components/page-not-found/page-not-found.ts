import {TranslatePipe} from '../../i18n/translate.pipe';
import { Component } from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-page-not-found',
  imports: [TranslatePipe,
    RouterLink
  ],
  templateUrl: './page-not-found.html',
  styleUrl: './page-not-found.css',
})
export class PageNotFound {}
