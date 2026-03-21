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
];
