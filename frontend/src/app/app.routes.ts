import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'vehicles', pathMatch: 'full' },
  {
    path: 'vehicles',
    loadComponent: () =>
      import('./features/vehicles/vehicle-list/vehicle-list.component').then(m => m.VehicleListComponent),
  },
  {
    path: 'vehicles/:id',
    loadComponent: () =>
      import('./features/vehicles/vehicle-detail/vehicle-detail.component').then(m => m.VehicleDetailComponent),
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/admin/email-settings/email-settings.component').then(m => m.EmailSettingsComponent),
  },
];
