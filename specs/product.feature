# language: fr
Fonctionnalité: Gestion des produits
  En tant qu'utilisateur de Takalo
  Je veux gérer mes produits depuis l'interface
  Afin de tenir un catalogue à jour rattaché à des catégories

  # Modèle:
  # - Un produit est identifié par un nom unique (entre 3 et 100 caractères).
  # - Un produit peut être rattaché à une catégorie existante (optionnel).
  # - La consultation est ouverte à tout utilisateur ayant la permission "product:read".
  # - La création, la modification et la suppression exigent la permission "product:write".
  # - Tous les messages affichés à l'utilisateur sont en français.

  Contexte:
    Étant donné un utilisateur authentifié "alice" avec la permission "product:read"
    Et l'utilisateur "alice" possède la permission "product:write"
    Et l'utilisateur "alice" possède la permission "category:read"
    Et les catégories suivantes existent:
      | Produits laitiers |
      | Boulangerie       |
    Et "alice" se trouve sur la page "Produits"

  # ---------------------------------------------------------------------------
  # Création d'un produit
  # ---------------------------------------------------------------------------
  Scénario: 01 - Ouvrir le formulaire de création depuis la liste
    Quand "alice" clique sur le bouton "Nouveau produit"
    Alors un formulaire "Créer un produit" s'affiche
    Et le champ "Nom" est vide et a le focus
    Et le champ "Catégorie" affiche "Aucune"
    Et le bouton "Enregistrer" est désactivé

  Scénario: 02 - Créer un produit rattaché à une catégorie
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Yaourt nature" dans le champ "Nom"
    Et "alice" sélectionne "Produits laitiers" dans le champ "Catégorie"
    Et "alice" clique sur "Enregistrer"
    Alors un message de succès "Produit créé" s'affiche
    Et le produit "Yaourt nature" apparaît dans la liste avec la catégorie "Produits laitiers"

  Scénario: 03 - Créer un produit sans catégorie
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Eau plate" dans le champ "Nom"
    Et "alice" laisse le champ "Catégorie" vide
    Et "alice" clique sur "Enregistrer"
    Alors le produit "Eau plate" apparaît dans la liste
    Et la colonne "Catégorie" pour "Eau plate" affiche "—"

  Scénario: 04 - Sélectionner une catégorie via une recherche
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" ouvre le sélecteur de catégorie
    Et "alice" saisit "boul" dans le champ de recherche du sélecteur
    Alors la catégorie "Boulangerie" est proposée
    Et la catégorie "Produits laitiers" n'est pas proposée

  Scénario: 05 - Refuser la création quand le nom est vide
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" laisse le champ "Nom" vide
    Alors le bouton "Enregistrer" reste désactivé
    Et le message "Le nom du produit est obligatoire." s'affiche sous le champ "Nom"

  Scénario: 06 - Refuser la création quand le nom est trop court
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Ya" dans le champ "Nom"
    Et "alice" clique sur "Enregistrer"
    Alors le message "Le nom doit contenir entre 3 et 100 caractères" s'affiche sous le champ "Nom"
    Et le produit n'est pas créé

  Scénario: 07 - Refuser la création quand le nom dépasse 100 caractères
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit un nom de 101 caractères
    Et "alice" clique sur "Enregistrer"
    Alors le message "Le nom doit contenir entre 3 et 100 caractères" s'affiche sous le champ "Nom"

  Scénario: 08 - Refuser la création d'un produit avec un nom déjà existant
    Étant donné qu'un produit "Yaourt nature" existe déjà
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Yaourt nature" dans le champ "Nom"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Un produit avec ce nom existe déjà" s'affiche
    Et le formulaire reste ouvert avec les valeurs saisies

  Scénario: 09 - Trim automatique des espaces autour du nom
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "  Pain de mie  " dans le champ "Nom"
    Et "alice" clique sur "Enregistrer"
    Alors le produit "Pain de mie" apparaît dans la liste

  Scénario: 10 - Refuser la création quand la catégorie sélectionnée n'existe plus
    Étant donné une catégorie "Surgelés" supprimée juste avant la soumission
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Glace vanille" dans le champ "Nom"
    Et "alice" sélectionne la catégorie "Surgelés"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Catégorie associée introuvable" s'affiche

  Scénario: 11 - Annuler la création depuis le formulaire
    Quand "alice" ouvre le formulaire de création de produit
    Et "alice" saisit "Camembert" dans le champ "Nom"
    Et "alice" clique sur "Annuler"
    Alors le formulaire est refermé
    Et le produit "Camembert" n'apparaît pas dans la liste

  # ---------------------------------------------------------------------------
  # Consultation, listing, recherche
  # ---------------------------------------------------------------------------
  Scénario: 12 - Afficher la liste paginée par défaut
    Étant donné qu'il existe 25 produits
    Quand "alice" ouvre la page "Produits"
    Alors 10 produits sont affichés
    Et la pagination indique "Page 1 / 3"
    Et le bouton "Page précédente" est désactivé
    Et le bouton "Page suivante" est actif

  Scénario: 13 - Naviguer vers la page suivante
    Étant donné qu'il existe 25 produits
    Et "alice" se trouve sur la page 1 de la liste des produits
    Quand "alice" clique sur "Page suivante"
    Alors la page 2 affiche 10 produits
    Et la pagination indique "Page 2 / 3"

  Scénario: 14 - Filtrer la liste par nom
    Étant donné les produits "Yaourt nature", "Yaourt fruits", "Pain de mie"
    Quand "alice" saisit "yaourt" dans le champ de recherche
    Alors la liste affiche "Yaourt nature" et "Yaourt fruits"
    Et le produit "Pain de mie" n'est pas affiché

  Scénario: 15 - Filtrer la liste par catégorie
    Étant donné les produits "Yaourt nature" (catégorie "Produits laitiers") et "Pain de mie" (catégorie "Boulangerie")
    Quand "alice" sélectionne la catégorie "Boulangerie" dans le filtre
    Alors la liste affiche uniquement "Pain de mie"

  Scénario: 16 - Filtrer par plusieurs catégories simultanément
    Étant donné les produits "Yaourt nature" (catégorie "Produits laitiers"), "Pain de mie" (catégorie "Boulangerie") et "Eau plate" sans catégorie
    Quand "alice" sélectionne les catégories "Produits laitiers" et "Boulangerie" dans le filtre
    Alors la liste affiche "Yaourt nature" et "Pain de mie"
    Et le produit "Eau plate" n'est pas affiché

  Scénario: 17 - Combiner recherche par nom et filtre par catégorie
    Étant donné les produits "Yaourt nature" (catégorie "Produits laitiers"), "Yaourt fruits" (catégorie "Produits laitiers") et "Pain de mie" (catégorie "Boulangerie")
    Quand "alice" saisit "yaourt" dans le champ de recherche
    Et "alice" sélectionne la catégorie "Boulangerie" dans le filtre
    Alors un message "Aucun produit ne correspond à votre recherche" s'affiche

  Scénario: 18 - Recherche sans résultat
    Étant donné les produits "Yaourt nature", "Pain de mie"
    Quand "alice" saisit "viande" dans le champ de recherche
    Alors un message "Aucun produit ne correspond à votre recherche" s'affiche
    Et la liste est vide

  Scénario: 19 - Effacer le filtre de recherche
    Étant donné les produits "Yaourt nature", "Pain de mie"
    Et "alice" a filtré la liste avec "yaourt"
    Quand "alice" efface le champ de recherche
    Alors la liste complète des produits est affichée

  Scénario: 20 - Consulter le détail d'un produit
    Étant donné un produit "Yaourt nature" rattaché à "Produits laitiers"
    Quand "alice" clique sur le produit "Yaourt nature"
    Alors la page de détail affiche le nom "Yaourt nature"
    Et la catégorie "Produits laitiers" est visible

  Scénario: 21 - Produit introuvable
    Quand "alice" ouvre l'URL d'un produit inexistant
    Alors un message "Produit introuvable" s'affiche
    Et un lien de retour vers la liste est proposé

  # ---------------------------------------------------------------------------
  # Mise à jour
  # ---------------------------------------------------------------------------
  Scénario: 22 - Ouvrir le formulaire d'édition pré-rempli
    Étant donné un produit "Yaourt nature" rattaché à "Produits laitiers"
    Quand "alice" clique sur "Modifier" pour le produit "Yaourt nature"
    Alors un formulaire "Modifier le produit" s'affiche
    Et le champ "Nom" contient "Yaourt nature"
    Et le champ "Catégorie" affiche "Produits laitiers"

  Scénario: 23 - Modifier le nom d'un produit
    Étant donné un produit "Yaourt nature"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" remplace le nom par "Yaourt nature 0%"
    Et "alice" clique sur "Enregistrer"
    Alors un message de succès "Produit mis à jour" s'affiche
    Et la liste affiche "Yaourt nature 0%" à la place de "Yaourt nature"

  Scénario: 24 - Changer la catégorie d'un produit
    Étant donné un produit "Yaourt nature" rattaché à "Produits laitiers"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" sélectionne "Boulangerie" dans le champ "Catégorie"
    Et "alice" clique sur "Enregistrer"
    Alors la page de détail affiche désormais la catégorie "Boulangerie"

  Scénario: 25 - Détacher un produit de toute catégorie
    Étant donné un produit "Yaourt nature" rattaché à "Produits laitiers"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" efface le champ "Catégorie"
    Et "alice" clique sur "Enregistrer"
    Alors la colonne "Catégorie" pour "Yaourt nature" affiche "—"

  Scénario: 26 - Refuser la modification si le nom entre en conflit
    Étant donné les produits "Yaourt nature" et "Pain de mie"
    Quand "alice" ouvre l'édition du produit "Pain de mie"
    Et "alice" remplace le nom par "Yaourt nature"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Un produit avec ce nom existe déjà" s'affiche
    Et le produit "Pain de mie" conserve son nom d'origine

  Scénario: 27 - Conserver le même nom pendant l'édition n'est pas un conflit
    Étant donné un produit "Yaourt nature" rattaché à "Produits laitiers"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" sélectionne "Boulangerie" dans le champ "Catégorie" sans changer le nom
    Et "alice" clique sur "Enregistrer"
    Alors la mise à jour est acceptée
    Et aucun message d'erreur d'unicité n'est affiché

  Scénario: 28 - Refuser la modification avec un nom vide
    Étant donné un produit "Yaourt nature"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" efface le champ "Nom"
    Alors le bouton "Enregistrer" est désactivé
    Et le message "Le nom du produit est obligatoire." s'affiche

  Scénario: 29 - Refuser la modification si la catégorie sélectionnée n'existe plus
    Étant donné un produit "Yaourt nature"
    Et une catégorie "Surgelés" supprimée juste avant la soumission
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" sélectionne la catégorie "Surgelés"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Produit ou catégorie introuvable" s'affiche

  Scénario: 30 - Annuler la modification
    Étant donné un produit "Yaourt nature"
    Quand "alice" ouvre l'édition du produit "Yaourt nature"
    Et "alice" remplace le nom par "Yaourt allégé"
    Et "alice" clique sur "Annuler"
    Alors le nom reste "Yaourt nature" dans la liste

  # ---------------------------------------------------------------------------
  # Suppression
  # ---------------------------------------------------------------------------
  Scénario: 31 - Demander confirmation avant suppression
    Étant donné un produit "Yaourt nature"
    Quand "alice" clique sur "Supprimer" pour le produit "Yaourt nature"
    Alors une boîte de dialogue "Supprimer le produit ?" s'affiche
    Et le message indique "Cette action est irréversible."
    Et deux boutons "Annuler" et "Confirmer" sont proposés

  Scénario: 32 - Confirmer la suppression d'un produit
    Étant donné un produit "Yaourt nature"
    Quand "alice" supprime le produit "Yaourt nature" et confirme
    Alors un message de succès "Produit supprimé" s'affiche
    Et le produit "Yaourt nature" n'apparaît plus dans la liste

  Scénario: 33 - Annuler la suppression depuis la boîte de dialogue
    Étant donné un produit "Yaourt nature"
    Quand "alice" clique sur "Supprimer" pour le produit "Yaourt nature"
    Et "alice" clique sur "Annuler" dans la boîte de dialogue
    Alors le produit "Yaourt nature" est toujours présent dans la liste

  # ---------------------------------------------------------------------------
  # Permissions et accès
  # ---------------------------------------------------------------------------
  Scénario: 34 - Utilisateur en lecture seule ne voit pas les actions de gestion
    Étant donné un utilisateur authentifié "bob" avec uniquement la permission "product:read"
    Quand "bob" ouvre la page "Produits"
    Alors le bouton "Nouveau produit" n'est pas affiché
    Et aucune action "Modifier" ou "Supprimer" n'est proposée sur les lignes

  Scénario: 35 - Utilisateur sans permission de lecture est redirigé
    Étant donné un utilisateur authentifié "carol" sans la permission "product:read"
    Quand "carol" tente d'ouvrir la page "Produits"
    Alors un message "Accès refusé" s'affiche
    Et "carol" est redirigée vers la page d'accueil

  Scénario: 36 - Session expirée pendant la création
    Étant donné "alice" a ouvert le formulaire de création de produit
    Et la session de "alice" a expiré
    Quand "alice" clique sur "Enregistrer"
    Alors "alice" est redirigée vers la page de connexion
    Et un message "Votre session a expiré, veuillez vous reconnecter" s'affiche
