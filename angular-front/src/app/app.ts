import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LoadingService } from './api';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="site-header">
      <div class="container py-3 d-flex flex-wrap gap-3 align-items-center justify-content-between">
        <a routerLink="/accounts" class="brand"><i class="bi bi-bank2" aria-hidden="true"></i> EBank</a>
        <nav aria-label="Navigation principale" class="d-flex flex-wrap gap-1">
          <a routerLink="/accounts" routerLinkActive="active">Comptes</a>
          <a routerLink="/new-account" routerLinkActive="active">Nouveau compte</a>
          <a routerLink="/bot" routerLinkActive="active">Assistant</a>
        </nav>
      </div>
      @if (loading.busy()) {
        <div class="activity" role="status" aria-label="Chargement en cours">
          <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        </div>
      }
    </header>
    <main class="container py-5"><router-outlet /></main>
  `
})
export class App {
  readonly loading = inject(LoadingService);
}
