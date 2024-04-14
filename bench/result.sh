#!/bin/bash

declare -A servers tests count start end error

parse()
	{
	s="${1##*/}"
	s="${s%.*}"
	servers[$s]=""
	
	while read c n
	do
		tests[$n]=""
		count[$s:$n]=$c	
	done < <(cut -d ',' -f 3 "$1" | sort | uniq -c)
	
	while read c n
	do
		error[$s:$n]=$c
	done < <(grep ',false$' "$1" | cut -d ',' -f 3 | sort | uniq -c)
	
	
	local IFS=,
	while read t a n e
	do
		start[$s:$n]=$t
	done < <(sort -t , -k 3,1n "$1" | sort -t , -k 3 -u)
		
	while read t a n e
	do
		l=end[$s:$n]
		e=$(($t+$a ))
		[[ -z $l || $l -lt $n ]] && end[$s:$n]=$e
	done < <(sort -t , -r -k 3,1n "$1")
	}

for i in $1/*.csv; do parse "$i"; done

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
	for n in "${!tests[@]}"; do printf ' %10s' "${error[$s:$n]:-0}"; done
	echo
done
