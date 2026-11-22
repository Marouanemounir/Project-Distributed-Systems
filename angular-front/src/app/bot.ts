import { HttpClient, HttpEventType } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MarkdownComponent } from 'ngx-markdown';
import { finalize, Subscription } from 'rxjs';
import { apiError, gateway } from './api';

@Component({
  imports: [FormsModule, MarkdownComponent],
  template: `
    <h1 class="mb-4">Assistant EBank</h1>
    <form class="panel mb-3" (ngSubmit)="ask()">
      <label class="form-label" for="query">Votre question</label>
      <textarea id="query" class="form-control" rows="3" maxlength="4000" name="query"
        placeholder="Par exemple, liste les comptes d’épargne" [(ngModel)]="question"></textarea>
      <div class="d-flex flex-wrap gap-3 align-items-center mt-3">
        <button type="submit" class="btn btn-primary" [disabled]="working() || !question.trim()">Envoyer</button>
        @if (working()) {
          <button class="btn btn-outline-secondary" type="button" (click)="cancel()">Arrêter</button>
        }
        <div class="form-check">
          <input class="form-check-input" id="stream" type="checkbox" name="stream" [(ngModel)]="live" [disabled]="working()">
          <label class="form-check-label" for="stream">Réponse en direct</label>
        </div>
      </div>
    </form>
    @if (error()) { <div role="alert" class="alert alert-danger">{{ error() }}</div> }
    @if (working() && !answer()) { <p role="status">L’assistant prépare sa réponse…</p> }
    @if (answer()) {
      <section class="panel" aria-label="Réponse de l’assistant">
        <markdown [data]="answer()"></markdown>
      </section>
    }
  `
})
export class Bot {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly conversationId = crypto.randomUUID();
  private request?: Subscription;
  private receivedLength = 0;
  private pending = '';
  private sawDone = false;
  question = '';
  live = true;
  readonly answer = signal('');
  readonly error = signal('');
  readonly working = signal(false);

  ask() {
    const query = this.question.trim();
    if (!query || this.working()) return;
    this.answer.set('');
    this.error.set('');
    this.working.set(true);
    this.receivedLength = 0;
    this.pending = '';
    this.sawDone = false;
    const params = { query, conversationId: this.conversationId };
    const endpoint = gateway + '/EBANK-BOT';
    const complete = () => this.working.set(false);
    if (!this.live) {
      this.request = this.http.get(endpoint + '/chat', { params, responseType: 'text' }).pipe(
        takeUntilDestroyed(this.destroyRef), finalize(complete)
      ).subscribe({ next: answer => this.answer.set(answer), error: error => this.error.set(apiError(error)) });
      return;
    }
    this.request = this.http.get(endpoint + '/chat-stream', {
      params, responseType: 'text', observe: 'events', reportProgress: true,
    }).pipe(takeUntilDestroyed(this.destroyRef), finalize(complete)).subscribe({
      next: event => {
        if (event.type === HttpEventType.DownloadProgress && event.partialText !== undefined) {
          this.process(event.partialText);
        } else if (event.type === HttpEventType.Response) {
          this.process(event.body ?? '');
          if (!this.sawDone && !this.error()) this.error.set('La réponse a été interrompue.');
        }
      },
      error: error => this.error.set(apiError(error)),
    });
  }

  cancel() { this.request?.unsubscribe(); }

  private process(fullText: string) {
    this.pending += fullText.slice(this.receivedLength);
    this.receivedLength = fullText.length;
    const frames = this.pending.split(/\r?\n\r?\n/);
    this.pending = frames.pop() ?? '';
    for (const frame of frames) {
      const lines = frame.split(/\r?\n/);
      const type = lines.find(line => line.startsWith('event:'))?.slice(6).trim();
      const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trimStart()).join('\n');
      if (!data) continue;
      try {
        const text: string = JSON.parse(data).text;
        if (type === 'chunk') this.answer.update(answer => answer + text);
        if (type === 'done') this.sawDone = true;
        if (type === 'failure') this.error.set(text);
      } catch {
        this.error.set('Un événement de la réponse est illisible.');
      }
    }
  }
}
