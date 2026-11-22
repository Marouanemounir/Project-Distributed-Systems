# Partie 2 — Agent Spring AI et MCP

Démarrer les services de [la partie 1](partie-1.md). Les deux serveurs exposent MCP Streamable HTTP.

| Service | URL MCP | Outils |
|---|---|---|
| Customer Service | `http://localhost:8056/mcp` | `getAllCustomers`, `findCustomerById`, `saveCustomer` |
| EBank Service | `http://localhost:8057/mcp` | `getAllBankAccounts`, `getBankAccountById`, `saveBankAccount` |

Avec Node installé, lancer `npx @modelcontextprotocol/inspector` et sélectionner **Streamable HTTP**. Pour chaque service, connecter le client, ouvrir **Tools**, déclencher **List Tools**, puis appeler l’outil de liste. Tester ensuite `findCustomerById` avec l’ID 1 et un détail de compte avec l’UUID obtenu dans sa liste.

## Agent REST

Dans le terminal PowerShell qui démarre le bot, lire la clé sans l’afficher ni la conserver dans l’historique.

```powershell
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Clé OpenAI' -AsSecureString)).Password
java -jar ebank-bot/target/ebank-bot-1.0.0-SNAPSHOT.jar
```

Le bot démarre sur le port 8058. Tester via le dossier « Agent IA » de la collection Postman ou sur <http://localhost:8058/swagger-ui.html>. Demander « Liste les clients », puis « Donne-moi les comptes bancaires ». La requête accepte un UUID `conversationId` à conserver entre deux tours. Utiliser un UUID différent pour chaque conversation.

## Telegram

Créer le bot auprès de @BotFather, renseigner `TELEGRAM_BOT_TOKEN` dans le même terminal avant le démarrage et ouvrir une conversation privée. Envoyer « Liste les clients », puis une photo légendée « Décris cette image ». Le bot récupère les messages par long polling et utilise Spring AI pour le texte et les photos.

## Discord

Créer une application dans le portail développeur, activer **Message Content Intent** et inviter le bot avec les droits **View Channel** et **Send Messages**. Renseigner `DISCORD_TOKEN` avant le démarrage du bot. Depuis le serveur, envoyer `!ebank Liste les clients`. Un message privé accepte la question directement.

Les deux canaux sont indépendants et s’activent lorsque leur jeton est renseigné. Leurs tests réels nécessitent des bots enregistrés et des jetons valides. Les exemples utilisent les données fictives du devoir.
