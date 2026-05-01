# language: fr
Fonctionnalité: Gestion des achats
  En tant qu'utilisateur de Takalo
  Je veux saisir et consulter mes achats depuis l'interface
  Afin de suivre mes dépenses et de les rattacher à mes budgets

  # Modèle:
  # - Un achat appartient à un propriétaire (l'utilisateur authentifié à la création).
  # - Un achat contient au moins un article (produit, prix unitaire, quantité, remise optionnelle).
  # - Un achat peut être rattaché à un budget : si l'utilisateur a défini un budget par défaut,
  #   il est appliqué automatiquement quand le champ n'est pas renseigné.
  # - Pour rattacher un achat à un budget, l'utilisateur doit en être éditeur.
  # - Un utilisateur "purchase:read:own" ne voit et ne modifie que ses propres achats.
  # - Un utilisateur "purchase:read:any" voit ceux de tous.
  # - Un achat peut être réassigné à un autre budget (uniquement par son auteur, et il doit
  #   être éditeur de l'ancien et du nouveau budget).
  # - Tous les messages affichés à l'utilisateur sont en français.

  Contexte:
    Étant donné un utilisateur authentifié "alice" avec la permission "purchase:create"
    Et l'utilisateur "alice" possède la permission "purchase:read:own"
    Et l'utilisateur "alice" possède la permission "purchase:write"
    Et l'utilisateur "alice" possède la permission "product:read"
    Et l'utilisateur "alice" possède la permission "budget:read"
    Et les produits suivants existent:
      | Yaourt nature |
      | Pain de mie   |
      | Eau plate     |
    Et "alice" se trouve sur la page "Achats"

  # ---------------------------------------------------------------------------
  # Création d'un achat
  # ---------------------------------------------------------------------------
  Scénario: 01 - Ouvrir le formulaire de création depuis la liste
    Quand "alice" clique sur le bouton "Nouvel achat"
    Alors un formulaire "Créer un achat" s'affiche
    Et le champ "Date d'achat" est pré-rempli avec la date du jour
    Et une ligne d'article vide est présente
    Et le bouton "Enregistrer" est désactivé

  Scénario: 02 - Créer un achat à un seul article
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit l'article suivant:
      | Produit       | Yaourt nature |
      | Prix unitaire | 2.50          |
      | Quantité      | 2             |
    Et "alice" clique sur "Enregistrer"
    Alors un message de succès "Achat enregistré" s'affiche
    Et l'achat apparaît dans la liste avec un montant total de 5.00

  Scénario: 03 - Créer un achat avec plusieurs articles
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit les articles suivants:
      | Produit       | Prix unitaire | Quantité | Remise |
      | Yaourt nature | 2.50          | 2        | 0.00   |
      | Pain de mie   | 1.20          | 1        | 0.00   |
    Et "alice" clique sur "Enregistrer"
    Alors le montant total affiché est 6.20

  Scénario: 04 - Appliquer une remise sur un article
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit l'article suivant:
      | Produit       | Yaourt nature |
      | Prix unitaire | 2.50          |
      | Quantité      | 2             |
      | Remise        | 0.50          |
    Et "alice" clique sur "Enregistrer"
    Alors le montant total affiché est 4.50

  Scénario: 05 - Renseigner un magasin et une date de péremption
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit l'article suivant:
      | Produit            | Yaourt nature           |
      | Prix unitaire      | 2.50                    |
      | Quantité           | 2                       |
      | Magasin            | Carrefour Antananarivo  |
      | Date de péremption | 2026-12-31              |
    Et "alice" clique sur "Enregistrer"
    Alors la page de détail affiche le magasin "Carrefour Antananarivo"
    Et la date de péremption "31/12/2026" est visible sur l'article

  Scénario: 06 - Refuser la création sans aucun article
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" supprime l'unique ligne d'article
    Et "alice" clique sur "Enregistrer"
    Alors le message "L'achat doit contenir au moins un article." s'affiche

  Scénario: 07 - Refuser la création sans date d'achat
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" efface le champ "Date d'achat"
    Alors le message "La date d'achat est obligatoire." s'affiche
    Et le bouton "Enregistrer" reste désactivé

  Scénario: 08 - Refuser une date d'achat dans le futur
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit une date d'achat fixée à demain
    Alors le message "La date d'achat ne peut pas être dans le futur" s'affiche

  Scénario: 09 - Refuser une date de péremption dans le passé
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit l'article suivant:
      | Produit            | Yaourt nature |
      | Prix unitaire      | 2.50          |
      | Quantité           | 1             |
      | Date de péremption | 2020-01-01    |
    Et "alice" clique sur "Enregistrer"
    Alors le message "La date de péremption doit être aujourd'hui ou dans le futur" s'affiche

  Scénario: 10 - Refuser un prix unitaire négatif
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit un prix unitaire de "-1.00" pour un article
    Alors le message "Le prix unitaire ne peut pas être négatif." s'affiche

  Scénario: 11 - Accepter un prix unitaire à zéro (article gratuit)
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit l'article suivant:
      | Produit       | Yaourt nature |
      | Prix unitaire | 0.00          |
      | Quantité      | 1             |
    Et "alice" clique sur "Enregistrer"
    Alors l'achat est enregistré avec un montant total de 0.00

  Scénario: 12 - Refuser une quantité nulle ou négative
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit une quantité de "0" pour un article
    Alors le message "La quantité doit être positive" s'affiche

  Scénario: 13 - Refuser une remise négative
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit une remise de "-0.50" pour un article
    Alors le message "La remise ne peut être négatif." s'affiche

  Scénario: 14 - Choisir un produit via une recherche dans le sélecteur
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" ouvre le sélecteur de produit sur la première ligne d'article
    Et "alice" saisit "yaou" dans le champ de recherche du sélecteur
    Alors le produit "Yaourt nature" est proposé
    Et le produit "Pain de mie" n'est pas proposé

  Scénario: 15 - Refuser la création sans produit sélectionné sur un article
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" saisit prix et quantité sans choisir de produit
    Et "alice" clique sur "Enregistrer"
    Alors le message "L'identifiant du produit est obligatoire." s'affiche

  Scénario: 16 - Ajouter et supprimer une ligne d'article
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" clique sur "Ajouter un article"
    Alors une seconde ligne d'article apparaît
    Quand "alice" clique sur "Supprimer" sur la seconde ligne
    Alors il ne reste qu'une ligne d'article

  # ---------------------------------------------------------------------------
  # Rattachement à un budget
  # ---------------------------------------------------------------------------
  Scénario: 17 - Affecter automatiquement le budget par défaut
    Étant donné un budget "Courses" dont "alice" est éditrice
    Et "Courses" est défini comme budget par défaut de "alice"
    Quand "alice" crée un achat sans choisir de budget
    Alors l'achat est rattaché au budget "Courses"

  Scénario: 18 - Choisir explicitement un budget différent du défaut
    Étant donné un budget "Courses" dont "alice" est éditrice
    Et un budget "Loisirs" dont "alice" est éditrice
    Et "Courses" est défini comme budget par défaut de "alice"
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" sélectionne le budget "Loisirs"
    Et "alice" enregistre un article valide
    Alors l'achat est rattaché au budget "Loisirs"

  Scénario: 19 - Créer un achat sans aucun budget
    Étant donné aucun budget par défaut n'est défini pour "alice"
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" coche "Sans budget"
    Et "alice" enregistre un article valide
    Alors l'achat est créé sans budget associé
    Et la liste affiche "—" dans la colonne "Budget" pour cet achat

  Scénario: 20 - Refuser le rattachement à un budget dont l'utilisateur n'est pas éditeur
    Étant donné un budget "Famille" créé par "bob" dont "alice" n'est pas éditrice
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" tente de sélectionner le budget "Famille"
    Alors le budget "Famille" n'apparaît pas dans la liste des budgets sélectionnables

  Scénario: 21 - Refuser le rattachement quand le produit choisi n'existe plus
    Étant donné un produit "Glace vanille" supprimé juste avant la soumission
    Quand "alice" ouvre le formulaire de création d'achat
    Et "alice" sélectionne le produit "Glace vanille"
    Et "alice" clique sur "Enregistrer"
    Alors un message d'erreur "Produit référencé introuvable" s'affiche

  # ---------------------------------------------------------------------------
  # Consultation, listing, recherche
  # ---------------------------------------------------------------------------
  Scénario: 22 - Afficher la liste paginée par défaut
    Étant donné qu'il existe 25 achats appartenant à "alice"
    Quand "alice" ouvre la page "Achats"
    Alors 10 achats sont affichés
    Et la pagination indique "Page 1 / 3"

  Scénario: 23 - Naviguer vers la page suivante
    Étant donné qu'il existe 25 achats appartenant à "alice"
    Et "alice" se trouve sur la page 1 de la liste des achats
    Quand "alice" clique sur "Page suivante"
    Alors la page 2 affiche 10 achats

  Scénario: 24 - Filtrer par fenêtre temporelle
    Étant donné les achats suivants appartiennent à "alice":
      | Date           |
      | 2026-03-01     |
      | 2026-03-15     |
      | 2026-04-10     |
    Quand "alice" sélectionne la période du "2026-03-01" au "2026-03-31"
    Alors la liste affiche 2 achats
    Et l'achat du "2026-04-10" n'est pas affiché

  Scénario: 25 - Filtrer sur la date de début seulement
    Étant donné les achats suivants appartiennent à "alice":
      | Date           |
      | 2026-03-01     |
      | 2026-04-10     |
    Quand "alice" saisit uniquement la date de début "2026-04-01"
    Alors la liste affiche 1 achat
    Et seul l'achat du "2026-04-10" est affiché

  Scénario: 26 - Recherche sans résultat dans la fenêtre
    Étant donné qu'aucun achat de "alice" n'a été effectué en mai 2026
    Quand "alice" sélectionne la période du "2026-05-01" au "2026-05-31"
    Alors un message "Aucun achat dans cette période" s'affiche

  Scénario: 27 - Effacer le filtre temporel
    Étant donné "alice" a appliqué un filtre temporel
    Quand "alice" clique sur "Réinitialiser les filtres"
    Alors la liste complète des achats de "alice" est affichée

  Scénario: 28 - Consulter le détail d'un achat
    Étant donné un achat de "alice" contenant 2 articles
    Quand "alice" clique sur cet achat
    Alors la page de détail affiche les 2 articles avec prix, quantité et remise
    Et le montant total est affiché

  Scénario: 29 - Achat introuvable
    Quand "alice" ouvre l'URL d'un achat inexistant
    Alors un message "Achat introuvable" s'affiche
    Et un lien de retour vers la liste est proposé

  Scénario: 30 - Tentative d'accès à un achat appartenant à un autre utilisateur
    Étant donné un achat appartient à "bob"
    Quand "alice" ouvre l'URL de cet achat
    Alors un message "Accès refusé à cet achat" s'affiche

  Scénario: 31 - Lecture étendue avec "purchase:read:any"
    Étant donné un utilisateur "carol" avec la permission "purchase:read:any"
    Et il existe des achats appartenant à plusieurs utilisateurs
    Quand "carol" ouvre la page "Achats"
    Alors la liste affiche les achats de tous les utilisateurs

  Scénario: 32 - Affichage du nom du produit même si le produit a été supprimé
    Étant donné un achat de "alice" référençant un produit supprimé depuis
    Quand "alice" consulte cet achat
    Alors la ligne d'article affiche "Produit supprimé" comme nom

  # ---------------------------------------------------------------------------
  # Mise à jour d'un achat
  # ---------------------------------------------------------------------------
  Scénario: 33 - Modifier la date d'achat
    Étant donné un achat de "alice" daté du "2026-04-01"
    Quand "alice" ouvre l'édition de l'achat
    Et "alice" change la date d'achat pour "2026-04-05"
    Et "alice" clique sur "Enregistrer"
    Alors l'achat affiche la date "05/04/2026"

  Scénario: 34 - Modifier la quantité d'un article
    Étant donné un achat de "alice" avec un article "Yaourt nature" en quantité 2
    Quand "alice" ouvre l'édition de l'achat
    Et "alice" remplace la quantité par 3
    Et "alice" clique sur "Enregistrer"
    Alors le montant total est recalculé

  Scénario: 35 - Ajouter un article à un achat existant
    Étant donné un achat de "alice" avec un seul article
    Quand "alice" ouvre l'édition de l'achat
    Et "alice" ajoute un nouvel article "Pain de mie" à 1.20 en quantité 1
    Et "alice" clique sur "Enregistrer"
    Alors l'achat contient désormais 2 articles

  Scénario: 36 - Supprimer un article d'un achat existant
    Étant donné un achat de "alice" contenant 2 articles
    Quand "alice" ouvre l'édition de l'achat
    Et "alice" supprime le second article
    Et "alice" clique sur "Enregistrer"
    Alors l'achat ne contient plus qu'un article

  Scénario: 37 - Le budget reste inchangé lors d'une simple mise à jour
    Étant donné un achat de "alice" rattaché au budget "Courses"
    Quand "alice" ouvre l'édition de l'achat
    Et "alice" change la quantité d'un article
    Et "alice" clique sur "Enregistrer"
    Alors l'achat reste rattaché au budget "Courses"

  # ---------------------------------------------------------------------------
  # Réassignation à un autre budget
  # ---------------------------------------------------------------------------
  Scénario: 38 - Réassigner un achat à un autre budget
    Étant donné un achat de "alice" rattaché au budget "Courses"
    Et un budget "Loisirs" dont "alice" est éditrice
    Quand "alice" clique sur "Changer de budget" depuis le détail de l'achat
    Et "alice" sélectionne le budget "Loisirs"
    Et "alice" saisit la raison "Erreur d'imputation"
    Et "alice" clique sur "Confirmer"
    Alors l'achat est désormais rattaché au budget "Loisirs"
    Et un message "Achat réassigné" s'affiche

  Scénario: 39 - Refuser la réassignation par un autre utilisateur que l'auteur
    Étant donné un achat appartient à "bob" et est rattaché au budget "Courses"
    Et "alice" est éditrice de "Courses"
    Quand "alice" tente de réassigner cet achat à un autre budget
    Alors un message "Accès refusé à cet achat" s'affiche

  Scénario: 40 - Refuser la réassignation si non éditeur du nouveau budget
    Étant donné un achat de "alice" rattaché au budget "Courses"
    Et un budget "Famille" dont "alice" n'est pas éditrice
    Quand "alice" tente de réassigner l'achat vers "Famille"
    Alors un message "Vous n'êtes pas éditeur de ce budget" s'affiche

  Scénario: 41 - Réassignation impossible vers un budget inexistant
    Étant donné un achat de "alice"
    Quand "alice" tente de réassigner l'achat vers un budget supprimé
    Alors un message d'erreur "Achat ou budget introuvable" s'affiche

  # ---------------------------------------------------------------------------
  # Suppression
  # ---------------------------------------------------------------------------
  Scénario: 42 - Demander confirmation avant suppression
    Étant donné un achat de "alice"
    Quand "alice" clique sur "Supprimer" pour cet achat
    Alors une boîte de dialogue "Supprimer l'achat ?" s'affiche
    Et le message indique "Cette action est irréversible."
    Et deux boutons "Annuler" et "Confirmer" sont proposés

  Scénario: 43 - Confirmer la suppression d'un achat
    Étant donné un achat de "alice"
    Quand "alice" supprime l'achat et confirme
    Alors un message de succès "Achat supprimé" s'affiche
    Et l'achat n'apparaît plus dans la liste

  Scénario: 44 - Refuser la suppression d'un achat appartenant à un autre utilisateur
    Étant donné un achat appartient à "bob"
    Quand "alice" tente de supprimer cet achat
    Alors un message "Accès refusé à cet achat" s'affiche

  Scénario: 45 - Annuler la suppression depuis la boîte de dialogue
    Étant donné un achat de "alice"
    Quand "alice" clique sur "Supprimer" puis sur "Annuler"
    Alors l'achat est toujours présent dans la liste

  # ---------------------------------------------------------------------------
  # Import depuis un fichier
  # ---------------------------------------------------------------------------
  Scénario: 46 - Importer des achats depuis un fichier Excel
    Étant donné l'utilisateur "alice" possède la permission "purchase:import"
    Et un fichier "achats.xlsx" contenant 3 lignes valides
    Quand "alice" clique sur "Importer"
    Et "alice" sélectionne le fichier "achats.xlsx"
    Et "alice" valide l'import
    Alors un récapitulatif indique "3 achats importés"
    Et 0 erreur n'est affichée

  Scénario: 47 - Importer un fichier contenant des lignes invalides
    Étant donné l'utilisateur "alice" possède la permission "purchase:import"
    Et un fichier "achats.xlsx" contenant 2 lignes valides et 1 ligne en erreur
    Quand "alice" importe le fichier "achats.xlsx"
    Alors un récapitulatif indique "2 achats importés, 1 erreur"
    Et le détail de l'erreur précise la ligne et la cause

  Scénario: 48 - Refuser un format de fichier non supporté
    Étant donné l'utilisateur "alice" possède la permission "purchase:import"
    Quand "alice" tente d'importer un fichier "achats.pdf"
    Alors un message d'erreur "Format de fichier non supporté" s'affiche

  Scénario: 49 - Utilisateur sans permission d'import ne voit pas le bouton
    Étant donné "alice" ne possède pas la permission "purchase:import"
    Quand "alice" ouvre la page "Achats"
    Alors le bouton "Importer" n'est pas affiché

  # ---------------------------------------------------------------------------
  # Permissions et accès
  # ---------------------------------------------------------------------------
  Scénario: 50 - Utilisateur sans permission de création ne voit pas le bouton
    Étant donné un utilisateur "dan" avec uniquement la permission "purchase:read:own"
    Quand "dan" ouvre la page "Achats"
    Alors le bouton "Nouvel achat" n'est pas affiché

  Scénario: 51 - Utilisateur sans permission de lecture est redirigé
    Étant donné un utilisateur authentifié "carol" sans permission de lecture des achats
    Quand "carol" tente d'ouvrir la page "Achats"
    Alors un message "Accès refusé" s'affiche
    Et "carol" est redirigée vers la page d'accueil

  Scénario: 52 - Session expirée pendant la création
    Étant donné "alice" a ouvert le formulaire de création d'achat
    Et la session de "alice" a expiré
    Quand "alice" clique sur "Enregistrer"
    Alors "alice" est redirigée vers la page de connexion
    Et un message "Votre session a expiré, veuillez vous reconnecter" s'affiche

  # ---------------------------------------------------------------------------
  # Vue détaillée des lignes d'achats (filtres + tri)
  # Endpoint backend : GET /api/v1/purchases/items
  # Colonnes : date, produit, catégorie, prix unitaire, quantité, remise, total, magasin.
  # ---------------------------------------------------------------------------
  Scénario: 53 - Afficher les lignes d'achats triées par date décroissante par défaut
    Étant donné les achats suivants existent pour "alice":
      | Date       | Produit       | Catégorie         | Prix unitaire | Quantité | Remise | Magasin   |
      | 2026-01-10 | Pain de mie   | Boulangerie       | 1.50          | 1        | 0.00   | Carrefour |
      | 2026-04-20 | Yaourt nature | Produits laitiers | 2.10          | 3        | 0.30   | Leclerc   |
    Quand "alice" ouvre la page "Lignes d'achats"
    Alors un tableau s'affiche avec les colonnes "Date", "Produit", "Catégorie", "Prix unitaire", "Quantité", "Remise", "Total", "Magasin"
    Et la première ligne du tableau correspond à l'achat du "2026-04-20"
    Et la colonne "Total" de la première ligne affiche "6.00"
    Et la deuxième ligne du tableau correspond à l'achat du "2026-01-10"

  Scénario: 54 - Trier les lignes par produit
    Étant donné des lignes d'achats existent pour "alice"
    Quand "alice" clique sur l'en-tête de colonne "Produit"
    Alors les lignes sont triées par nom de produit en ordre croissant
    Et un indicateur de tri ascendant est visible sur la colonne "Produit"
    Quand "alice" clique à nouveau sur l'en-tête "Produit"
    Alors les lignes sont triées par nom de produit en ordre décroissant

  Scénario: 55 - Trier les lignes par catégorie
    Étant donné des lignes d'achats existent pour "alice" portant sur les catégories "Boulangerie" et "Produits laitiers"
    Quand "alice" clique sur l'en-tête de colonne "Catégorie"
    Alors les lignes "Boulangerie" apparaissent avant les lignes "Produits laitiers"

  Scénario: 56 - Filtrer les lignes par intervalle de dates
    Étant donné des lignes d'achats existent pour "alice" entre "2026-01-01" et "2026-04-30"
    Quand "alice" saisit "2026-04-01" dans le champ "Du"
    Et "alice" saisit "2026-04-30" dans le champ "Au"
    Et "alice" valide les filtres
    Alors seules les lignes dont la date d'achat est comprise entre "2026-04-01" et "2026-04-30" sont affichées

  Scénario: 57 - Filtrer les lignes par nom de produit (insensible à la casse)
    Étant donné des lignes d'achats existent avec les produits "Yaourt nature" et "Pain de mie"
    Quand "alice" saisit "yaourt" dans le champ "Produit"
    Et "alice" valide les filtres
    Alors toutes les lignes affichées contiennent "Yaourt" dans la colonne "Produit"
    Et aucune ligne "Pain de mie" n'est affichée

  Scénario: 58 - Filtrer les lignes par nom de catégorie
    Étant donné des lignes d'achats existent dans les catégories "Boulangerie" et "Produits laitiers"
    Quand "alice" saisit "laitiers" dans le champ "Catégorie"
    Et "alice" valide les filtres
    Alors toutes les lignes affichées appartiennent à la catégorie "Produits laitiers"

  Scénario: 59 - Combiner plusieurs filtres
    Étant donné des lignes d'achats variées existent pour "alice"
    Quand "alice" saisit "lait" dans le champ "Produit"
    Et "alice" saisit "2026-04-01" dans le champ "Du"
    Et "alice" valide les filtres
    Alors les lignes affichées correspondent au produit recherché ET à la fenêtre temporelle

  Scénario: 60 - Pagination des lignes d'achats
    Étant donné 25 lignes d'achats existent pour "alice"
    Quand "alice" ouvre la page "Lignes d'achats"
    Alors 10 lignes sont affichées
    Et un contrôle de pagination indique 3 pages
    Quand "alice" clique sur "Page suivante"
    Alors les lignes 11 à 20 sont affichées

  Scénario: 61 - Afficher la catégorie courante (et non figée) du produit
    Étant donné un achat "alice" avec un article "Yaourt nature" rattaché à la catégorie "Produits laitiers"
    Et la catégorie du produit "Yaourt nature" est renommée en "Frais"
    Quand "alice" ouvre la page "Lignes d'achats"
    Alors la ligne correspondante affiche "Frais" dans la colonne "Catégorie"

  Scénario: 62 - Afficher une ligne dont le produit a été supprimé
    Étant donné un achat de "alice" avec un article "Yaourt nature" dont le produit a été supprimé
    Quand "alice" ouvre la page "Lignes d'achats"
    Alors la ligne affiche "Yaourt nature" (snapshot) dans la colonne "Produit"
    Et la colonne "Catégorie" est vide

  Scénario: 63 - Un utilisateur "purchase:read:own" ne voit que ses propres lignes
    Étant donné un utilisateur "bob" avec uniquement la permission "purchase:read:own"
    Et des lignes d'achats existent pour "alice" et pour "bob"
    Quand "bob" ouvre la page "Lignes d'achats"
    Alors seules les lignes des achats de "bob" sont affichées

  Scénario: 64 - Un utilisateur "purchase:read:any" voit toutes les lignes
    Étant donné un utilisateur "carol" avec la permission "purchase:read:any"
    Et des lignes d'achats existent pour "alice" et pour "bob"
    Quand "carol" ouvre la page "Lignes d'achats"
    Alors les lignes de "alice" et celles de "bob" sont affichées

  Scénario: 65 - Calcul du total par ligne (prix unitaire * quantité - remise)
    Étant donné une ligne d'achat avec prix unitaire 2.10, quantité 3 et remise 0.30
    Quand "alice" ouvre la page "Lignes d'achats"
    Alors la colonne "Total" de cette ligne affiche "6.00"

  Scénario: 66 - Aucune ligne ne correspond aux filtres
    Étant donné des lignes d'achats existent pour "alice"
    Quand "alice" saisit "produit-inexistant-xyz" dans le champ "Produit"
    Et "alice" valide les filtres
    Alors un message "Aucune ligne ne correspond aux critères" s'affiche
    Et le tableau est vide