import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Vehicle } from '../../../core/models/vehicle.model';
import { VehicleService } from '../../../core/services/vehicle.service';
import { VehicleFormComponent } from '../vehicle-form/vehicle-form.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [
    DecimalPipe, RouterLink,
    MatButtonModule, MatCardModule, MatIconModule,
    MatProgressBarModule, MatTooltipModule,
  ],
  template: `
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    <div class="page-content">
      <div class="header-row">
        <h1>My Vehicles</h1>
        <button mat-flat-button color="primary" (click)="openForm()">
          <mat-icon>add</mat-icon> Add Vehicle
        </button>
      </div>

      @if (!loading() && vehicles().length === 0) {
        <div class="empty-state">
          <mat-icon>directions_car</mat-icon>
          <p>No vehicles yet. Add one to get started.</p>
        </div>
      }

      <div class="card-grid">
        @for (v of vehicles(); track v.id) {
          <mat-card>
            <mat-card-header>
              <mat-card-title>{{ v.name }}</mat-card-title>
              <mat-card-subtitle>
                {{ v.year }} {{ v.make }} {{ v.model }}
              </mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              @if (v.avg_miles_per_year) {
                <p class="muted">~{{ v.avg_miles_per_year | number }} mi/year</p>
              }
            </mat-card-content>
            <mat-card-actions align="end">
              <button mat-icon-button matTooltip="Edit" (click)="openForm(v)">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button matTooltip="Delete" color="warn" (click)="delete(v)">
                <mat-icon>delete</mat-icon>
              </button>
              <a mat-flat-button color="primary" [routerLink]="['/vehicles', v.id]">View</a>
            </mat-card-actions>
          </mat-card>
        }
      </div>
    </div>
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .empty-state { text-align: center; padding: 60px 0; color: #9e9e9e; }
    .empty-state mat-icon { font-size: 64px; height: 64px; width: 64px; }
    .muted { color: #757575; margin: 0; }
  `]
})
export class VehicleListComponent implements OnInit {
  private vehicleService = inject(VehicleService);
  private dialog = inject(MatDialog);

  vehicles = signal<Vehicle[]>([]);
  loading = signal(true);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.vehicleService.list().subscribe({
      next: v => { this.vehicles.set(v); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openForm(vehicle?: Vehicle) {
    const ref = this.dialog.open(VehicleFormComponent, {
      data: vehicle ?? null,
      width: '440px',
    });
    ref.afterClosed().subscribe((result: unknown) => {
      if (!result) return;
      const call = vehicle
        ? this.vehicleService.update(vehicle.id, result as Vehicle)
        : this.vehicleService.create(result as Vehicle);
      call.subscribe(() => this.load());
    });
  }

  delete(vehicle: Vehicle) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Vehicle', message: `Delete "${vehicle.name}" and all its data?` },
    });
    ref.afterClosed().subscribe((confirmed: unknown) => {
      if (confirmed) this.vehicleService.delete(vehicle.id).subscribe(() => this.load());
    });
  }
}
