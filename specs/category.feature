# language: fr
Fonctionnalité: Gestion des catégories de produits
  En tant qu'utilisateur de Takalo
  Je veux gérer les catégories de produits depuis l'interface
  Afin d'organiser mon catalogue et de retrouver facilement mes produits

  # Modèle:
  # - Une catégorie est identifiée par un libellé unique (entre 2 et 100 caractères).
  # - Une description libre peut être associée à la catégorie.
  # - La consultation est ouverte à tout utilisateur ayant la permission "category:read".
  # - La création, la modification et la suppression exigent la permission "category:manage".
  # - Tous les messages affichés à l'utilisateur sont en français.

  Contexte:
    Étant donné un utilisateur authentifié "alice" avec la permission "category:read"
    Et l'utilisateur "alice" possède la permission "category:manage"
    Et "alice" se trouve sur la page "Catégories"

  # ---------------------------------------------------------------------------
  # Création d'une catégorie
  # ---------------------------------------------------------------------------
  Scénario: 01 - Ouvrir le formulaire de création depuis la liste
    Quand "alice" clique sur le bouton "Nouvelle catégorie"
    Alors un formulaire "Créer une catégorie" s'affiche
    Et le champ "Libellé" est vide et a le focus
    Et le champ "Description" est vide
    Et le bouton "Enregistrer" est désactivé

  Scénario: 02 - Créer une catégorie avec libellé et description
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit:
      | Libellé     | Produits laitiers      |
      | Description | Lait, yaourts, fromages |
    Et "alice" clique sur "Enregistrer"
    Alors un message de succès "Catégorie créée" s'affiche
    Et la catégorie "Produits laitiers" apparaît dans la liste
    Et le formulaire est refermé

  Scénario: 03 - Créer une catégorie sans description
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit "Boulangerie" dans le champ "Libellé"
    Et "alice" clique sur "Enregistrer"
    Alors la catégorie "Boulangerie" apparaît dans la liste
    Et la description affichée pour "Boulangerie" est vide

  Scénario: 04 - Refuser la création quand le libellé est vide
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" laisse le champ "Libellé" vide
    Alors le bouton "Enregistrer" reste désactivé
    Et le message "Le libellé de la catégorie est obligatoire." s'affiche sous le champ "Libellé"

  Scénario: 05 - Refuser la création quand le libellé est trop court
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit "A" dans le champ "Libellé"
    Et "alice" clique sur "Enregistrer"
    Alors le message "Le libellé doit contenir entre 2 et 100 caractères" s'affiche sous le champ "Libellé"
    Et la catégorie n'est pas créée

  Scénario: 06 - Refuser la création quand le libellé dépasse 100 caractères
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit un libellé de 101 caractères
    Et "alice" clique sur "Enregistrer"
    Alors le message "Le libellé doit contenir entre 2 et 100 caractères" s'affiche sous le champ "Libellé"

  Scénario: 07 - Refuser la création d'une catégorie avec un libellé déjà existant
    Étant donné qu'une catégorie "Produits laitiers" existe déjà
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit "Produits laitiers" dans le champ "Libellé"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Une catégorie avec ce nom existe déjà." s'affiche
    Et le formulaire reste ouvert avec les valeurs saisies

  Scénario: 08 - Trim automatique des espaces autour du libellé
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit "  Surgelés  " dans le champ "Libellé"
    Et "alice" clique sur "Enregistrer"
    Alors la catégorie "Surgelés" apparaît dans la liste

  Scénario: 09 - Annuler la création depuis le formulaire
    Quand "alice" ouvre le formulaire de création de catégorie
    Et "alice" saisit "Boissons" dans le champ "Libellé"
    Et "alice" clique sur "Annuler"
    Alors le formulaire est refermé
    Et la catégorie "Boissons" n'apparaît pas dans la liste

  # ---------------------------------------------------------------------------
  # Consultation et recherche
  # ---------------------------------------------------------------------------
  Scénario: 10 - Afficher la liste paginée par défaut
    Étant donné qu'il existe 25 catégories
    Quand "alice" ouvre la page "Catégories"
    Alors 10 catégories sont affichées
    Et la pagination indique "Page 1 / 3"
    Et le bouton "Page suivante" est actif
    Et le bouton "Page précédente" est désactivé

  Scénario: 11 - Naviguer vers la page suivante
    Étant donné qu'il existe 25 catégories
    Et "alice" se trouve sur la page 1 de la liste des catégories
    Quand "alice" clique sur "Page suivante"
    Alors la page 2 affiche 10 catégories
    Et la pagination indique "Page 2 / 3"

  Scénario: 12 - Filtrer la liste par libellé
    Étant donné les catégories "Produits laitiers", "Produits secs", "Boulangerie"
    Quand "alice" saisit "produit" dans le champ de recherche
    Alors la liste affiche "Produits laitiers" et "Produits secs"
    Et la catégorie "Boulangerie" n'est pas affichée

  Scénario: 13 - Recherche sans résultat
    Étant donné les catégories "Produits laitiers", "Boulangerie"
    Quand "alice" saisit "viande" dans le champ de recherche
    Alors un message "Aucune catégorie ne correspond à votre recherche" s'affiche
    Et la liste est vide

  Scénario: 14 - Effacer le filtre de recherche
    Étant donné les catégories "Produits laitiers", "Boulangerie"
    Et "alice" a filtré la liste avec "lait"
    Quand "alice" efface le champ de recherche
    Alors la liste complète des catégories est affichée

  Scénario: 15 - Consulter le détail d'une catégorie
    Étant donné une catégorie "Produits laitiers" avec la description "Lait, yaourts, fromages"
    Quand "alice" clique sur la catégorie "Produits laitiers"
    Alors la page de détail affiche le libellé "Produits laitiers"
    Et la description "Lait, yaourts, fromages" est visible

  Scénario: 16 - Catégorie introuvable
    Quand "alice" ouvre l'URL d'une catégorie inexistante
    Alors un message "Catégorie introuvable" s'affiche
    Et un lien de retour vers la liste est proposé

  # ---------------------------------------------------------------------------
  # Mise à jour
  # ---------------------------------------------------------------------------
  Scénario: 17 - Ouvrir le formulaire d'édition pré-rempli
    Étant donné une catégorie "Produits laitiers" avec la description "Lait, yaourts"
    Quand "alice" clique sur "Modifier" pour la catégorie "Produits laitiers"
    Alors un formulaire "Modifier la catégorie" s'affiche
    Et le champ "Libellé" contient "Produits laitiers"
    Et le champ "Description" contient "Lait, yaourts"

  Scénario: 18 - Modifier le libellé d'une catégorie
    Étant donné une catégorie "Produits laitiers"
    Quand "alice" ouvre l'édition de la catégorie "Produits laitiers"
    Et "alice" remplace le libellé par "Produits frais"
    Et "alice" clique sur "Enregistrer"
    Alors un message de succès "Catégorie mise à jour" s'affiche
    Et la liste affiche "Produits frais" à la place de "Produits laitiers"

  Scénario: 19 - Modifier uniquement la description
    Étant donné une catégorie "Boulangerie" avec la description "Pain"
    Quand "alice" ouvre l'édition de la catégorie "Boulangerie"
    Et "alice" remplace la description par "Pain, viennoiseries, pâtisseries"
    Et "alice" clique sur "Enregistrer"
    Alors la nouvelle description est visible sur la page de détail

  Scénario: 20 - Refuser la modification si le libellé entre en conflit
    Étant donné les catégories "Produits laitiers" et "Boulangerie"
    Quand "alice" ouvre l'édition de la catégorie "Boulangerie"
    Et "alice" remplace le libellé par "Produits laitiers"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Une catégorie avec ce nom existe déjà." s'affiche
    Et la catégorie "Boulangerie" conserve son libellé d'origine

  Scénario: 21 - Refuser la modification avec un libellé vide
    Étant donné une catégorie "Boulangerie"
    Quand "alice" ouvre l'édition de la catégorie "Boulangerie"
    Et "alice" efface le champ "Libellé"
    Alors le bouton "Enregistrer" est désactivé
    Et le message "Le libellé de la catégorie est obligatoire." s'affiche

  Scénario: 22 - Annuler la modification
    Étant donné une catégorie "Boulangerie"
    Quand "alice" ouvre l'édition de la catégorie "Boulangerie"
    Et "alice" remplace le libellé par "Boulangerie artisanale"
    Et "alice" clique sur "Annuler"
    Alors le libellé reste "Boulangerie" dans la liste

  # ---------------------------------------------------------------------------
  # Suppression
  # ---------------------------------------------------------------------------
  Scénario: 23 - Demander confirmation avant suppression
    Étant donné une catégorie "Boulangerie"
    Quand "alice" clique sur "Supprimer" pour la catégorie "Boulangerie"
    Alors une boîte de dialogue "Supprimer la catégorie ?" s'affiche
    Et le message indique "Cette action est irréversible."
    Et deux boutons "Annuler" et "Confirmer" sont proposés

  Scénario: 24 - Confirmer la suppression d'une catégorie
    Étant donné une catégorie "Boulangerie"
    Quand "alice" supprime la catégorie "Boulangerie" et confirme
    Alors un message de succès "Catégorie supprimée" s'affiche
    Et la catégorie "Boulangerie" n'apparaît plus dans la liste

  Scénario: 25 - Annuler la suppression depuis la boîte de dialogue
    Étant donné une catégorie "Boulangerie"
    Quand "alice" clique sur "Supprimer" pour la catégorie "Boulangerie"
    Et "alice" clique sur "Annuler" dans la boîte de dialogue
    Alors la catégorie "Boulangerie" est toujours présente dans la liste

  # ---------------------------------------------------------------------------
  # Permissions et accès
  # ---------------------------------------------------------------------------
  Scénario: 26 - Utilisateur en lecture seule ne voit pas les actions de gestion
    Étant donné un utilisateur authentifié "bob" avec uniquement la permission "category:read"
    Quand "bob" ouvre la page "Catégories"
    Alors le bouton "Nouvelle catégorie" n'est pas affiché
    Et aucune action "Modifier" ou "Supprimer" n'est proposée sur les lignes

  Scénario: 27 - Utilisateur sans permission de lecture est redirigé
    Étant donné un utilisateur authentifié "carol" sans la permission "category:read"
    Quand "carol" tente d'ouvrir la page "Catégories"
    Alors un message "Accès refusé" s'affiche
    Et "carol" est redirigée vers la page d'accueil

  Scénario: 28 - Session expirée pendant la création
    Étant donné "alice" a ouvert le formulaire de création de catégorie
    Et la session de "alice" a expiré
    Quand "alice" clique sur "Enregistrer"
    Alors "alice" est redirigée vers la page de connexion
    Et un message "Votre session a expiré, veuillez vous reconnecter" s'affiche
