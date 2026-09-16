#!/usr/bin/env bash
#
# Builds the library and the demo bundle and deploys them into the running demo environment.
#
# The portal has to be up before this runs. A setup file touches data owned by modules that are
# registered late during startup (expando permission checks, the data engine behind web content
# structures, search indexers), so a setup that runs while the portal boots fails on them.
# Deploying into a started portal avoids that entirely.
#
# The library is also copied into ./liferay/files/osgi/modules, which the official Liferay image
# copies into the container on every start. The demo bundle is deliberately not copied there:
# it would be installed during startup and its setup would run too early.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "${SCRIPT_DIR}")"
EXAMPLE_DIR="${PROJECT_DIR}/com.ableneo.liferay.site.example"
MODULES_DIR="${SCRIPT_DIR}/liferay/files/osgi/modules"
DEPLOY_DIR="${SCRIPT_DIR}/liferay/deploy"
CONTAINER="lfr-demo-liferay"

if [ -z "$(docker ps -q -f name="^${CONTAINER}$" 2>/dev/null)" ]; then
    echo "${CONTAINER} is not running. Start it first and wait for the portal to come up:" >&2
    echo "    docker compose up -d" >&2
    echo "    docker compose logs -f liferay   # wait for 'Server startup in'" >&2
    exit 1
fi

echo "==> Building and installing the library"
(cd "${PROJECT_DIR}" && mvn -B -q clean install -DskipTests)

echo "==> Building the demo bundle"
(cd "${EXAMPLE_DIR}" && mvn -B -q clean package)

find_jar() {
    find "$1/target" -maxdepth 1 -name 'com.ableneo.*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name 'original-*.jar' | head -1
}

LIBRARY_JAR="$(find_jar "${PROJECT_DIR}")"
DEMO_JAR="$(find_jar "${EXAMPLE_DIR}")"

# The library on its own runs no setup, so it is safe to have it installed at startup.
mkdir -p "${MODULES_DIR}"
rm -f "${MODULES_DIR}"/com.ableneo.*.jar
cp "${LIBRARY_JAR}" "${MODULES_DIR}/"

# ./liferay/deploy is bind mounted as the container deploy folder. Liferay removes the jars from
# it once they are installed, so seeing it empty afterwards is expected.
mkdir -p "${DEPLOY_DIR}"
cp "${LIBRARY_JAR}" "${DEMO_JAR}" "${DEPLOY_DIR}/"

echo "==> Deployed:"
echo "    $(basename "${LIBRARY_JAR}")"
echo "    $(basename "${DEMO_JAR}")"
echo "==> Watch the result with: docker compose logs -f liferay"
