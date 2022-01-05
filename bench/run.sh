##
# run a bench on one server

. conf.sh

srv="$1"
URL="$2"

dotests() {
    for t in "${TESTS[@]}"
    do
        date +"%Y-%m-%d %H:%M run test $t"
        siege -R siegerc --quiet --log="out/$2" -t$1 "$URL/$t" >/dev/null 2>/dev/null
    done
}

echo "warmup $srv"
dotests 10s "/dev/null"
rm -f out/$srv
echo "testing $srv"
dotests 60s "$srv"
