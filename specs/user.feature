# language: fr
Fonctionnalité: Gestion des utilisateurs
  En tant qu'utilisateur de Takalo
  Je veux me connecter, consulter mon profil et — si je suis administrateur —
  gérer les rôles des autres utilisateurs depuis l'interface
  Afin d'accéder à l'application avec les bonnes permissions et de piloter
  les accès de l'équipe.

  # Modèle:
  # - L'authentification est déléguée à Keycloak (OIDC). Le frontend obtient un
  #   jeton JWT et l'envoie au backend dans l'en-tête Authorization.
  # - À la première connexion d'un utilisateur, le backend provisionne
  #   automatiquement un compte local rattaché à l'identité externe
  #   (externalId Keycloak), avec le rôle par défaut "USER".
  # - Le profil de l'utilisateur courant s'obtient via "GET /api/v1/users/me" et
  #   contient l'identifiant interne, l'email, le displayName, les rôles et
  #   l'ensemble des permissions effectives.
  # - L'interface s'appuie sur la liste des permissions retournée pour activer
  #   ou masquer les actions (boutons "Créer", "Supprimer", écrans d'admin…).
  # - L'administration des rôles (assignation, retrait, listing des rôles
  #   disponibles, accès au profil d'un autre utilisateur) exige la permission
  #   "user:manage".
  # - La recherche d'utilisateurs par nom ou email est ouverte aux utilisateurs
  #   authentifiés et sert notamment à choisir un éditeur lors de la gestion
  #   d'un budget (cf. budget.feature, scénarios 42-55).
  # - Tous les messages affichés à l'utilisateur sont en français.

  # ---------------------------------------------------------------------------
  # Connexion et provisioning automatique
  # ---------------------------------------------------------------------------
  Scénario: 01 - Première connexion provisionne automatiquement l'utilisateur
    Étant donné un utilisateur "alice" qui se connecte pour la première fois via Keycloak
    Quand le frontend appelle "GET /api/v1/users/me" avec un jeton JWT valide
    Alors la réponse a le statut 200
    Et un compte local est créé pour "alice" avec son externalId Keycloak
    Et le profil renvoyé contient le rôle "USER"
    Et le profil renvoyé contient au moins la permission "budget:read"

  Scénario: 02 - Connexion d'un utilisateur déjà provisionné
    Étant donné un utilisateur "alice" déjà provisionné avec les rôles "USER" et "ADMIN"
    Quand le frontend appelle "GET /api/v1/users/me" avec un jeton JWT valide
    Alors la réponse a le statut 200
    Et le profil renvoyé contient les rôles "USER" et "ADMIN"
    Et aucun nouveau compte n'est créé

  Scénario: 03 - Refuser l'accès au profil sans jeton
    Quand le frontend appelle "GET /api/v1/users/me" sans jeton
    Alors la réponse a le statut 401
    Et l'utilisateur est redirigé vers la page de connexion Keycloak

  Scénario: 04 - Refuser l'accès au profil avec un jeton expiré
    Étant donné un utilisateur "alice" déjà provisionné
    Quand le frontend appelle "GET /api/v1/users/me" avec un jeton expiré
    Alors la réponse a le statut 401
    Et l'utilisateur est invité à se reconnecter

  # ---------------------------------------------------------------------------
  # Profil de l'utilisateur courant
  # ---------------------------------------------------------------------------
  Scénario: 05 - Afficher le profil courant dans l'en-tête de l'application
    Étant donné un utilisateur authentifié "alice" avec le displayName "Alice Martin"
    Quand "alice" arrive sur la page d'accueil
    Alors l'en-tête affiche "Alice Martin"
    Et un bouton "Mon profil" est visible
    Et un bouton "Se déconnecter" est visible

  Scénario: 06 - Consulter la page "Mon profil"
    Étant donné un utilisateur authentifié "alice" avec l'email "alice@takalo.fr"
    Quand "alice" clique sur "Mon profil"
    Alors la page "Mon profil" s'affiche
    Et le champ "Email" affiche "alice@takalo.fr" en lecture seule
    Et le champ "Nom affiché" affiche "Alice Martin"
    Et la liste des rôles attribués est visible
    Et la liste des permissions effectives est visible

  Scénario: 07 - Les actions disponibles dépendent des permissions
    Étant donné un utilisateur authentifié "alice" sans la permission "category:manage"
    Quand "alice" se trouve sur la page "Catégories"
    Alors le bouton "Nouvelle catégorie" n'est pas visible
    Et les boutons "Modifier" et "Supprimer" ne sont pas visibles sur les lignes

  Scénario: 08 - Une permission accordée active immédiatement les actions correspondantes
    Étant donné un utilisateur authentifié "alice" avec la permission "category:manage"
    Quand "alice" se trouve sur la page "Catégories"
    Alors le bouton "Nouvelle catégorie" est visible et actif
    Et les boutons "Modifier" et "Supprimer" sont visibles sur chaque ligne

  Scénario: 09 - Se déconnecter
    Étant donné un utilisateur authentifié "alice"
    Quand "alice" clique sur "Se déconnecter"
    Alors la session Keycloak est terminée
    Et "alice" est redirigée vers la page de connexion
    Et le jeton local est effacé du stockage du navigateur

  # ---------------------------------------------------------------------------
  # Recherche d'utilisateurs (sélection d'un éditeur de budget, mentions, etc.)
  # ---------------------------------------------------------------------------
  Scénario: 10 - Rechercher un utilisateur par nom partiel
    Étant donné un utilisateur authentifié "alice"
    Et les utilisateurs suivants existent:
      | displayName  | email             |
      | Bob Dupont   | bob@takalo.fr     |
      | Bobby Singer | bobby@takalo.fr   |
      | Carol Smith  | carol@takalo.fr   |
    Quand "alice" saisit "bob" dans un champ de recherche d'utilisateurs
    Alors la liste des suggestions contient "Bob Dupont" et "Bobby Singer"
    Et la liste ne contient pas "Carol Smith"

  Scénario: 11 - Rechercher un utilisateur par email
    Étant donné un utilisateur authentifié "alice"
    Et un utilisateur "Bob Dupont" avec l'email "bob@takalo.fr"
    Quand "alice" saisit "bob@takalo" dans un champ de recherche d'utilisateurs
    Alors la liste des suggestions contient "Bob Dupont"

  Scénario: 12 - Une recherche sans résultat affiche un message explicite
    Étant donné un utilisateur authentifié "alice"
    Quand "alice" saisit "inconnu" dans un champ de recherche d'utilisateurs
    Alors le message "Aucun utilisateur trouvé" est affiché
    Et aucune suggestion n'est sélectionnable

  Scénario: 13 - L'utilisateur courant est exclu de ses propres résultats de recherche d'éditeurs
    Étant donné un utilisateur authentifié "alice"
    Quand "alice" saisit "ali" dans le champ "Ajouter un éditeur" d'un budget
    Alors "alice" n'apparaît pas dans la liste des suggestions

  # ---------------------------------------------------------------------------
  # Administration des rôles (réservée à "user:manage")
  # ---------------------------------------------------------------------------
  Scénario: 14 - Le menu "Administration" n'est visible que pour les administrateurs
    Étant donné un utilisateur authentifié "alice" sans la permission "user:manage"
    Quand "alice" arrive sur la page d'accueil
    Alors le menu "Administration" n'est pas visible

  Scénario: 15 - Accéder à la liste des utilisateurs en tant qu'administrateur
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Quand "alice" ouvre le menu "Administration" puis clique sur "Utilisateurs"
    Alors la page "Utilisateurs" s'affiche
    Et la liste contient tous les utilisateurs provisionnés avec leur email et leurs rôles

  Scénario: 16 - Ouvrir le détail d'un utilisateur
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont" avec les rôles "USER"
    Quand "alice" clique sur "Bob Dupont" dans la liste des utilisateurs
    Alors la page "Détail utilisateur" s'affiche
    Et la section "Rôles attribués" contient "USER"
    Et la section "Rôles disponibles" contient au moins "ADMIN"

  Scénario: 17 - Assigner un rôle à un utilisateur
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont" avec les rôles "USER"
    Quand "alice" assigne le rôle "ADMIN" à "Bob Dupont"
    Alors la réponse a le statut 200
    Et la section "Rôles attribués" contient "USER" et "ADMIN"
    Et un message de confirmation "Rôle assigné" est affiché

  Scénario: 18 - Retirer un rôle à un utilisateur
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont" avec les rôles "USER" et "ADMIN"
    Quand "alice" retire le rôle "ADMIN" de "Bob Dupont"
    Alors la réponse a le statut 200
    Et la section "Rôles attribués" contient uniquement "USER"

  Scénario: 19 - Refuser le retrait du dernier rôle d'un utilisateur
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont" avec le rôle unique "USER"
    Quand "alice" tente de retirer le rôle "USER" de "Bob Dupont"
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Un utilisateur doit conserver au moins un rôle"

  Scénario: 20 - Refuser l'assignation d'un rôle inexistant
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont"
    Quand "alice" tente d'assigner le rôle "INCONNU" à "Bob Dupont"
    Alors la réponse a le statut 404
    Et le message d'erreur indique "Rôle introuvable"

  Scénario: 21 - Refuser l'assignation d'un rôle déjà attribué
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Et un utilisateur "Bob Dupont" avec le rôle "ADMIN"
    Quand "alice" tente d'assigner à nouveau le rôle "ADMIN" à "Bob Dupont"
    Alors la réponse indique que le rôle est déjà attribué
    Et la liste des rôles attribués reste inchangée

  Scénario: 22 - Refuser l'accès à l'administration des utilisateurs sans la permission
    Étant donné un utilisateur authentifié "alice" sans la permission "user:manage"
    Quand "alice" tente d'accéder directement à l'URL "/admin/users"
    Alors la réponse a le statut 403
    Et un message "Accès refusé" est affiché

  Scénario: 23 - Lister les rôles disponibles pour le formulaire d'assignation
    Étant donné un utilisateur authentifié "alice" avec la permission "user:manage"
    Quand "alice" ouvre le sélecteur "Ajouter un rôle"
    Alors la liste contient au moins les rôles "USER" et "ADMIN"
    Et chaque rôle affiche son libellé et la liste de ses permissions

  Scénario: 24 - Refuser le listing des rôles sans la permission "user:manage"
    Étant donné un utilisateur authentifié "alice" sans la permission "user:manage"
    Quand le frontend appelle "GET /api/v1/roles"
    Alors la réponse a le statut 403

  # ---------------------------------------------------------------------------
  # Cohérence : la mise à jour des rôles se reflète dans la session courante
  # ---------------------------------------------------------------------------
  Scénario: 25 - Un utilisateur dont les rôles ont changé voit l'interface mise à jour à la prochaine actualisation
    Étant donné un utilisateur authentifié "bob" avec le rôle "USER"
    Et "bob" ne voit pas le menu "Administration"
    Quand un administrateur assigne le rôle "ADMIN" à "bob"
    Et que "bob" recharge la page ou que son jeton est rafraîchi
    Alors le menu "Administration" devient visible pour "bob"

  Scénario: 26 - Un utilisateur dont un rôle a été retiré perd les actions correspondantes après actualisation
    Étant donné un utilisateur authentifié "bob" avec les rôles "USER" et "ADMIN"
    Et "bob" voit le menu "Administration"
    Quand un administrateur retire le rôle "ADMIN" à "bob"
    Et que "bob" recharge la page ou que son jeton est rafraîchi
    Alors le menu "Administration" n'est plus visible pour "bob"
