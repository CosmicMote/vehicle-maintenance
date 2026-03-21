import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MaintenanceType } from '../../../core/models/maintenance-type.model';

export interface RecordFormData {
  maintenanceTypes: MaintenanceType[];
}

@Component({
  selector: 'app-record-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Log Service</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-col">
        <mat-form-field appearance="outline">
          <mat-label>Maintenance Type *</mat-label>
          <mat-select formControlName="maintenance_type_id">
            @for (t of data.maintenanceTypes; track t.id) {
              <mat-option [value]="t.id">{{ t.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date *</mat-label>
          <input matInput formControlName="performed_date" type="date" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Odometer (miles) *</mat-label>
          <input matInput formControlName="performed_miles" type="number" placeholder="e.g. 42000" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="3"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`.form-col { display: flex; flex-direction: column; gap: 4px; min-width: 360px; padding-top: 8px; } mat-form-field { width: 100%; }`]
})
export class RecordFormComponent {
  data = inject<RecordFormData>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<RecordFormComponent>);
  private fb = inject(FormBuilder);

  today = new Date().toISOString().substring(0, 10);

  form = this.fb.group({
    maintenance_type_id: [null as number | null, Validators.required],
    performed_date: [this.today, Validators.required],
    performed_miles: [null as number | null, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  save() {
    if (this.form.valid) this.dialogRef.close(this.form.value);
  }
}
