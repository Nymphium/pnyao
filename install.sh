#!/usr/bin/bash -eu

product_version=$(awk '$1 == "version" {gsub(/"/, "", $3); print $3}' build.sbt)
scala_version=$(awk '$1 == "scalaVersion" {gsub(/"/, "", $3); gsub(/\.[0-9]+$/, "", $3); print $3}' build.sbt)
target_jar="pnyao-assembly-${product_version}.jar"

if ! sbt compile assembly; then
	return
fi

mkdir -p ~/.config/systemd/{pnyao,script,user}
cp "target/scala-${scala_version}/${target_jar}" ~/.config/systemd/pnyao/

## generate run script {{{
runscript="pnyao.sh"

cat <<-SHELL > "${runscript}"
#!/usr/bin/bash

java -Dplay.http.secret.key=none -jar ~/.config/systemd/pnyao/${target_jar}
SHELL

chmod 755 "${runscript}"
mv "${runscript}" ~/.config/systemd/script/
## }}}

## generate Systemd file {{{
service="pnyao-server.service"

cat <<-INI > "${service}"
[Unit]
Description=Pnyao PDF Management system Server

[Service]
Type=simple
ExecStart=/usr/bin/bash ${HOME}/.config/systemd/script/${runscript}

[Install]
WantedBy=multi-user.target
INI

mv "${service}" ~/.config/systemd/user/
## }}}

