#!/bin/sh

URL=http://127.0.0.1:8080

die() { echo $@; exit 1; }
http_code() { curl -s -w '%{http_code}' "$URL$1"; }
post() { curl -s -XPOST -d@".github/$1" $URL/ws -o out.xml -w '%{http_code}'; }

xml_parse()
	{
	local IFS=\>
	declare -A ns
	tns=
	read -d \< a
	TAG=
	xml_parse_ns()
		{
		local IFS=' '
		for i in ${1#* }
		do
			k=${i%%=*}
			v=${i#*=}
			[[ "$k" = "xmlns" ]] && tns="{${v:1:-1}}" && continue
			[[ "$k" =~ xmlns:.* ]] && ns[${k#*:}]="{${v:1:-1}}"
		done
		}
	xml_parse_tag()
		{
		TAG=${1%% *}
		local n=
		[[ "$TAG" =~ .*:.* ]] && n="${TAG%%:*}"
		[[ -z $n ]] && TAG="$tns${TAG#*:}" || TAG="${ns[$n]}${TAG#*:}"
		echo $TAG
		}
	xml_parse_attrs()
		{
		local IFS=' '
		for i in $2
		do
			[[ "$i" =~ xmlns(:.*)? ]] && continue
			k=${i%%=*}
			v=${i#*=}
			echo "+$1@$k=${v:1:-1}"
		done
		}
	while read -d \< e c
	do
		xml_parse_ns "$e"
		if [ "${e:0:1}" = "/" ]
		then
			echo "-$(xml_parse_tag "${e:1}")"
		else
			tag=$(xml_parse_tag "$e")
			echo "+$tag"
			r=".* .*"
			[[ "$e" =~ $r ]] && xml_parse_attrs "$tag" "${e#* }"
		fi
		[[ "$c" ]] && echo "$c"
	done
	}

java -jar unknow-http-test/target/server.jar &
pid=$!
trap "kill -9 $pid" EXIT
sleep 5

[ "$(http_code "/error")" = "500" ] || die 'get /error'
[ "$(http_code "/error?code=503")" = "503" ] || die 'get /error?code'
[ "$(http_code "/missing")" = "404" ] || die 'get /missing'

## validate webservice
[ "$(post broken)" = '500' ] || die 'Webservice broken'
xmllint --format out.xml | grep -q '<faultstring>' || die 'Webservice broken content'

[ "$(post xml/bare_req.xml)" = '200' ] || die 'webservice bare'
cat out.xml | xml_parse | diff - .github/xml/bare_res.xml || die 'webservice base'

[ "$(post xml/wrapped_req.xml)" = '200' ] || die 'webservice wrapped'
cat out.xml | xml_parse | tee out | diff - .github/xml/wrapped_res.xml || die 'webservice wrapped'

curl -s -XGET "$URL/ws?wsdl" | xmllint --format - >/dev/null || die 'webservice wsdl'
