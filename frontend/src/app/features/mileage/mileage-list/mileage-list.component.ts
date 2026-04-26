import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MileageRecord } from '../../../core/models/mileage-record.model';
import { MileageService } from '../../../core/services/mileage.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { MileageFormComponent } from '../mileage-form/mileage-form.component';

@Component({
  selector: 'app-mileage-list',
  standalone: true,
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule],
  template: `
    <div class="tab-actions">
      <button mat-flat-button color="primary" (click)="openForm()">
        <mat-icon>add</mat-icon> Add Mileage Record
      </button>
    </div>

    @if (records().length === 0) {
      <p class="empty">No mileage records yet.</p>
    } @else {
      <table mat-table [dataSource]="records()" class="full-width">
        <ng-container matColumnDef="date">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let r">{{ r.recorded_date | date:'mediumDate' }}</td>
        </ng-container>
        <ng-container matColumnDef="miles">
          <th mat-header-cell *matHeaderCellDef>Odometer</th>
          <td mat-cell *matCellDef="let r">{{ r.miles | number }} mi</td>
        </ng-container>
        <ng-container matColumnDef="notes">
          <th mat-header-cell *matHeaderCellDef>Notes</th>
          <td mat-cell *matCellDef="let r">{{ r.notes }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let r">
            <button mat-icon-button matTooltip="Edit" (click)="openForm(r)">
              <mat-icon>edit</mat-icon>
            </button>
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
export class MileageListComponent implements OnInit {
  vehicleId = input.required<number>();
  private mileageService = inject(MileageService);
  private dialog = inject(MatDialog);

  records = signal<MileageRecord[]>([]);
  cols = ['date', 'miles', 'notes', 'actions'];

  ngOnInit() { this.load(); }

  load() {
    this.mileageService.list(this.vehicleId()).subscribe(r => {
      this.records.set(r.slice().sort((a, b) =>
        a.miles !== b.miles
          ? b.miles - a.miles
          : b.recorded_date.localeCompare(a.recorded_date)
      ));
    });
  }

  openForm(record?: MileageRecord) {
    const ref = this.dialog.open(MileageFormComponent, { data: record ?? null, width: '360px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const call = record
        ? this.mileageService.update(this.vehicleId(), record.id, result)
        : this.mileageService.create(this.vehicleId(), result);
      call.subscribe(() => this.load());
    });
  }

  delete(record: MileageRecord) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Mileage Record', message: 'Delete this mileage record?' },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) this.mileageService.delete(this.vehicleId(), record.id).subscribe(() => this.load());
    });
  }
}
