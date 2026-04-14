#!/bin/bash

# Read the choice sent by the POST (fetch)
read playerChoice

# Normalize player choice
case "$playerChoice" in
    rock|Rock|ROCK|r|R|🗿) player="rock" ;;
    paper|Paper|PAPER|p|P|📄) player="paper" ;;
    scissors|Scissors|SCISSORS|s|S|✂️|✂) player="scissors" ;;
    *) player="invalid" ;;
esac

# Random computer choice
options=("rock" "paper" "scissors")
computer=${options[$RANDOM % 3]}

# Determine result
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

# Return JSON
echo "{\"result\":\"$result\", \"computerChoice\":\"$computer\"}"
