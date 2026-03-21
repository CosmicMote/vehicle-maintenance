import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatButtonModule, MatIconModule, MatToolbarModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>build</mat-icon>
      <span style="margin-left:8px">Vehicle Maintenance</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/vehicles">My Vehicles</a>
    </mat-toolbar>
    <router-outlet />
  `,
  styles: [`
    mat-toolbar { position: sticky; top: 0; z-index: 100; }
    .spacer { flex: 1 1 auto; }
  `]
})
export class App {}
