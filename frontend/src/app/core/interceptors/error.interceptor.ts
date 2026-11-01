import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  return next(req).pipe(
    catchError(err => {
      const detail = err.error?.detail;
      // FastAPI validation errors (422) return a list of {msg, ...} objects
      const message = Array.isArray(detail)
        ? detail.map((d: { msg?: string }) => (d.msg ?? '').replace(/^Value error, /, '')).join('; ')
        : detail ?? err.message ?? 'An unexpected error occurred';
      snackBar.open(message, 'Dismiss', { duration: 5000, panelClass: 'error-snackbar' });
      return throwError(() => err);
    })
  );
};
