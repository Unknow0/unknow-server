#!/bin/bash

. config.sh

print() {
	printf '%-10s' 'server'
	for i in "${TESTS[@]}"; do printf '%-10s' "${i%% *}"; done
	echo
	IFS=$'\n'
	for s in ${SERVER[@]}
	do
		printf '%-10s' "$s"
		for r in $(tail -n +2 $s | cut -d ',' -f $1); do printf '%-10s' "${r// }"; done
		echo
	done
}

echo
echo "Transaction per sec"
print 6

echo "Failled connection"
print 10
