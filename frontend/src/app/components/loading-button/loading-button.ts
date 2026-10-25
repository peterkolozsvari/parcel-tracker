import {Component, input, output} from '@angular/core';

@Component({
  selector: 'app-loading-button',
  imports: [],
  templateUrl: './loading-button.html',
  styleUrl: './loading-button.css',
})
export class LoadingButton {
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly label = input('Mentés');
  readonly loadingLabel = input('Mentés…');

  readonly activated = output<void>();
}
