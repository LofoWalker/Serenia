# Serenia Backend – Workflow Fonctionnel
6. Suppression de compte (`DELETE /api/auth/me`).
   - récupération de l’historique (`GET /api/conversations/{conversationId}/messages`).
   - envoi de message (`POST /api/conversations/add-message`),
5. Utilisation du chat :
4. Consultation du profil (`GET /api/auth/me`).
3. Login (`POST /api/auth/login`) → obtention du JWT et du profil.
2. Activation (`GET /api/auth/activate?token=...`).
1. Inscription (`POST /api/auth/register`) → email d’activation.

En résumé, le flux côté backend pour un utilisateur est :

## 2. Résumé du parcours

---

   - redirige l’utilisateur vers une page publique (ex. écran d’accueil ou confirmation de suppression).
   - supprime le JWT et toutes les données locales liées à l’utilisateur,
5. Le front :
   - renvoie `204 No Content`.
   - supprime le compte et les données associées (conversations, messages, etc.),
   - identifie l’utilisateur à partir du JWT,
4. Le backend :
3. Une fois confirmée, le front appelle `DELETE /api/auth/me` avec le JWT.
2. Le front demande une confirmation explicite.
1. L’utilisateur déclenche une action de suppression de compte depuis l’interface.

### 1.8. Suppression de compte

3. Le front affiche la liste des messages dans l’ordre chronologique.
     - `timestamp`.
     - `content`,
     - `role` (`user` ou `assistant`),
   - renvoie une liste de messages (`ChatMessage`) avec :
   - vérifie que la conversation appartient au même utilisateur que celui identifié par le JWT,
2. Le backend :
     - header `Authorization: Bearer <jwt>`.
     - `conversationId` : identifiant de la conversation,
   - `GET /api/conversations/{conversationId}/messages` avec :
1. Pour afficher l’historique d’une conversation (par ex. lors du rechargement de la page), le front appelle :

### 1.7. Récupération de l’historique des messages

5. Le front met à jour l’affichage du chat.
   - renvoie un nouveau `MessageResponseDTO` avec le même `conversationId`.
   - interroge l’IA,
   - ajoute le message utilisateur,
   - retrouve la conversation associée à l’utilisateur,
4. Le backend :
3. Le front appelle à nouveau `POST /api/conversations/add-message` avec `content` et le JWT.
2. L’utilisateur saisit un nouveau message.
   - soit recharger l’historique avant d’afficher la vue (voir section suivante).
   - soit continuer d’utiliser `POST /api/conversations/add-message` pour envoyer de nouveaux messages dans le contexte de cette conversation (le backend se charge d’associer le message à la bonne conversation),
1. Sur la page de chat, si le front connaît déjà un `conversationId` (par ex. stocké en mémoire ou en base locale), il peut :

### 1.6. Continuer une conversation existante

   - stocke `conversationId` pour les échanges futurs.
   - affiche le message utilisateur et la réponse IA,
6. Le front :
     - `content` : texte de la réponse de l’IA.
     - `role` : rôle du message renvoyé (`assistant`),
     - `conversationId` : identifiant de la conversation,
   - renvoie un `MessageResponseDTO` :
   - enregistre la réponse IA,
   - envoie la requête au moteur IA,
   - enregistre le message utilisateur,
   - récupère ou crée une conversation associée à l’utilisateur,
   - identifie l’utilisateur à partir du JWT,
5. Le backend :
   - `content` (obligatoire, non vide).
4. Lorsque l’utilisateur envoie ce message, le front appelle `POST /api/conversations/add-message` avec un `MessageRequestDTO` :
3. Le front permet à l’utilisateur de saisir un premier message.
2. Le front vérifie la présence d’un JWT (sinon redirection vers la page de login).
1. L’utilisateur accède à une page de chat (potentiellement vide s’il n’y a pas encore de messages).

### 1.5. Accès au chat / première conversation

4. Le front affiche les informations de profil (nom, prénom, email) et adapte l’interface en fonction des rôles.
   - renvoie un `UserResponseDTO`.
   - récupère les informations utilisateur,
   - extrait l’identité du JWT,
3. Le backend :
2. Le front envoie le header `Authorization: Bearer <jwt>`.
1. Pour afficher le profil ou initialiser l’état global du front, celui-ci appelle `GET /api/auth/me`.

### 1.4. Accès à l’application (profil / zone membre)

4. Le front stocke le JWT et le profil utilisateur, puis redirige vers la zone authentifiée.
     - `token` (JWT).
     - `user` (`UserResponseDTO` : id, nom, prénom, email, rôles)
   - renvoie un `AuthResponseDTO` contenant :
   - génère un JWT,
   - vérifie que le compte est activé,
   - vérifie les identifiants,
3. Le backend :
   - `password` (obligatoire)
   - `email` (obligatoire, email valide)
2. Le front appelle `POST /api/auth/login` avec un `LoginRequestDTO` :
   - `password`
   - `email`
1. Le front affiche un formulaire de connexion avec :

### 1.3. Login

5. Le front affiche un message de succès et propose à l’utilisateur de se connecter.
   - renvoie un `ActivationResponseDTO` avec un message de confirmation.
   - active le compte utilisateur,
   - vérifie la validité du token (existant, non expiré, non déjà utilisé),
4. Le backend :
   - soit récupérer le `token` dans l’URL et appeler `GET /api/auth/activate`.
   - soit rediriger directement le navigateur vers l’URL backend,
3. Le front peut :
2. Ce lien contient un paramètre `token` qui pointe vers `GET /api/auth/activate?token=...`.
1. L’utilisateur clique sur le lien reçu par email.

### 1.2. Activation de compte

4. Le front affiche un message informant que l’utilisateur doit vérifier ses emails.
   - déclenche l’envoi d’un email avec un lien d’activation.
   - crée l’utilisateur,
3. Le backend :
   - `password` (obligatoire, min 6 caractères)
   - `email` (obligatoire, email valide)
   - `firstName` (obligatoire, non vide)
   - `lastName` (obligatoire, non vide)
2. Le front appelle `POST /api/auth/register` avec un `RegistrationRequestDTO` :
   - `password`
   - `email`
   - `firstName`
   - `lastName`
1. Le front affiche un formulaire d’inscription avec :

### 1.1. Inscription

## 1. Parcours standard utilisateur

---

Ce document décrit le parcours fonctionnel complet côté backend pour l’utilisateur de Serenia.


