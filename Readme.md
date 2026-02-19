1.TYPES DE COMMITS (CONVENTIONAL COMMITS)

feat : ajout d’une nouvelle fonctionnalité
fix : correction d’un bug
docs : modifications de documentation (README, wiki…)
style : changements sans impact sur le code (formatage, indentation…)
refactor : réécriture interne sans changer le comportement
perf : amélioration des performances
test : ajout ou modification de tests
build : changements liés au build ou dépendances
ci : changements liés à l’intégration continue
chore : tâches diverses (nettoyage, scripts…)
Format recommandé : <type>(scope): description courte
Exemples : feat(api): ajoute la route GET /users
fix(ui): corrige l’affichage du bouton

2.COMMANDES GIT ESSENTIELLES

- Initialisation et configuration git init
git clone <url>
git config --global user.name "Nom"
git config --global user.email "Email"
- Gestion des fichiers git status
git add <fichier>
git add .
git restore <fichier>
git restore --staged <fichier>
- Commits git commit -m "message"
git commit
git commit --amend
- Branches git branch
git branch  nom
git checkout <nom>
git switch <nom>
git switch -c <nom>
- Fusion et rebase git merge <branche>
git rebase <branche>
git merge --abort
git rebase --abort
- Dépôt distant git remote -v
git remote add origin <url>
git push -u origin <branche>
git push
git pull
git fetch
- Historique et inspection git log
git log --oneline --graph
git diff
git diff --staged
- Annulation et récupération git reset --soft HEAD1
git reset --hard HEAD~1
git revert <hash>

3.WORKFLOW RECOMMANDE POUR L’EQUIPE

- Créer une branche pour chaque fonctionnalité
git switch -c feat/nom-fonction
- Développer et faire des commits propres
git add .
git commit -m "feat(api): ajoute la route /users"
- Mettre à jour depuis main
git pull origin main
- Résoudre les conflits si nécessaire
- Envoyer la branche
git push -u origin feat/nom-fonction
- Créer une Pull Request
 

 pour creation du bash :

 - faire du parsing
 - getopt sert à analyser les options passées à un script et à les transformer en quelque chose de propre et cohérent.
   (À utiliser si)