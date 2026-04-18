import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AdminService } from './core/services/admin.service';
import { ThemeService } from './core/services/theme.service';
import { ConfirmDialogComponent } from './shared/components/confirm-dialog/confirm-dialog.component';

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
      <button mat-icon-button matTooltip="Export database" (click)="exportDb()">
        <mat-icon>download</mat-icon>
      </button>
      <button mat-icon-button matTooltip="Import database" (click)="fileInput.click()">
        <mat-icon>upload</mat-icon>
      </button>
      <input #fileInput type="file" accept=".db" style="display:none" (change)="onFileSelected($event)" />
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
  private admin = inject(AdminService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  exportDb(): void {
    this.admin.exportDb();
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    // Reset input so the same file can be re-selected if needed
    this.fileInput.nativeElement.value = '';

    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Import Database',
        message: 'This will replace all current data with the contents of the selected file. This cannot be undone. Continue?',
      },
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.admin.importDb(file).subscribe({
        next: () => {
          this.snackBar.open('Database imported successfully. Reload the page to see updated data.', 'Reload', { duration: 8000 })
            .onAction().subscribe(() => window.location.reload());
        },
        error: () => {
          this.snackBar.open('Import failed. Make sure the file is a valid database export.', 'Dismiss', { duration: 5000 });
        },
      });
    });
  }
}
