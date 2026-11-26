import { HttpErrorResponse } from '@angular/common/http';
import { messageErreur } from './http-error';

describe('messageErreur', () => {
  it('signale un serveur injoignable (status 0)', () => {
    expect(messageErreur(new HttpErrorResponse({ status: 0 }), 'Chargement')).toContain('serveur injoignable');
  });

  it('reprend le message renvoyé par Spring', () => {
    const err = new HttpErrorResponse({ status: 500, error: { message: 'No value present' } });
    expect(messageErreur(err, 'Chargement')).toBe('Chargement : erreur 500 (No value present).');
  });
});
