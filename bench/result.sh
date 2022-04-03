#!/bin/bash

declare -A servers tests count start end error

parse()
	{
	s="${1#*/}"
	s="${s%.*}"
	servers[$s]=""
	local IFS=','
	while read -ra l
	do
		n="${l[2]}"
		[[ $n = "warmup" ]] && continue
		tests[$n]=""

		k="$s:$n"
		((count[$k]++))
		[[ -z ${error[$k]} ]] && error[$k]="0"
		[[ "${l[3]}" = "true" ]] || ((error[$k]++))
		
		t=${l[0]}
		[[ "${start[$k]}" -gt "$t" ]] && start[$k]=$t
		[[ -z "${start[$k]}" ]] && start[$k]=$t

		((t+=${l[1]}))
		[[ "${end[$k]}" -lt "$t" ]] && end[$k]=$t
		[[ -z "${end[$k]}" ]] && end[$k]=$t
	done < "$1"
	}

for i in $1/*; do parse "$i"; done

echo
echo 'throughput:'
printf '%10s' ''; for n in "${!tests[@]}"; do printf ' %10s' "$n"; done; echo

for s in "${!servers[@]}"
do
	printf '%10s' "$s"
	for n in "${!tests[@]}"
	do
		k="$s:$n"
		t=$((end[$k]-start[$k]))
		printf ' %10s' "$((count[$k]*1000/t))"
	done
	echo
done

echo
echo 'errors:'
printf '%10s' ''; for n in "${!tests[@]}"; do printf ' %10s' "$n"; done; echo

for s in "${!servers[@]}"
do
	printf '%10s' "$s"
	for n in "${!tests[@]}"; do printf ' %10s' "${error[$s:$n]}"; done
	echo
done
