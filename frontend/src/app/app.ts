import {Router, RouterOutlet} from '@angular/router';
import {Navbar} from './components/navbar/navbar';
import { Component, inject, signal } from '@angular/core';




@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly router = inject(Router);
  protected readonly title = signal('frontend');
}
