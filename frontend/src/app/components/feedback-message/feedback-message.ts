import {TranslatePipe} from '../../i18n/translate.pipe';
import {Component, input} from '@angular/core';

@Component({
  selector: 'app-feedback-message',
  imports: [TranslatePipe],
  templateUrl: './feedback-message.html',
  styleUrl: './feedback-message.css',
})
export class FeedbackMessage {
  readonly message = input('');
  readonly type = input<'success' | 'error'>('error');
}
