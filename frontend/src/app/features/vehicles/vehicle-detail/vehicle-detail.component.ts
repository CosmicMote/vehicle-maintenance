import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Vehicle } from '../../../core/models/vehicle.model';
import { VehicleService } from '../../../core/services/vehicle.service';
import { MileageListComponent } from '../../mileage/mileage-list/mileage-list.component';
import { TypeListComponent } from '../../maintenance-types/type-list/type-list.component';
import { RecordListComponent } from '../../records/record-list/record-list.component';
import { StatusReportComponent } from '../../status/status-report/status-report.component';

@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule, MatIconModule, MatProgressBarModule, MatTabsModule,
    MileageListComponent, TypeListComponent, RecordListComponent, StatusReportComponent,
  ],
  template: `
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (vehicle()) {
      <div class="page-content">
        <div class="header-row">
          <div>
            <a mat-icon-button routerLink="/vehicles" matTooltip="Back"><mat-icon>arrow_back</mat-icon></a>
            <h1 style="display:inline; margin-left: 8px;">{{ vehicle()!.name }}</h1>
            <span class="subtitle">
              {{ vehicle()!.year }} {{ vehicle()!.make }} {{ vehicle()!.model }}
            </span>
          </div>
        </div>

        <mat-tab-group>
          <mat-tab label="Maintenance Types">
            <div class="tab-content">
              <app-type-list [vehicleId]="vehicle()!.id" />
            </div>
          </mat-tab>
          <mat-tab label="Mileage Records">
            <div class="tab-content">
              <app-mileage-list [vehicleId]="vehicle()!.id" />
            </div>
          </mat-tab>
          <mat-tab label="Service Records">
            <div class="tab-content">
              <app-record-list [vehicleId]="vehicle()!.id" />
            </div>
          </mat-tab>
          <mat-tab label="Status Report">
            <div class="tab-content">
              <app-status-report [vehicleId]="vehicle()!.id" [vehicle]="vehicle()!" />
            </div>
          </mat-tab>
        </mat-tab-group>
      </div>
    }
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .subtitle { color: #757575; margin-left: 8px; font-size: 14px; }
    .tab-content { padding: 20px 0; }
  `]
})
export class VehicleDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private vehicleService = inject(VehicleService);

  vehicle = signal<Vehicle | null>(null);
  loading = signal(true);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.vehicleService.get(id).subscribe({
      next: v => { this.vehicle.set(v); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
