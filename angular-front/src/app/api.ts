import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';

export const gateway = 'http://localhost:9999';
export type AccountType = 'CURRENT_ACCOUNT' | 'SAVING_ACCOUNT';
export interface AccountRequest { balance: number; type: AccountType; customerId: number }
export interface Customer { id: number; name: string; email: string }
export interface Account extends AccountRequest { id: string; createdAt: string; customer?: Customer }

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = signal(0);
  readonly busy = computed(() => this.pending() > 0);
  start() { this.pending.update(count => count + 1); }
  stop() { this.pending.update(count => count - 1); }
}

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loading = inject(LoadingService);
  loading.start();
  return next(request).pipe(finalize(() => loading.stop()));
};

@Injectable({ providedIn: 'root' })
export class BankingApi {
  private readonly http = inject(HttpClient);
  private readonly accounts = gateway + '/EBANK-SERVICE/accounts';

  listAccounts() { return this.http.get<Account[]>(this.accounts); }
  account(id: string) { return this.http.get<Account>(this.accounts + '/' + encodeURIComponent(id)); }
  create(request: AccountRequest) { return this.http.post<Account>(this.accounts, request); }
  customers() { return this.http.get<Customer[]>(gateway + '/CUSTOMER-SERVICE/customers'); }
}

export function apiError(error: unknown): string {
  const status = error instanceof HttpErrorResponse ? error.status : undefined;
  switch (status) {
    case 0: return 'La gateway est inaccessible. Vérifiez que les services sont démarrés.';
    case 400: return 'Vérifiez les données saisies.';
    case 404: return 'La ressource demandée est introuvable.';
    case 422: return 'Le client sélectionné est introuvable.';
    case 503: return 'Le service est momentanément indisponible.';
    default: return 'La requête a échoué. Réessayez.';
  }
}
