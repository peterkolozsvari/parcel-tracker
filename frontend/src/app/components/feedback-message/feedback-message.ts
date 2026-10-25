import {Component, input} from '@angular/core';

@Component({
  selector: 'app-feedback-message',
  imports: [],
  templateUrl: './feedback-message.html',
  styleUrl: './feedback-message.css',
})
export class FeedbackMessage {
  readonly message = input('');
  readonly type = input<'success' | 'error'>('error');
}
