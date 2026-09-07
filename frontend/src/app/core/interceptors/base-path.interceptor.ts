import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Prepends the app's deployed sub-path (from <base href>, set server-side via
 * the BASE_PATH env var) to API requests, so /api/... calls still work when the
 * app is served behind a reverse proxy at e.g. https://host/vehicle-maintenance/.
 */
export const basePathInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  const basePath = new URL(document.baseURI).pathname.replace(/\/$/, '');
  return next(req.clone({ url: `${basePath}${req.url}` }));
};
