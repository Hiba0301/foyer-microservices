import { HttpErrorResponse } from '@angular/common/http';

/** Transforme une erreur HTTP en message lisible pour l'utilisateur. */
export function messageErreur(err: unknown, contexte: string): string {
  if (!(err instanceof HttpErrorResponse)) {
    return `${contexte} : erreur inattendue.`;
  }
  if (err.status === 0) {
    return `${contexte} : serveur injoignable. Vérifiez que l'API Gateway tourne sur le port 8080.`;
  }
  if (err.status === 404) {
    return `${contexte} : ressource introuvable (404).`;
  }
  const detail = typeof err.error?.message === 'string' ? err.error.message : err.statusText;
  return `${contexte} : erreur ${err.status}${detail ? ' (' + detail + ')' : ''}.`;
}
