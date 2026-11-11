import {inject, Pipe, PipeTransform} from '@angular/core';
import {LanguageService} from '../services/language.service';

@Pipe({name: 'translate', pure: false})
export class TranslatePipe implements PipeTransform {
  private readonly language = inject(LanguageService);
  transform(value: string | null | undefined): string { return this.language.translate(value); }
}
