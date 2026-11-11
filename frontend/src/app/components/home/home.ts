import {TranslatePipe} from '../../i18n/translate.pipe';
import { Component } from '@angular/core';

@Component({
  imports: [TranslatePipe],
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {}
