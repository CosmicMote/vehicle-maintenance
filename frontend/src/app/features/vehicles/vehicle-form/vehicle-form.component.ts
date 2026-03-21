import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Vehicle } from '../../../core/models/vehicle.model';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Edit Vehicle' : 'Add Vehicle' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline">
          <mat-label>Name *</mat-label>
          <input matInput formControlName="name" placeholder="e.g. My Truck" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Make</mat-label>
          <input matInput formControlName="make" placeholder="e.g. Ford" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Model</mat-label>
          <input matInput formControlName="model" placeholder="e.g. F-150" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Year</mat-label>
          <input matInput formControlName="year" type="number" placeholder="e.g. 2020" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Avg Miles / Year</mat-label>
          <input matInput formControlName="avg_miles_per_year" type="number" placeholder="e.g. 12000" />
          <mat-hint>Used to estimate mileage when checking status</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid { display: flex; flex-direction: column; gap: 4px; min-width: 360px; padding-top: 8px; }
    mat-form-field { width: 100%; }
  `]
})
export class VehicleFormComponent {
  data = inject<Vehicle | null>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<VehicleFormComponent>);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    name: [this.data?.name ?? '', Validators.required],
    make: [this.data?.make ?? ''],
    model: [this.data?.model ?? ''],
    year: [this.data?.year ?? null],
    avg_miles_per_year: [this.data?.avg_miles_per_year ?? null],
  });

  save() {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }
}
