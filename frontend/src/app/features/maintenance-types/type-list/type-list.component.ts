import { Component, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MaintenanceType } from '../../../core/models/maintenance-type.model';
import { MaintenanceTypeService } from '../../../core/services/maintenance-type.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { TypeFormComponent } from '../type-form/type-form.component';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-type-list',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule, DecimalPipe],
  template: `
    <div class="tab-actions">
      <button mat-flat-button color="primary" (click)="openForm()">
        <mat-icon>add</mat-icon> Add Type
      </button>
    </div>

    @if (types().length === 0) {
      <p class="empty">No maintenance types defined yet.</p>
    } @else {
      <table mat-table [dataSource]="types()" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Type</th>
          <td mat-cell *matCellDef="let t">{{ t.name }}</td>
        </ng-container>
        <ng-container matColumnDef="interval_miles">
          <th mat-header-cell *matHeaderCellDef>Interval</th>
          <td mat-cell *matCellDef="let t">Every {{ t.interval_miles | number }} mi</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let t">
            <button mat-icon-button matTooltip="Edit" (click)="openForm(t)"><mat-icon>edit</mat-icon></button>
            <button mat-icon-button matTooltip="Delete" color="warn" (click)="delete(t)"><mat-icon>delete</mat-icon></button>
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
export class TypeListComponent implements OnInit {
  vehicleId = input.required<number>();
  private typeService = inject(MaintenanceTypeService);
  private dialog = inject(MatDialog);

  types = signal<MaintenanceType[]>([]);
  cols = ['name', 'interval_miles', 'actions'];

  ngOnInit() { this.load(); }

  load() {
    this.typeService.list(this.vehicleId()).subscribe(t => this.types.set(t));
  }

  openForm(type?: MaintenanceType) {
    const ref = this.dialog.open(TypeFormComponent, { data: type ?? null, width: '380px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const call = type
        ? this.typeService.update(this.vehicleId(), type.id, result)
        : this.typeService.create(this.vehicleId(), result);
      call.subscribe(() => this.load());
    });
  }

  delete(type: MaintenanceType) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Type', message: `Delete "${type.name}" and all its records?` },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) this.typeService.delete(this.vehicleId(), type.id).subscribe(() => this.load());
    });
  }
}
