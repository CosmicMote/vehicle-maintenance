import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MaintenanceRecord } from '../../../core/models/maintenance-record.model';
import { MaintenanceType } from '../../../core/models/maintenance-type.model';
import { MaintenanceTypeService } from '../../../core/services/maintenance-type.service';
import { RecordService } from '../../../core/services/record.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { RecordFormComponent } from '../record-form/record-form.component';

@Component({
  selector: 'app-record-list',
  standalone: true,
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule],
  template: `
    <div class="tab-actions">
      <button mat-flat-button color="primary" (click)="openForm()">
        <mat-icon>add</mat-icon> Log Service
      </button>
    </div>

    @if (records().length === 0) {
      <p class="empty">No service records yet.</p>
    } @else {
      <table mat-table [dataSource]="records()" class="full-width">
        <ng-container matColumnDef="date">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let r">{{ r.performed_date | date:'mediumDate' }}</td>
        </ng-container>
        <ng-container matColumnDef="type">
          <th mat-header-cell *matHeaderCellDef>Type</th>
          <td mat-cell *matCellDef="let r">{{ r.maintenance_type_name }}</td>
        </ng-container>
        <ng-container matColumnDef="miles">
          <th mat-header-cell *matHeaderCellDef>Odometer</th>
          <td mat-cell *matCellDef="let r">{{ r.performed_miles | number }} mi</td>
        </ng-container>
        <ng-container matColumnDef="notes">
          <th mat-header-cell *matHeaderCellDef>Notes</th>
          <td mat-cell *matCellDef="let r">{{ r.notes }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let r">
            <button mat-icon-button color="warn" matTooltip="Delete" (click)="delete(r)">
              <mat-icon>delete</mat-icon>
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr>
        <tr mat-row *matRowDef="let row; columns: cols;"></tr>
      </table>
    }
  `,
  styles: [`
    .tab-actions { margin-bottom: 16px; }
    .full-width { width: 100%; }
    .empty { color: #9e9e9e; padding: 24px 0; text-align: center; }
  `]
})
export class RecordListComponent implements OnInit {
  vehicleId = input.required<number>();
  private recordService = inject(RecordService);
  private typeService = inject(MaintenanceTypeService);
  private dialog = inject(MatDialog);

  records = signal<MaintenanceRecord[]>([]);
  types = signal<MaintenanceType[]>([]);
  cols = ['date', 'type', 'miles', 'notes', 'actions'];

  ngOnInit() { this.load(); }

  load() {
    this.typeService.list(this.vehicleId()).subscribe(t => this.types.set(t));
    this.recordService.list(this.vehicleId()).subscribe(r => this.records.set(r));
  }

  openForm() {
    this.typeService.list(this.vehicleId()).subscribe(types => {
      const ref = this.dialog.open(RecordFormComponent, {
        data: { maintenanceTypes: types },
        width: '420px',
      });
      ref.afterClosed().subscribe(result => {
        if (result) this.recordService.create(this.vehicleId(), result).subscribe(() => this.load());
      });
    });
  }

  delete(record: MaintenanceRecord) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Record', message: 'Delete this service record?' },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) this.recordService.delete(this.vehicleId(), record.id).subscribe(() => this.load());
    });
  }
}
