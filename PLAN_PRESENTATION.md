# Plan de Présentation — Jeu Bash : Pierre Papier Ciseaux

---

## Informations générales

| Élément | Détail |
|---------|--------|
| **Titre** | Pierre Papier Ciseaux — Un jeu web piloté par Bash |
| **Durée estimée** | 10 à 15 minutes |
| **Public cible** | Étudiants en informatique, développeurs débutants, jury pédagogique |
| **Supports suggérés** | Slides de présentation + démonstration live dans le navigateur |

---

## Objectif de la présentation

Expliquer comment un jeu de navigateur classique peut être construit **sans framework**, en combinant trois technologies complémentaires : un script Bash, un serveur HTTP Java, et une interface HTML/CSS/JS — et en montrant comment ces couches communiquent entre elles.

---

## Structure de la présentation

### Slide 1 — Accroche & Titre

**Titre :** *Pierre Papier Ciseaux — piloté par Bash*

**Message d'accroche :**
> "Et si on faisait tourner un jeu web... avec un script shell ?"

**But :** Susciter la curiosité en annonçant une architecture inhabituelle.

---

### Slide 2 — Contexte & Motivation

**Points à aborder :**
- La plupart des jeux web reposent sur des frameworks lourds (Node.js, Spring, Django...).
- Ce projet prend le contrepied : tout est fait "à la main", avec les outils fondamentaux.
- Objectif pédagogique : comprendre comment fonctionne réellement un serveur HTTP, et comment des processus communiquent entre eux.

**Question rhétorique à poser au public :**
> "Qu'est-ce qui se passe réellement entre le moment où vous cliquez sur un bouton et le moment où le résultat s'affiche ?"

---

### Slide 3 — Présentation de l'architecture globale

**Schéma à afficher :**

```
Navigateur  →  (HTTP POST)  →  Serveur Java  →  (stdin/stdout)  →  Script Bash
    ↑                               ↓
    └────────── (JSON) ─────────────┘
```

**Points à expliquer :**
- Le navigateur envoie le choix du joueur via une requête HTTP POST.
- Le serveur Java reçoit la requête et lance le script Bash en sous-processus.
- Le script Bash traite la logique du jeu et renvoie un JSON en sortie standard.
- Le serveur Java lit ce JSON et le renvoie au navigateur comme réponse HTTP.

**Mot clé à introduire ici :** IPC (Inter-Process Communication).

---

### Slide 4 — Couche 1 : Le script Bash (`Play.sh`)

**Rôle :** Moteur du jeu. Toute la logique métier y est concentrée.

**Points à couvrir :**
- Lit le choix du joueur depuis `stdin` (une seule ligne).
- Normalise l'entrée : `rock`, `Rock`, `ROCK`, `r`, `🗿` → tous équivalents.
- Génère aléatoirement le choix de l'ordinateur via `$RANDOM`.
- Applique les règles du jeu (3 conditions de victoire explicites).
- Renvoie un objet JSON sur `stdout`.

**Exemple de sortie à montrer :**
```json
{"result":"win", "computerChoice":"scissors"}
```

**Point fort à souligner :** La séparation complète entre la logique et l'interface — le script ne "sait" pas qu'il tourne dans un contexte web.

---

### Slide 5 — Couche 2 : Le serveur Java (`Server.java`)

**Rôle :** Pont entre le navigateur et le script Bash.

**Points à couvrir :**
- Écrit en Java pur, sans bibliothèque externe : `ServerSocket` brut sur le port 8080.
- Gère jusqu'à 10 connexions simultanées grâce à un `ExecutorService` (pool de threads).
- Trois routes définies manuellement :
  - `GET /` → sert `index.html`
  - `GET /style.css` → sert la feuille de style
  - `POST /play` → lance `Play.sh` et retourne le JSON
- Compatible Windows (via `bash script.sh`) et Linux/macOS (invocation directe).

**Point technique à expliquer simplement :**
> "Pour éviter un deadlock, le serveur lit d'abord la sortie du script, puis attend sa fin — et non l'inverse."

---

### Slide 6 — Couche 3 : L'interface web (`index.html` + `style.css`)

**Rôle :** Ce que l'utilisateur voit et manipule.

**Points à couvrir :**
- Interface minimaliste : une bannière, trois boutons emoji, une zone de résultat.
- Utilise la **Fetch API** (JavaScript moderne) pour envoyer la requête au serveur sans recharger la page.
- Affichage immédiat de l'icône joueur + icône de chargement pendant l'attente.
- Gestion des erreurs côté client (serveur inaccessible, JSON invalide).

