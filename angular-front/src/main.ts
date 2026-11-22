import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideMarkdown } from 'ngx-markdown';
import { App } from './app/app';
import { Accounts, NewAccount } from './app/accounts';
import { Bot } from './app/bot';
import { loadingInterceptor } from './app/api';

bootstrapApplication(App, {
  providers: [
    provideHttpClient(withInterceptors([loadingInterceptor])),
    provideMarkdown(),
    provideRouter([
      { path: '', pathMatch: 'full', redirectTo: 'accounts' },
      { path: 'accounts', component: Accounts },
      { path: 'new-account', component: NewAccount },
      { path: 'bot', component: Bot },
    ]),
  ],
}).catch(console.error);
