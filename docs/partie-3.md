# Partie 3 — Angular 21 et streaming

Démarrer [le backend](partie-1.md) et [l’agent](partie-2.md). Avec Node.js 22.12+ ou 24, exécuter depuis `angular-front` :

```sh
npm install
npm run start
```

Ouvrir <http://localhost:4200>. La table utilise les API de la gateway. Cliquer sur un UUID pour consulter son titulaire. L’écran « Nouveau compte » utilise les Signal Forms et recharge la liste après une création acceptée.

L’onglet « Assistant » utilise `/EBANK-BOT/chat` en mode standard et `/EBANK-BOT/chat-stream` en mode direct. La réponse s’affiche dans un composant Markdown. En mode direct, vérifier que le texte s’enrichit au fil des événements SSE. Le bouton « Arrêter » annule la requête. Le mode standard reste accessible en décochant « Réponse en direct ».

Pour inspecter le flux depuis un client REST, garder l’agent actif puis exécuter :

```sh
curl -N -G --data-urlencode "query=Liste les clients" http://localhost:9999/EBANK-BOT/chat-stream
```

Compiler la version de production avec `npm run build`.
