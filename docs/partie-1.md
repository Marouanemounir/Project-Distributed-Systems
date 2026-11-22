# Partie 1 — Microservices Spring Cloud

Prérequis : Java 21 et Maven 3.9+. Exécuter `mvn clean package` depuis la racine.

Démarrer chaque commande dans son propre terminal. Attendre que l'application soit accessible avant de poursuivre.

| Ordre | Commande | Port |
|---|---|---|
| 1 | `java -jar config-service/target/config-service-1.0.0-SNAPSHOT.jar` | 8888 |
| 2 | `java -jar discovery-service/target/discovery-service-1.0.0-SNAPSHOT.jar` | 8761 |
| 3 | `java -jar customer-service/target/customer-service-1.0.0-SNAPSHOT.jar` | 8056 |
| 4 | `java -jar ebank-service/target/ebank-service-1.0.0-SNAPSHOT.jar` | 8057 |
| 5 | `java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar` | 9999 |

Eureka s'ouvre sur <http://localhost:8761/>. Attendre que la gateway récupère le registre avant d'utiliser les routes.

- Clients via la gateway : <http://localhost:9999/CUSTOMER-SERVICE/customers>
- Comptes via la gateway : <http://localhost:9999/EBANK-SERVICE/accounts>
- Swagger UI : <http://localhost:8056/swagger-ui.html> et <http://localhost:8057/swagger-ui.html>
- Postman : importer [la collection](../postman/EBank.postman_collection.json) et lancer le dossier « Microservices ». La liste des comptes alimente automatiquement la variable utilisée par la requête de détail.

## Scénario de résilience

Relever un identifiant de compte sur la liste, puis consulter le détail. Arrêter Customer Service et répéter la consultation quatre fois. Le compte reste accessible avec un titulaire à l'état `Not Available`. Une création de compte renvoie HTTP 503 pendant la panne. Après le redémarrage du service, attendre sa réinscription dans Eureka et la fenêtre de réouverture du circuit. Consulter le détail deux fois pour vérifier sa récupération.

Les données H2 sont initialisées au démarrage avec trois clients fictifs et quinze comptes. Un redémarrage réinitialise les données du service concerné.