**Extrait de code à montrer :**
```javascript
const response = await fetch('/play', {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain; charset=UTF-8' },
    body: playerChoice
});
const data = await response.json();
```

---

### Slide 7 — Défi technique : UTF-8 et les emoji

**Pourquoi c'est un vrai problème :**
- Les emoji sont des caractères Unicode encodés sur **4 octets** en UTF-8.
- Sans encodage explicite, certains systèmes utilisent ISO-8859-1 par défaut, ce qui corrompt les données.

**Solution appliquée :**
- Encodage UTF-8 forcé à **chaque frontière** : header HTTP, lecture du socket, écriture vers Bash, lecture depuis Bash, réponse HTTP.

**Message à retenir :**
> "L'encodage n'est pas un détail — c'est une frontière à franchir proprement à chaque étape."

---

### Slide 8 — Démonstration live

**Scénario de démo (3 à 5 minutes) :**

1. Lancer le serveur depuis le terminal avec `bash Script_Bash/Launcher_Serv.sh`.
2. Ouvrir `http://localhost:8080` dans le navigateur.
3. Jouer une partie en cliquant sur les boutons emoji.
4. Ouvrir les outils développeur du navigateur (onglet Réseau) pour montrer la requête POST et la réponse JSON en direct.
5. Optionnel : montrer le terminal Java qui affiche les logs de traitement.

**Phrase d'introduction à la démo :**
> "Voyons maintenant comment tout ça s'assemble en conditions réelles."

---

### Slide 9 — Points forts du projet

| Force | Explication |
|-------|-------------|
| Architecture claire | Chaque couche a un rôle unique et séparé |
| Aucune dépendance externe | Pas de framework, pas de librairie tierce |
| Compatible multiplateforme | Windows (WSL), Linux, macOS |
| Code lisible et commenté | Facile à reprendre ou modifier |
| Gestion des cas d'erreur | Erreurs réseau, JSON invalide, choix invalide — tous couverts |

---

### Slide 10 — Limites & Pistes d'amélioration

**Limites actuelles :**
- Pas de HTTPS (uniquement localhost).
- Pas de persistance du score (réinitialisé au rechargement).
- Pas de validation des méthodes HTTP (pas de réponse 405).
- Dépend de Bash (ne fonctionne pas sous Windows sans WSL).

**Améliorations possibles :**
- Ajouter un système de score côté serveur (fichier ou base de données légère).
- Implémenter le support HTTPS avec des certificats auto-signés.
- Ajouter un mode multijoueur (deux joueurs sur le réseau local).
- Remplacer le script Bash par un script Python pour plus de portabilité.

---

### Slide 11 — Conclusion

**Message clé :**
> "Ce projet montre qu'on peut construire une application web fonctionnelle en comprenant et maîtrisant chaque brique — sans s'appuyer sur des abstractions toutes faites."

**Ce qu'on a appris :**
- Le fonctionnement interne d'un serveur HTTP.
- La communication inter-processus (IPC) via stdin/stdout.
- L'importance de l'encodage des caractères dans un système distribué.
- La conception d'une architecture en couches proprement séparées.

**Mot de fin :**
> "Questions ?"

---

## Conseils pour la présentation orale

- **Ne pas lire les slides :** Utiliser les points clés comme aide-mémoire, parler librement.
- **Préparer la démo :** Tester le lancement du serveur avant la présentation. Avoir une capture d'écran en backup en cas de problème technique.
- **Anticiper les questions fréquentes :**
  - *Pourquoi Java et pas Node.js ?* → Pour montrer le fonctionnement bas niveau sans l'abstraction d'un runtime JS.
  - *Pourquoi Bash pour la logique ?* → Pour démontrer l'intégration de scripts shell dans une architecture web.
  - *Est-ce utilisable en production ?* → Non, il manque HTTPS, authentification et persistance — mais ce n'est pas l'objectif.

---

## Répartition du temps suggérée

| Section | Durée |
|---------|-------|
| Introduction & accroche | 1 min |
| Architecture globale | 2 min |
| Les 3 couches (Bash, Java, HTML) | 4 min |
| Défis techniques (UTF-8) | 1 min |
| Démonstration live | 4 min |
| Points forts & limites | 1 min |
| Conclusion & questions | 2 min |
| **Total** | **~15 min** |
