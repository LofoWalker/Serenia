import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CursorGlowComponent } from './shared/ui/cursor-glow/cursor-glow.component';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, CursorGlowComponent],
  template: `
    <app-cursor-glow />
    <router-outlet />
  `
})
export class App {}
