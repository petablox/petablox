# WARNING: Don't execute this file directly.

# This fragment checks that the main script was called correctly (i.e., exactly
# one argument), and retrieves the absolute path to any directory passed to it.
# It expects that the variable ARG_DESCRIPTION has been set to a description of
# the expected single argument.

# =============================================================================

if [ "$#" -ne "1" ]; then
    SCRIPT_NAME=$(basename "$0")
    echo "Usage: $SCRIPT_NAME <$ARG_DESCRIPTION>"
    exit
fi

if [ -d "$1" ]; then
    ARG="$(cd "$1"; pwd)"
else
    ARG="$1"
fi
