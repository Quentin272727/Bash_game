#!/bin/bash

# Lire le choix envoyé par le POST (fetch)
read playerChoice

# Normaliser le choix du joueur
case "$playerChoice" in
    rock|Rock|ROCK|r|R|🗿) player="rock" ;;
    paper|Paper|PAPER|p|P|📄) player="paper" ;;
    scissors|Scissors|SCISSORS|s|S|✂️|✂) player="scissors" ;;
    *) player="invalid" ;;
esac

# Choix aléatoire de l'ordinateur
options=("rock" "paper" "scissors")
computer=${options[$RANDOM % 3]}

# Déterminer le résultat
if [[ "$player" == "invalid" ]]; then
    result="invalid"
elif [[ "$player" == "$computer" ]]; then
    result="tie"
elif [[ "$player" == "rock" && "$computer" == "scissors" ]] ||
     [[ "$player" == "paper" && "$computer" == "rock" ]] ||
     [[ "$player" == "scissors" && "$computer" == "paper" ]]; then
    result="win"
else
    result="lose"
fi

# Retour JSON
echo "{\"result\":\"$result\", \"computerChoice\":\"$computer\"}"