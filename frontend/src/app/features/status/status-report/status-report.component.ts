import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { DueStatusItem, DueStatusResponse } from '../../../core/models/due-status.model';
import { Vehicle } from '../../../core/models/vehicle.model';
import { StatusService } from '../../../core/services/status.service';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-status-report',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe, DatePipe,
    MatButtonModule, MatFormFieldModule, MatInputModule, MatTableModule,
    StatusBadgeComponent,
  ],
  template: `
    <div class="controls">
      <mat-form-field appearance="outline">
        <mat-label>Current Odometer (miles)</mat-label>
        <input matInput type="number" [(ngModel)]="currentMiles" placeholder="e.g. 42000" />
        @if (vehicle().avg_miles_per_year) {
          <mat-hint>Leave blank to estimate from avg {{ vehicle().avg_miles_per_year | number }} mi/year</mat-hint>
        }
      </mat-form-field>
      <button mat-flat-button color="primary" (click)="check()">Check Status</button>
    </div>

    @if (result()) {
      @if (result()!.estimated_miles_used) {
        <p class="estimate-note">
          Estimated current mileage: {{ result()!.current_miles | number }} mi
          (based on avg miles/year)
        </p>
      }

      @if (result()!.items.length === 0) {
        <p class="empty">No maintenance types configured.</p>
      } @else {
        <table mat-table [dataSource]="result()!.items" class="full-width">
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let item">
              <app-status-badge [status]="item.status" />
            </td>
          </ng-container>
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let item">{{ item.name }}</td>
          </ng-container>
          <ng-container matColumnDef="last_done">
            <th mat-header-cell *matHeaderCellDef>Last Done</th>
            <td mat-cell *matCellDef="let item">
              @if (item.last_performed_date) {
                {{ item.last_performed_date | date:'mediumDate' }} @ {{ item.last_performed_miles | number }} mi
              } @else {
                —
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="next_due">
            <th mat-header-cell *matHeaderCellDef>Next Due</th>
            <td mat-cell *matCellDef="let item">
              @if (item.next_due_miles != null) {
                {{ item.next_due_miles | number }} mi
              } @else {
                —
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="miles_remaining">
            <th mat-header-cell *matHeaderCellDef>Miles Until Due</th>
            <td mat-cell *matCellDef="let item">
              @if (item.miles_until_due <= 0) {
                <span class="overdue">{{ item.miles_until_due | number }} mi overdue</span>
              } @else {
                {{ item.miles_until_due | number }} mi
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols;"></tr>
        </table>
      }
    }
  `,
  styles: [`
    .controls { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
    .controls mat-form-field { min-width: 240px; }
    .estimate-note { color: #757575; font-style: italic; margin-bottom: 12px; }
    .full-width { width: 100%; }
    .empty { color: #9e9e9e; text-align: center; padding: 24px 0; }
    .overdue { color: #d32f2f; font-weight: 500; }
  `]
})
export class StatusReportComponent implements OnInit {
  vehicleId = input.required<number>();
  vehicle = input.required<Vehicle>();
  private statusService = inject(StatusService);

  currentMiles: number | null = null;
  result = signal<DueStatusResponse | null>(null);
  cols = ['status', 'name', 'last_done', 'next_due', 'miles_remaining'];

  ngOnInit() {
    this.check();
  }

  check() {
    this.statusService
      .getStatus(this.vehicleId(), this.currentMiles ?? undefined)
      .subscribe(r => {
        r.items.sort((a, b) => a.miles_until_due - b.miles_until_due);
        this.result.set(r);
      });
  }
}
