# language: fr
Fonctionnalité: Gestion des budgets
  En tant qu'utilisateur de Takalo
  Je veux gérer des budgets dotés d'un fond
  Afin de suivre mes achats et de répartir mes dépenses entre plusieurs enveloppes

  # Modèle:
  # - Un budget a un créateur (immuable) et une liste d'éditeurs (utilisateurs autorisés à le modifier).
  # - À la création, la liste contient uniquement le créateur.
  # - Toute modification du budget (fond, transfert, crédit, suppression, assignation d'un achat)
  #   exige d'appartenir à la liste des éditeurs.
  # - Seul le créateur peut ajouter ou retirer des utilisateurs de cette liste.
  # - Un budget est visible par tous les utilisateurs authentifiés.
  # - L'auteur d'un achat est conservé sur l'achat même s'il/elle n'est plus éditeur du budget.

  Contexte:
    Étant donné un utilisateur authentifié "alice" avec la permission "budget:create"
    Et l'utilisateur "alice" possède la permission "budget:read"
    Et l'utilisateur "alice" possède la permission "budget:write"
    Et l'utilisateur "alice" possède la permission "budget:manage-editors"
    Et l'utilisateur "alice" possède la permission "purchase:create"

  # ---------------------------------------------------------------------------
  # Création d'un budget
  # ---------------------------------------------------------------------------
  Scénario: 01 - Créer un budget avec un fond positif
    Quand "alice" crée un budget avec:
      | nom         | Courses     |
      | description | Alimentaire |
      | fond        | 500.00      |
    Alors la réponse a le statut 201
    Et le budget créé a pour créateur "alice"
    Et la liste des éditeurs du budget contient uniquement "alice"
    Et le fond du budget vaut 500.00
    Et le reste du budget vaut 500.00

  Scénario: 02 - Créer un budget avec un fond négatif (découvert autorisé)
    Quand "alice" crée un budget avec:
      | nom  | Avance salaire |
      | fond | -200.00        |
    Alors la réponse a le statut 201
    Et le fond du budget vaut -200.00
    Et le reste du budget vaut -200.00

  Scénario: 03 - Créer un budget avec un fond à zéro
    Quand "alice" crée un budget avec:
      | nom  | Loisirs |
      | fond | 0.00    |
    Alors la réponse a le statut 201
    Et le reste du budget vaut 0.00

  Scénario: 04 - Refuser la création d'un budget sans nom
    Quand "alice" crée un budget avec:
      | nom  |        |
      | fond | 100.00 |
    Alors la réponse a le statut 400
    Et le message d'erreur mentionne le champ "nom"

  Scénario: 05 - Refuser la création d'un budget avec un nom déjà utilisé (unicité globale)
    Étant donné que "alice" a déjà créé un budget nommé "Courses"
    Quand "alice" crée un budget avec:
      | nom  | Courses |
      | fond | 100.00  |
    Alors la réponse a le statut 409
    Et le message d'erreur indique "Un budget avec ce nom existe déjà"

  Scénario: 06 - Refuser la création d'un budget avec un nom déjà utilisé par un autre créateur
    Étant donné que "alice" a déjà créé un budget nommé "Courses"
    Et un utilisateur authentifié "bob" avec la permission "budget:create"
    Quand "bob" crée un budget avec:
      | nom  | Courses |
      | fond | 250.00  |
    Alors la réponse a le statut 409
    Et le message d'erreur indique "Un budget avec ce nom existe déjà"

  # ---------------------------------------------------------------------------
  # Consultation et calcul du reste (lecture ouverte à tous)
  # ---------------------------------------------------------------------------
  Scénario: 07 - Consulter le reste d'un budget sans achats
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" consulte le budget "Courses"
    Alors la réponse a le statut 200
    Et le fond du budget vaut 500.00
    Et le total des achats du budget vaut 0.00
    Et le reste du budget vaut 500.00

  Scénario: 08 - Le reste d'un budget est égal au fond moins la somme des achats associés
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et les achats suivants associés au budget "Courses":
      | montant |
      | 120.00  |
      | 80.50   |
      | 49.50   |
    Quand "alice" consulte le budget "Courses"
    Alors le total des achats du budget vaut 250.00
    Et le reste du budget vaut 250.00

  Scénario: 09 - Le reste d'un budget peut devenir négatif lorsque les achats dépassent le fond
    Étant donné un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Et les achats suivants associés au budget "Loisirs":
      | montant |
      | 70.00   |
      | 60.00   |
    Quand "alice" consulte le budget "Loisirs"
    Alors le total des achats du budget vaut 130.00
    Et le reste du budget vaut -30.00

  Scénario: 10 - Lister tous les budgets du système avec leur reste, leur créateur et leurs éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00 et 200.00 d'achats
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00 et 25.00 d'achats
    Et un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" liste les budgets
    Alors la réponse a le statut 200
    Et la liste contient exactement 3 budgets
    Et le budget "Courses" a un reste de 300.00 et pour créateur "alice"
    Et le budget "Loisirs" a un reste de 75.00 et pour créateur "alice"
    Et le budget "Voyages" a un reste de 1000.00 et pour créateur "bob"

  Scénario: 11 - Consulter un budget créé par un autre utilisateur est autorisé
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" consulte le budget "Voyages"
    Alors la réponse a le statut 200
    Et le budget renvoyé a pour créateur "bob"
    Et le fond du budget vaut 1000.00

  Scénario: 12 - Renvoyer 404 pour un budget inexistant
    Quand "alice" consulte un budget avec l'identifiant "00000000-0000-0000-0000-000000000000"
    Alors la réponse a le statut 404

  # ---------------------------------------------------------------------------
  # Mise à jour des méta-données d'un budget (réservée aux éditeurs)
  # Le fond initial est immuable : pour ajuster, utiliser un crédit (voir 30-36).
  # ---------------------------------------------------------------------------
  Scénario: 13 - Refuser la modification du fond initial d'un budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" met à jour le budget "Courses" avec un fond de 800.00
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Le fond initial d'un budget ne peut pas être modifié, utiliser un crédit"
    Et le fond du budget "Courses" vaut 500.00

  Scénario: 14 - Mettre à jour le nom et la description d'un budget sans toucher au fond
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00 et 200.00 d'achats
    Quand "alice" met à jour le budget "Courses" avec:
      | nom         | Alimentation                  |
      | description | Courses hebdomadaires + extra |
    Alors la réponse a le statut 200
    Et le nom du budget vaut "Alimentation"
    Et la description du budget vaut "Courses hebdomadaires + extra"
    Et le fond du budget vaut 500.00
    Et le reste du budget vaut 300.00

  # ---------------------------------------------------------------------------
  # Association achat / budget (réservée aux éditeurs du budget cible)
  # ---------------------------------------------------------------------------
  Scénario: 15 - Associer un achat à un budget lors de sa création (auteur éditeur du budget)
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" crée un achat de 75.00 associé au budget "Courses"
    Alors la réponse a le statut 201
    Et l'achat est associé au budget "Courses"
    Et l'achat a pour auteur "alice"
    Et le reste du budget "Courses" vaut 425.00

  Scénario: 16 - Refuser l'assignation d'un achat à un budget par un non-éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" crée un achat de 50.00 associé au budget "Voyages"
    Alors la réponse a le statut 403

  Scénario: 17 - Refuser un achat associé à un budget inexistant
    Quand "alice" crée un achat de 50.00 associé au budget "00000000-0000-0000-0000-000000000000"
    Alors la réponse a le statut 404

  Scénario: 18 - Un achat sans budget est autorisé et n'impacte aucun reste
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" crée un achat de 30.00 sans budget associé
    Alors la réponse a le statut 201
    Et le reste du budget "Courses" vaut 500.00

  Scénario: 19 - Réassigner un achat à un autre budget (auteur éditeur des deux budgets)
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 200.00
    Et un achat de 80.00 par "alice" associé au budget "Courses"
    Quand "alice" réassigne cet achat au budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Mauvaise affectation |
    Alors le reste du budget "Courses" vaut 500.00
    Et le reste du budget "Loisirs" vaut 120.00
    Et un mouvement de "DESASSIGNATION_ACHAT" est enregistré sur le budget "Courses" avec la date "2026-04-29T10:00:00Z" et la raison "Mauvaise affectation"
    Et un mouvement de "ASSIGNATION_ACHAT" est enregistré sur le budget "Loisirs" avec la date "2026-04-29T10:00:00Z" et la raison "Mauvaise affectation"

  Scénario: 20 - Supprimer un achat libère le fond du budget associé
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un achat de 120.00 par "alice" associé au budget "Courses"
    Quand "alice" supprime cet achat
    Alors le reste du budget "Courses" vaut 500.00

  # ---------------------------------------------------------------------------
  # Transfert de fond entre budgets (réservé aux éditeurs des deux budgets)
  # Tout transfert exige une date de transfert et une raison.
  # ---------------------------------------------------------------------------
  Scénario: 21 - Transférer un montant d'un budget à un autre avec date et raison
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Quand "alice" transfère 150.00 du budget "Courses" vers le budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z         |
      | raison | Réallocation de fin de mois  |
    Alors la réponse a le statut 200
    Et le fond du budget "Courses" vaut 350.00
    Et le fond du budget "Loisirs" vaut 250.00
    Et un mouvement de transfert de 150.00 est enregistré du budget "Courses" vers le budget "Loisirs" avec la date "2026-04-29T10:00:00Z" et la raison "Réallocation de fin de mois"

  Scénario: 22 - Le transfert n'affecte pas les achats déjà associés à chaque budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00 et 200.00 d'achats
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00 et 25.00 d'achats
    Quand "alice" transfère 150.00 du budget "Courses" vers le budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Ajustement           |
    Alors le fond du budget "Courses" vaut 350.00
    Et le reste du budget "Courses" vaut 150.00
    Et le fond du budget "Loisirs" vaut 250.00
    Et le reste du budget "Loisirs" vaut 225.00

  Scénario: 23 - Autoriser un transfert qui rend le fond source négatif
    Étant donné un budget "Courses" créé par "alice" avec un fond de 100.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 0.00
    Quand "alice" transfère 250.00 du budget "Courses" vers le budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Découvert exceptionnel |
    Alors la réponse a le statut 200
    Et le fond du budget "Courses" vaut -150.00
    Et le fond du budget "Loisirs" vaut 250.00

  Scénario: 24 - Refuser un transfert d'un montant négatif ou nul
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Quand "alice" transfère <montant> du budget "Courses" vers le budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Le montant du transfert doit être strictement positif"

    Exemples:
      | montant |
      | 0.00    |
      | -10.00  |

  Scénario: 25 - Refuser un transfert vers le même budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" transfère 50.00 du budget "Courses" vers le budget "Courses" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Le budget source et le budget cible doivent être différents"

  Scénario: 26 - Refuser un transfert depuis un budget dont l'utilisateur n'est pas éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Quand "alice" transfère 100.00 du budget "Voyages" vers le budget "Loisirs" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Tentative            |
    Alors la réponse a le statut 403

  Scénario: 27 - Refuser un transfert vers un budget dont l'utilisateur n'est pas éditeur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" transfère 100.00 du budget "Courses" vers le budget "Voyages" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Tentative            |
    Alors la réponse a le statut 403

  Scénario: 28 - Refuser un transfert si l'un des budgets n'existe pas
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" transfère 50.00 du budget "Courses" vers le budget "00000000-0000-0000-0000-000000000000" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 404

  Scénario: 29 - Le transfert est atomique - aucun budget n'est modifié si l'opération échoue
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" transfère 50.00 du budget "Courses" vers le budget "00000000-0000-0000-0000-000000000000" avec:
      | date   | 2026-04-29T10:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 404
    Et le fond du budget "Courses" vaut 500.00

  Scénario: 29bis - Refuser un transfert sans date ou sans raison
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Quand "alice" transfère 50.00 du budget "Courses" vers le budget "Loisirs" avec:
      | date   | <date>   |
      | raison | <raison> |
    Alors la réponse a le statut 400
    Et le message d'erreur mentionne le champ "<champ>"

    Exemples:
      | date                 | raison      | champ  |
      |                      | Réallocation| date   |
      | 2026-04-29T10:00:00Z |             | raison |

  # ---------------------------------------------------------------------------
  # Crédit depuis une source externe (source inconnue) - réservé aux éditeurs
  # Tout crédit exige une date de crédit et une raison.
  # ---------------------------------------------------------------------------
  Scénario: 30 - Créditer un budget depuis une source externe inconnue avec date et raison
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" crédite le budget "Courses" de 200.00 depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Remboursement assurance |
    Alors la réponse a le statut 200
    Et le fond du budget "Courses" vaut 700.00
    Et un mouvement de crédit de 200.00 est enregistré sur le budget "Courses" avec la source "INCONNUE", la date "2026-04-15T09:00:00Z" et la raison "Remboursement assurance"

  Scénario: 31 - Le crédit externe augmente le reste du budget sans toucher aux achats
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00 et 200.00 d'achats
    Quand "alice" crédite le budget "Courses" de 100.00 depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Bonus                |
    Alors le fond du budget "Courses" vaut 600.00
    Et le total des achats du budget vaut 200.00
    Et le reste du budget "Courses" vaut 400.00

  Scénario: 32 - Créditer un budget avec un fond négatif depuis une source externe
    Étant donné un budget "Avance salaire" créé par "alice" avec un fond de -200.00
    Quand "alice" crédite le budget "Avance salaire" de 300.00 depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Versement employeur  |
    Alors la réponse a le statut 200
    Et le fond du budget "Avance salaire" vaut 100.00

  Scénario: 33 - Refuser un crédit externe d'un montant négatif ou nul
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" crédite le budget "Courses" de <montant> depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Le montant du crédit doit être strictement positif"

    Exemples:
      | montant |
      | 0.00    |
      | -50.00  |

  Scénario: 34 - Refuser un crédit externe par un non-éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" crédite le budget "Voyages" de 100.00 depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Tentative            |
    Alors la réponse a le statut 403

  Scénario: 35 - Refuser un crédit externe sur un budget inexistant
    Quand "alice" crédite un budget avec l'identifiant "00000000-0000-0000-0000-000000000000" de 100.00 depuis une source externe avec:
      | date   | 2026-04-15T09:00:00Z |
      | raison | Test                 |
    Alors la réponse a le statut 404

  Scénario: 36 - Tracer l'historique des crédits externes d'un budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a crédité le budget "Courses" de 100.00 depuis une source externe le "2026-04-10T09:00:00Z" avec la raison "Cadeau"
    Et "alice" a crédité le budget "Courses" de 50.00 depuis une source externe le "2026-04-20T09:00:00Z" avec la raison "Remboursement"
    Quand "alice" consulte l'historique des mouvements du budget "Courses"
    Alors la réponse a le statut 200
    Et l'historique contient 2 mouvements de crédit avec la source "INCONNUE"
    Et la somme des crédits externes vaut 150.00
    Et chaque mouvement de crédit a une date et une raison renseignées

  Scénario: 36bis - Refuser un crédit externe sans date ou sans raison
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" crédite le budget "Courses" de 100.00 depuis une source externe avec:
      | date   | <date>   |
      | raison | <raison> |
    Alors la réponse a le statut 400
    Et le message d'erreur mentionne le champ "<champ>"

    Exemples:
      | date                 | raison       | champ  |
      |                      | Remboursement| date   |
      | 2026-04-15T09:00:00Z |              | raison |

  # ---------------------------------------------------------------------------
  # Suppression d'un budget (réservée aux éditeurs)
  # ---------------------------------------------------------------------------
  Scénario: 37 - Supprimer un budget sans achat associé
    Étant donné un budget "Loisirs" créé par "alice" avec un fond de 100.00
    Quand "alice" supprime le budget "Loisirs"
    Alors la réponse a le statut 204
    Et le budget "Loisirs" n'apparaît plus dans la liste des budgets

  Scénario: 38 - Supprimer un budget ayant des achats associés détache ces achats sans les supprimer
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un achat de 120.00 par "alice" associé au budget "Courses"
    Quand "alice" supprime le budget "Courses"
    Alors la réponse a le statut 204
    Et l'achat de 120.00 existe toujours
    Et l'achat de 120.00 a toujours pour auteur "alice"
    Et l'achat de 120.00 n'est associé à aucun budget

  # ---------------------------------------------------------------------------
  # Restriction de modification aux seuls éditeurs
  # ---------------------------------------------------------------------------
  Scénario: 39 - Refuser la mise à jour d'un budget par un non-éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" met à jour le budget "Voyages" avec:
      | nom | Vacances |
    Alors la réponse a le statut 403
    Et le nom du budget "Voyages" vaut "Voyages"

  Scénario: 40 - Refuser la suppression d'un budget par un non-éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" supprime le budget "Voyages"
    Alors la réponse a le statut 403
    Et le budget "Voyages" existe toujours

  Scénario: 41 - Consulter l'historique des mouvements d'un budget créé par un autre est autorisé
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Et "bob" a crédité le budget "Voyages" de 200.00 depuis une source externe
    Quand "alice" consulte l'historique des mouvements du budget "Voyages"
    Alors la réponse a le statut 200
    Et l'historique contient 1 mouvement de crédit avec la source "INCONNUE"

  # ---------------------------------------------------------------------------
  # Gestion de la liste des éditeurs (réservée au créateur)
  # ---------------------------------------------------------------------------
  Scénario: 42 - À la création, la liste des éditeurs ne contient que le créateur
    Quand "alice" crée un budget avec:
      | nom  | Courses |
      | fond | 500.00  |
    Alors la liste des éditeurs du budget contient uniquement "alice"

  Scénario: 43 - Le créateur ajoute un utilisateur à la liste des éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur "bob" existe
    Quand "alice" ajoute "bob" à la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 200
    Et la liste des éditeurs du budget "Courses" contient "alice" et "bob"

  Scénario: 44 - Un éditeur ajouté peut modifier le budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "bob" met à jour le budget "Courses" avec:
      | description | Géré conjointement avec bob |
    Alors la réponse a le statut 200
    Et la description du budget "Courses" vaut "Géré conjointement avec bob"

  Scénario: 45 - Un éditeur ajouté peut associer son achat au budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec les permissions "budget:write" et "purchase:create"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "bob" crée un achat de 75.00 associé au budget "Courses"
    Alors la réponse a le statut 201
    Et l'achat a pour auteur "bob"
    Et l'achat est associé au budget "Courses"
    Et le reste du budget "Courses" vaut 425.00

  Scénario: 46 - Le créateur retire un utilisateur de la liste des éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "alice" retire "bob" de la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 200
    Et la liste des éditeurs du budget "Courses" contient uniquement "alice"

  Scénario: 47 - Un utilisateur retiré ne peut plus modifier le budget
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Et "alice" a retiré "bob" de la liste des éditeurs du budget "Courses"
    Quand "bob" met à jour le budget "Courses" avec:
      | description | Tentative de bob |
    Alors la réponse a le statut 403
    Et la description du budget "Courses" n'est pas "Tentative de bob"

  Scénario: 48 - Les achats créés par un utilisateur retiré conservent leur auteur et leur association
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec les permissions "budget:write" et "purchase:create"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Et "bob" a créé un achat de 75.00 associé au budget "Courses"
    Quand "alice" retire "bob" de la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 200
    Et l'achat de 75.00 a toujours pour auteur "bob"
    Et l'achat de 75.00 est toujours associé au budget "Courses"
    Et le reste du budget "Courses" vaut 425.00

  Scénario: 49 - Refuser l'ajout d'un éditeur par un utilisateur qui n'est pas le créateur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et un utilisateur "carol" existe
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "bob" ajoute "carol" à la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 403
    Et la liste des éditeurs du budget "Courses" contient uniquement "alice" et "bob"

  Scénario: 50 - Refuser le retrait d'un éditeur par un utilisateur qui n'est pas le créateur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "bob" retire "alice" de la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 403
    Et la liste des éditeurs du budget "Courses" contient toujours "alice"

  Scénario: 51 - Refuser que le créateur se retire lui-même de la liste des éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" retire "alice" de la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 400
    Et le message d'erreur indique "Le créateur ne peut pas être retiré de la liste des éditeurs"

  Scénario: 52 - Refuser l'ajout d'un utilisateur déjà éditeur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Quand "alice" ajoute "bob" à la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 409
    Et le message d'erreur indique "L'utilisateur est déjà éditeur de ce budget"

  Scénario: 53 - Refuser le retrait d'un utilisateur qui n'est pas éditeur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur "carol" existe
    Quand "alice" retire "carol" de la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 404

  Scénario: 54 - Refuser l'ajout d'un utilisateur inexistant à la liste des éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" ajoute un utilisateur inexistant à la liste des éditeurs du budget "Courses"
    Alors la réponse a le statut 404

  Scénario: 55 - Lister les éditeurs d'un budget est ouvert à tous les utilisateurs authentifiés
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Et un utilisateur authentifié "carol" avec la permission "budget:read"
    Quand "carol" liste les éditeurs du budget "Courses"
    Alors la réponse a le statut 200
    Et la liste contient exactement 2 éditeurs: "alice" et "bob"

  # ---------------------------------------------------------------------------
  # Historique pour visualisation graphique de la fluctuation du reste
  #
  # La timeline d'un budget est la suite chronologique des évènements affectant
  # son reste : création (fond initial), crédits externes, transferts entrants
  # et sortants, achats associés (par leur purchaseDate), assignations et
  # désassignations d'achats. Chaque évènement porte une date et expose le
  # reste cumulé après application — ce qui suffit à tracer une courbe.
  # ---------------------------------------------------------------------------
  Scénario: 56 - La création d'un budget enregistre le fond initial comme premier point de la timeline
    Quand "alice" crée un budget avec:
      | nom  | Courses |
      | fond | 500.00  |
    Alors la timeline du budget "Courses" contient un évènement initial de type "CREATION"
    Et cet évènement initial a une date égale à la date de création du budget
    Et cet évènement initial a un montant de 500.00
    Et le reste cumulé après cet évènement vaut 500.00

  Scénario: 57 - La timeline d'un budget agrège tous les évènements affectant son reste
    Étant donné un budget "Courses" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 200.00
    Et "alice" a crédité le budget "Courses" de 100.00 depuis une source externe le "2026-04-05T09:00:00Z" avec la raison "Cadeau"
    Et un achat de 80.00 par "alice" associé au budget "Courses" daté du "2026-04-10T12:00:00Z"
    Et "alice" a transféré 50.00 du budget "Courses" vers le budget "Loisirs" le "2026-04-15T10:00:00Z" avec la raison "Réajustement"
    Et un achat de 30.00 par "alice" associé au budget "Courses" daté du "2026-04-20T12:00:00Z"
    Quand "alice" consulte la timeline du budget "Courses"
    Alors la réponse a le statut 200
    Et la timeline contient 5 évènements triés par date croissante:
      | date                 | type             | montant | reste_cumule |
      | 2026-04-01T08:00:00Z | CREATION         |  500.00 |       500.00 |
      | 2026-04-05T09:00:00Z | CREDIT_EXTERNE   |  100.00 |       600.00 |
      | 2026-04-10T12:00:00Z | ACHAT            |  -80.00 |       520.00 |
      | 2026-04-15T10:00:00Z | TRANSFERT_SORTANT|  -50.00 |       470.00 |
      | 2026-04-20T12:00:00Z | ACHAT            |  -30.00 |       440.00 |

  Scénario: 58 - Filtrer la timeline d'un budget sur une période [start, end]
    Étant donné un budget "Courses" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 500.00
    Et "alice" a crédité le budget "Courses" de 100.00 depuis une source externe le "2026-04-05T09:00:00Z" avec la raison "Cadeau"
    Et un achat de 80.00 par "alice" associé au budget "Courses" daté du "2026-04-10T12:00:00Z"
    Et un achat de 30.00 par "alice" associé au budget "Courses" daté du "2026-04-20T12:00:00Z"
    Et un achat de 40.00 par "alice" associé au budget "Courses" daté du "2026-05-02T12:00:00Z"
    Quand "alice" consulte la timeline du budget "Courses" entre "2026-04-08T00:00:00Z" et "2026-04-30T23:59:59Z"
    Alors la réponse a le statut 200
    Et la timeline filtrée contient 2 évènements:
      | date                 | type  | montant | reste_cumule |
      | 2026-04-10T12:00:00Z | ACHAT |  -80.00 |       520.00 |
      | 2026-04-20T12:00:00Z | ACHAT |  -30.00 |       490.00 |
    Et le reste de référence à "2026-04-08T00:00:00Z" vaut 600.00
    Et le reste de référence à "2026-04-30T23:59:59Z" vaut 490.00

  Scénario: 59 - Une réassignation d'achat produit deux évènements datés sur les budgets concernés
    Étant donné un budget "Courses" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 200.00
    Et un achat de 80.00 par "alice" associé au budget "Courses" daté du "2026-04-10T12:00:00Z"
    Et "alice" a réassigné cet achat au budget "Loisirs" le "2026-04-25T15:00:00Z" avec la raison "Mauvaise affectation"
    Quand "alice" consulte la timeline du budget "Courses"
    Alors la timeline du budget "Courses" contient les évènements:
      | date                 | type                | montant | reste_cumule |
      | 2026-04-01T08:00:00Z | CREATION            |  500.00 |       500.00 |
      | 2026-04-10T12:00:00Z | ACHAT               |  -80.00 |       420.00 |
      | 2026-04-25T15:00:00Z | DESASSIGNATION_ACHAT|   80.00 |       500.00 |
    Quand "alice" consulte la timeline du budget "Loisirs"
    Alors la timeline du budget "Loisirs" contient les évènements:
      | date                 | type              | montant | reste_cumule |
      | 2026-04-01T08:00:00Z | CREATION          |  200.00 |       200.00 |
      | 2026-04-25T15:00:00Z | ASSIGNATION_ACHAT |  -80.00 |       120.00 |

  Scénario: 60 - Le transfert produit des évènements miroirs sur les deux budgets
    Étant donné un budget "Courses" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" le "2026-04-01T08:00:00Z" avec un fond de 100.00
    Quand "alice" transfère 150.00 du budget "Courses" vers le budget "Loisirs" avec:
      | date   | 2026-04-15T10:00:00Z |
      | raison | Réallocation         |
    Alors la timeline du budget "Courses" contient un évènement de type "TRANSFERT_SORTANT" daté du "2026-04-15T10:00:00Z" d'un montant de -150.00
    Et la timeline du budget "Loisirs" contient un évènement de type "TRANSFERT_ENTRANT" daté du "2026-04-15T10:00:00Z" d'un montant de 150.00
    Et les deux évènements partagent le même identifiant de transfert et la raison "Réallocation"

  Scénario: 61 - La timeline est ouverte en lecture à tous les utilisateurs authentifiés
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Et "bob" a crédité le budget "Voyages" de 200.00 depuis une source externe le "2026-04-15T09:00:00Z" avec la raison "Bonus"
    Quand "alice" consulte la timeline du budget "Voyages"
    Alors la réponse a le statut 200
    Et la timeline contient 2 évènements de types "CREATION" et "CREDIT_EXTERNE"

  # ---------------------------------------------------------------------------
  # Budget par défaut d'un utilisateur
  #
  # Chaque utilisateur peut désigner un budget par défaut parmi ceux dont il
  # est éditeur. Ce budget sera pré-sélectionné lors de la création d'un achat
  # si aucun budget n'est explicitement précisé. La préférence est invalidée
  # automatiquement si l'utilisateur n'est plus éditeur du budget choisi.
  # ---------------------------------------------------------------------------
  Scénario: 62 - Par défaut, un utilisateur n'a pas de budget par défaut
    Quand "alice" consulte son budget par défaut
    Alors la réponse a le statut 200
    Et le budget par défaut de "alice" est vide

  Scénario: 63 - Définir un budget par défaut parmi ceux dont on est éditeur
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Quand "alice" définit "Courses" comme budget par défaut
    Alors la réponse a le statut 200
    Et le budget par défaut de "alice" est "Courses"

  Scénario: 64 - Le budget par défaut est utilisé lorsqu'un achat est créé sans budget précisé
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" crée un achat de 75.00 sans préciser de budget
    Alors la réponse a le statut 201
    Et l'achat est associé au budget "Courses"
    Et le reste du budget "Courses" vaut 425.00

  Scénario: 65 - Préciser un autre budget à la création d'un achat surcharge le budget par défaut
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 200.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" crée un achat de 30.00 associé au budget "Loisirs"
    Alors l'achat est associé au budget "Loisirs"
    Et le reste du budget "Courses" vaut 500.00
    Et le reste du budget "Loisirs" vaut 170.00

  Scénario: 66 - Préciser explicitement aucun budget surcharge le budget par défaut
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" crée un achat de 40.00 explicitement sans budget associé
    Alors l'achat n'est associé à aucun budget
    Et le reste du budget "Courses" vaut 500.00

  Scénario: 67 - Changer le budget par défaut
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Loisirs" créé par "alice" avec un fond de 200.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" définit "Loisirs" comme budget par défaut
    Alors la réponse a le statut 200
    Et le budget par défaut de "alice" est "Loisirs"

  Scénario: 68 - Retirer son budget par défaut
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" retire son budget par défaut
    Alors la réponse a le statut 200
    Et le budget par défaut de "alice" est vide

  Scénario: 69 - Refuser de définir comme par défaut un budget dont on n'est pas éditeur
    Étant donné un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Quand "alice" définit "Voyages" comme budget par défaut
    Alors la réponse a le statut 403
    Et le message d'erreur indique "Le budget par défaut doit être un budget dont vous êtes éditeur"
    Et le budget par défaut de "alice" est vide

  Scénario: 70 - Refuser de définir comme par défaut un budget inexistant
    Quand "alice" définit le budget "00000000-0000-0000-0000-000000000000" comme budget par défaut
    Alors la réponse a le statut 404

  Scénario: 71 - Le budget par défaut est invalidé si l'utilisateur est retiré de la liste des éditeurs
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Et "bob" a défini "Courses" comme budget par défaut
    Quand "alice" retire "bob" de la liste des éditeurs du budget "Courses"
    Alors le budget par défaut de "bob" est vide

  Scénario: 72 - Le budget par défaut est invalidé si le budget est supprimé
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a défini "Courses" comme budget par défaut
    Quand "alice" supprime le budget "Courses"
    Alors le budget par défaut de "alice" est vide

  Scénario: 73 - Le budget par défaut est invalidé si le créateur retire l'éditeur via une autre voie (cohérence)
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et "alice" a défini "Courses" comme budget par défaut
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a ajouté "bob" à la liste des éditeurs du budget "Courses"
    Et "bob" a défini "Courses" comme budget par défaut
    Quand "alice" retire "bob" de la liste des éditeurs du budget "Courses"
    Alors le budget par défaut de "alice" est toujours "Courses"
    Et le budget par défaut de "bob" est vide

  Scénario: 74 - Le budget par défaut de chaque utilisateur est isolé
    Étant donné un budget "Courses" créé par "alice" avec un fond de 500.00
    Et un budget "Voyages" créé par "bob" avec un fond de 1000.00
    Et un utilisateur authentifié "bob" avec la permission "budget:write"
    Et "alice" a défini "Courses" comme budget par défaut
    Et "bob" a défini "Voyages" comme budget par défaut
    Quand "alice" consulte son budget par défaut
    Alors le budget par défaut de "alice" est "Courses"
    Quand "bob" consulte son budget par défaut
    Alors le budget par défaut de "bob" est "Voyages"
