import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MaintenanceType } from '../../../core/models/maintenance-type.model';

@Component({
  selector: 'app-type-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Edit Maintenance Type' : 'Add Maintenance Type' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-col">
        <mat-form-field appearance="outline">
          <mat-label>Name *</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Oil Change" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Interval (miles) *</mat-label>
          <input matInput formControlName="interval_miles" type="number" placeholder="e.g. 5000" />
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
  styles: [`.form-col { display: flex; flex-direction: column; gap: 4px; min-width: 320px; padding-top: 8px; } mat-form-field { width: 100%; }`]
})
export class TypeFormComponent {
  data = inject<MaintenanceType | null>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<TypeFormComponent>);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    name: [this.data?.name ?? '', Validators.required],
    interval_miles: [this.data?.interval_miles ?? null, [Validators.required, Validators.min(1)]],
    notes: [this.data?.notes ?? ''],
  });

  save() {
    if (this.form.valid) this.dialogRef.close(this.form.value);
  }
}
