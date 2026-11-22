import { AsyncPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormField, form, max, min, required } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { catchError, finalize, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Account, AccountType, apiError, BankingApi } from './api';

const listLoading = { status: 'loading' } as const;

@Component({
  imports: [AsyncPipe, DatePipe, DecimalPipe],
  template: `
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
      <h1 class="mb-0">Comptes bancaires</h1>
      <button class="btn btn-outline-secondary" (click)="reload.next()">Actualiser</button>
    </div>
    @if (state$ | async; as state) {
      @switch (state.status) {
        @case ('loading') { <p role="status">Chargement des comptes…</p> }
        @case ('error') { <div role="alert" class="alert alert-danger">{{ state.message }}</div> }
        @case ('success') {
          <section class="panel">
            <div class="table-responsive">
              <table class="table table-hover mb-0">
                <caption class="visually-hidden">Liste des comptes</caption>
                <thead><tr><th>Identifiant</th><th>Type</th><th>Solde</th><th>Titulaire</th><th>Création</th></tr></thead>
                <tbody>
                  @for (account of state.accounts; track account.id) {
                    <tr>
                      <td class="account-id"><button class="btn btn-link btn-sm text-start p-0" (click)="show(account.id)">{{ account.id }}</button></td>
                      <td>{{ account.type === 'CURRENT_ACCOUNT' ? 'Courant' : 'Épargne' }}</td>
                      <td class="amount">{{ account.balance | number:'1.2-2' }}</td>
                      <td>{{ account.customerId }}</td>
                      <td>{{ account.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5">Aucun compte disponible.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      }
    }
    @if (detailError()) {
      <p role="alert" class="alert alert-warning mt-3">{{ detailError() }}</p>
    }
    @if (selection(); as selected) {
      <section class="panel mt-3">
        <h2 class="h5">Titulaire du compte</h2>
        @if (selected.customer; as customer) {
          <p class="mb-0">{{ customer.name }} · {{ customer.email }}</p>
        }
        <button class="btn btn-outline-secondary btn-sm mt-3" (click)="selection.set(null)">Fermer</button>
      </section>
    }
  `
})
export class Accounts {
  private readonly api = inject(BankingApi);
  private readonly details = new Subject<string>();
  readonly reload = new Subject<void>();
  readonly selection = signal<Account | null>(null);
  readonly detailError = signal('');
  readonly state$ = this.reload.pipe(
    startWith(undefined),
    switchMap(() => this.api.listAccounts().pipe(
      map(accounts => ({ status: 'success', accounts }) as const),
      catchError(error => of({ status: 'error', message: apiError(error) } as const)),
      startWith(listLoading)
    ))
  );

  constructor() {
    this.details.pipe(
      switchMap(id => this.api.account(id).pipe(catchError(error => {
        this.detailError.set(apiError(error));
        return of(null);
      }))),
      takeUntilDestroyed()
    ).subscribe(account => this.selection.set(account));
  }

  show(id: string) {
    this.selection.set(null);
    this.detailError.set('');
    this.details.next(id);
  }
}

type Draft = { balance: number | null; type: AccountType | ''; customerId: string };

@Component({
  imports: [AsyncPipe, FormField],
  template: `
    <h1 class="mb-4">Nouveau compte</h1>
    <form class="panel mx-auto" style="max-width:40rem" (submit)="save($event)" novalidate>
      @if (error()) { <div class="alert alert-danger" role="alert">{{ error() }}</div> }
      @if (customers$ | async; as customers) {
        <label class="form-label" for="customer">Client</label>
        <select class="form-select mb-3" id="customer" [formField]="fields.customerId">
          <option value="">Sélectionner un client</option>
          @for (customer of customers; track customer.id) {
            <option [value]="customer.id.toString()">{{ customer.name }} ({{ customer.id }})</option>
          }
        </select>
      }
      <label class="form-label" for="balance">Solde initial</label>
      <input class="form-control mb-3" id="balance" type="number" step="0.01" [formField]="fields.balance">
      <label class="form-label" for="type">Type de compte</label>
      <select class="form-select mb-4" id="type" [formField]="fields.type">
        <option value="">Sélectionner un type</option>
        <option value="CURRENT_ACCOUNT">Courant</option>
        <option value="SAVING_ACCOUNT">Épargne</option>
      </select>
      <button class="btn btn-primary" type="submit" [disabled]="fields().invalid() || saving()">Créer le compte</button>
    </form>
  `
})
export class NewAccount {
  private readonly api = inject(BankingApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly error = signal('');
  readonly saving = signal(false);
  readonly customers$ = this.api.customers().pipe(catchError(error => {
    this.error.set(apiError(error));
    return of([]);
  }));
  private readonly draft = signal<Draft>({ balance: null, type: '', customerId: '' });
  readonly fields = form(this.draft, path => {
    required(path.balance);
    min(path.balance, 0);
    max(path.balance, 9_999_999_999_999);
    required(path.type);
    required(path.customerId);
  });

  save(event: Event) {
    event.preventDefault();
    if (this.fields().invalid() || this.saving()) return;
    const { balance, type, customerId } = this.draft();
    if (balance === null || type === '' || !/^\d+(\.\d{1,2})?$/.test(String(balance))) {
      this.error.set('Le solde doit comporter au maximum deux décimales.');
      return;
    }
    this.error.set('');
    this.saving.set(true);
    this.api.create({ balance, type, customerId: Number(customerId) }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.saving.set(false))
    ).subscribe({
      next: () => void this.router.navigate(['/accounts']),
      error: error => this.error.set(apiError(error)),
    });
  }
}
