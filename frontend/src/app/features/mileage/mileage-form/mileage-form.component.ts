import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MileageRecord } from '../../../core/models/mileage-record.model';

@Component({
  selector: 'app-mileage-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Edit Mileage Record' : 'Add Mileage Record' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-col">
        <mat-form-field appearance="outline">
          <mat-label>Odometer (miles) *</mat-label>
          <input matInput formControlName="miles" type="number" placeholder="e.g. 42000" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date *</mat-label>
          <input matInput formControlName="recorded_date" type="date" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`.form-col { display: flex; flex-direction: column; gap: 4px; min-width: 300px; padding-top: 8px; } mat-form-field { width: 100%; }`]
})
export class MileageFormComponent {
  data = inject<MileageRecord | null>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<MileageFormComponent>);
  private fb = inject(FormBuilder);

  today = new Date().toISOString().substring(0, 10);

  form = this.fb.group({
    miles: [this.data?.miles ?? null as number | null, [Validators.required, Validators.min(0)]],
    recorded_date: [this.data?.recorded_date ?? this.today, Validators.required],
  });

  save() {
    if (this.form.valid) this.dialogRef.close(this.form.value);
  }
}
