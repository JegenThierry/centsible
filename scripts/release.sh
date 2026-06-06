#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-}"

if [[ -z "$VERSION" ]]; then
  echo "Usage: $0 <version>" >&2
  exit 2
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$ ]]; then
  echo "Error: '$VERSION' is not valid semver (MAJOR.MINOR.PATCH[-PRERELEASE])." >&2
  exit 2
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Error: working tree is not clean. Commit or stash changes before releasing." >&2
  exit 1
fi

BRANCH="releases/${VERSION}"
TAG="v${VERSION}"
SNAP_DIR="centsible-db/migrations/snapshot"
ARCHIVE_DIR="centsible-db/migrations/${VERSION}"
TABLES_SQL="centsible-db/tables.sql"

if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  echo "Error: branch ${BRANCH} already exists." >&2
  exit 1
fi
if git show-ref --verify --quiet "refs/tags/${TAG}"; then
  echo "Error: tag ${TAG} already exists." >&2
  exit 1
fi
if [[ -e "$ARCHIVE_DIR" ]]; then
  echo "Error: ${ARCHIVE_DIR} already exists." >&2
  exit 1
fi
if [[ ! -d "$SNAP_DIR" ]]; then
  echo "Error: ${SNAP_DIR} not found." >&2
  exit 1
fi

CURRENT_VERSION="$(grep -E '^\s*version\s*=\s*"' build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
SOURCE_REF="$(git rev-parse --abbrev-ref HEAD)"

echo "Cutting v${VERSION} from ${SOURCE_REF} (currently ${CURRENT_VERSION})."
echo

git switch -c "${BRANCH}"

python3 - "$VERSION" <<'PY'
import re, sys, pathlib
target = sys.argv[1]
p = pathlib.Path("build.gradle.kts")
text = p.read_text()
new, n = re.subn(r'(?m)^(\s*version\s*=\s*")[^"]+(")', rf'\g<1>{target}\g<2>', text, count=1)
if n == 0:
    raise SystemExit("Could not find  version = \"...\"  line in build.gradle.kts")
p.write_text(new)
PY
echo "  build.gradle.kts            -> ${VERSION}"

python3 - "$VERSION" <<'PY'
import re, sys, pathlib
target = sys.argv[1]
p = pathlib.Path("centsible-ui/package.json")
text = p.read_text()
if re.search(r'"version"\s*:\s*"', text):
    new, _ = re.subn(r'("version"\s*:\s*")[^"]+(")', rf'\g<1>{target}\g<2>', text, count=1)
else:
    new, n = re.subn(
        r'("name"\s*:\s*"[^"]+",)\n',
        rf'\g<1>\n  "version": "{target}",\n',
        text,
        count=1,
    )
    if n == 0:
        new = re.sub(r'^\{\n', '{\n  "version": "%s",\n' % target, text, count=1)
p.write_text(new)
PY
echo "  centsible-ui/package.json   -> ${VERSION}"

LAST_NUM="$(ls "${SNAP_DIR}" 2>/dev/null | grep -E '^[0-9]+_' | sed -E 's/^([0-9]+)_.*/\1/' | sort -n | tail -1 || true)"
NEXT_NUM="$(printf "%02d" "$(( 10#${LAST_NUM:-0} + 1 ))")"
SLUG="$(echo "${VERSION}" | tr '.-' '__')"
SETVERSION_FILE="${SNAP_DIR}/${NEXT_NUM}_SetVersion_${SLUG}.sql"
cat > "${SETVERSION_FILE}" <<SQL
UPDATE system_information
SET version     = '${VERSION}',
    released_at = CURRENT_DATE,
    modified_at = now()
WHERE id = 1;
SQL
echo "  ${SETVERSION_FILE}"

python3 - "$VERSION" "$TABLES_SQL" <<'PY'
import re, sys, pathlib
target, path = sys.argv[1], sys.argv[2]
p = pathlib.Path(path)
text = p.read_text(encoding="utf-8")
new, n = re.subn(
    r"'[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?'",
    f"'{target}'",
    text, count=1,
)
if n == 0:
    raise SystemExit("Could not find the system_information version literal in tables.sql")
p.write_text(new, encoding="utf-8")
PY
echo "  ${TABLES_SQL}: system_information.version -> ${VERSION}"

mv "${SNAP_DIR}" "${ARCHIVE_DIR}"
mkdir "${SNAP_DIR}"
if [[ -f "${ARCHIVE_DIR}/.gitkeep" ]]; then
  mv "${ARCHIVE_DIR}/.gitkeep" "${SNAP_DIR}/.gitkeep"
else
  : > "${SNAP_DIR}/.gitkeep"
fi
echo "  ${SNAP_DIR}/  ->  ${ARCHIVE_DIR}/  (+ fresh empty snapshot)"

python3 - "$VERSION" "$TABLES_SQL" "$ARCHIVE_DIR" <<'PY'
import sys, pathlib
version, tables_sql, archive = sys.argv[1], sys.argv[2], sys.argv[3]
folder = pathlib.Path(archive).name
files = sorted(f.name for f in pathlib.Path(archive).glob("*.sql"))
p = pathlib.Path(tables_sql)
text = p.read_text(encoding="utf-8")
if not text.endswith("\n"):
    text += "\n"
rows = "".join(
    f"INSERT INTO schema_migrations (version) VALUES ('{folder}/{f}') ON CONFLICT (version) DO NOTHING;\n"
    for f in files
)
p.write_text(text + rows, encoding="utf-8")
print(f"  recorded {len(files)} baseline row(s) for {folder}/")
PY

cat <<EOF

Release v${VERSION} prepared (uncommitted) on branch ${BRANCH}.

Before committing:
  1. Hand-merge the DDL from ${ARCHIVE_DIR}/ into ${TABLES_SQL}
     (tables.sql == base + every migration). The SetVersion file needs no merge.
  2. Update CHANGELOG.md for v${VERSION}.
  3. Review:  git status  &&  git diff

Then:
     git add -A
     git commit -m "chore(release): v${VERSION}"
     git tag -a ${TAG} -m "Centsible v${VERSION}"
     git push -u origin ${BRANCH}
     git push origin ${TAG}
EOF
