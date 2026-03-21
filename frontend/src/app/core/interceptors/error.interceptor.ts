import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  return next(req).pipe(
    catchError(err => {
      const message = err.error?.detail ?? err.message ?? 'An unexpected error occurred';
      snackBar.open(message, 'Dismiss', { duration: 5000, panelClass: 'error-snackbar' });
      return throwError(() => err);
    })
  );
};
