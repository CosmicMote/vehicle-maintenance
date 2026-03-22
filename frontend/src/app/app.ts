import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule, MatToolbarModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>build</mat-icon>
      <span style="margin-left:8px">Vehicle Maintenance</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/vehicles">My Vehicles</a>
      <button mat-icon-button
        [matTooltip]="theme.isDark() ? 'Switch to light mode' : 'Switch to dark mode'"
        (click)="theme.toggle()">
        <mat-icon>{{ theme.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>
    </mat-toolbar>
    <router-outlet />
  `,
  styles: [`
    mat-toolbar { position: sticky; top: 0; z-index: 100; }
    .spacer { flex: 1 1 auto; }
  `]
})
export class App {
  theme = inject(ThemeService);
}
