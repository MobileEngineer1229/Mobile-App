#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  Agar.io Server — CentOS 8 Dependency Installer
#  Tested on: CentOS 8.5 (vault), CentOS Stream 8
#
#  Run as root or with sudo:
#      sudo bash install_deps.sh
#
#  What this script does:
#    1. Fixes CentOS 8 EOL repo URLs (vault.centos.org)
#    2. Enables devtoolset-9 for GCC 9 / C++17 support
#    3. Installs system packages (CMake, Boost, OpenSSL, etc.)
#    4. Builds mongo-c-driver and mongo-cxx-driver from source
#    5. Builds libdatachannel (WebRTC DataChannel) from source
#    6. Installs hiredis from source
#    7. Clones EnTT (header-only) to vendor/entt
#    8. Installs protobuf from source (3.21)
#
#  All built libs are installed to /opt/agario to avoid
#  polluting system directories.
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

# ── Config ───────────────────────────────────────────────────
PREFIX="/opt/agario"
JOBS=$(nproc)
BUILD_DIR="/tmp/agario_build"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENDOR_DIR="$SCRIPT_DIR/../vendor"

# Versions
MONGOC_VERSION="1.24.4"
MONGOCXX_VERSION="3.8.1"
PROTOBUF_VERSION="3.21.12"
HIREDIS_VERSION="1.2.0"
LIBDATACHANNEL_VERSION="0.19.5"
BOOST_VERSION="1.82.0"
BOOST_VER_UNDERSCORE="1_82_0"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ── Helpers ──────────────────────────────────────────────────
require_root() {
    if [[ $EUID -ne 0 ]]; then
        error "This script must be run as root (sudo bash install_deps.sh)"
        exit 1
    fi
}

step() { echo; echo -e "${GREEN}══ Step $1: $2 ══${NC}"; }

# ── Step 0: Fix CentOS 8 EOL repos ──────────────────────────
fix_centos8_repos() {
    step 0 "Fix CentOS 8 EOL package repositories"

    # Detect if we are on CentOS 8 (not Stream)
    if grep -q "CentOS Linux 8" /etc/os-release 2>/dev/null; then
        warn "CentOS 8 reached EOL on 2021-12-31."
        warn "Switching to vault.centos.org mirrors..."

        # Backup original repo files
        mkdir -p /etc/yum.repos.d/backup
        cp /etc/yum.repos.d/CentOS-*.repo /etc/yum.repos.d/backup/ 2>/dev/null || true

        # Redirect all BaseOS/AppStream/PowerTools/Extras to vault
        find /etc/yum.repos.d/ -name "CentOS-*.repo" | while read -r repo; do
            sed -i \
                -e 's|mirrorlist=|#mirrorlist=|g' \
                -e 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' \
                "$repo"
        done

        info "Repos updated to vault.centos.org"
    elif grep -q "CentOS Stream 8" /etc/os-release 2>/dev/null; then
        info "CentOS Stream 8 detected — repos are still active."
    else
        warn "Not CentOS 8; skipping repo fix."
    fi
}

# ── Step 1: System packages ──────────────────────────────────
install_system_packages() {
    step 1 "Install system packages via dnf"

    dnf -y update --nobest || true

    # Enable EPEL and PowerTools (needed for some dev packages)
    dnf -y install epel-release || true
    dnf config-manager --set-enabled powertools 2>/dev/null || \
    dnf config-manager --set-enabled PowerTools 2>/dev/null || true

    # Developer tools (SCL for GCC 9)
    dnf -y install centos-release-scl || true
    dnf -y install devtoolset-9-gcc devtoolset-9-gcc-c++ devtoolset-9-binutils

    # Core build tools
    dnf -y install \
        cmake3 \
        git \
        make \
        ninja-build \
        wget \
        curl \
        unzip \
        pkg-config \
        autoconf \
        automake \
        libtool

    # OpenSSL
    dnf -y install openssl-devel

    # Boost (system package, may be older than 1.82)
    dnf -y install boost-devel || true

    # Other dev libs
    dnf -y install \
        zlib-devel \
        cyrus-sasl-devel \
        krb5-devel \
        openldap-devel \
        libuuid-devel

    # Create cmake3 → cmake symlink if needed
    if ! command -v cmake &>/dev/null && command -v cmake3 &>/dev/null; then
        ln -sf /usr/bin/cmake3 /usr/local/bin/cmake
        info "Created cmake → cmake3 symlink"
    fi

    info "System packages installed."
}

# ── Step 2: Enable devtoolset-9 ──────────────────────────────
enable_devtoolset() {
    step 2 "Enable devtoolset-9 (GCC 9 / C++17)"

    # Source devtoolset for the rest of this script
    # shellcheck disable=SC1091
    source /opt/rh/devtoolset-9/enable

    GCC_VER=$(gcc --version | head -1)
    info "Active compiler: $GCC_VER"

    # Add permanent activation to /etc/profile.d
    cat > /etc/profile.d/devtoolset9.sh << 'EOF'
# Enable devtoolset-9 (GCC 9) for C++17 support
source /opt/rh/devtoolset-9/enable
EOF
    info "devtoolset-9 will be enabled for all future logins."
}

# ── Step 3: Boost 1.82 (optional, replaces system Boost) ─────
install_boost() {
    step 3 "Install Boost ${BOOST_VERSION}"

    if pkg-config --exists "boost >= 1.66" 2>/dev/null; then
        INSTALLED=$(pkg-config --modversion boost 2>/dev/null || echo "unknown")
        warn "Boost $INSTALLED found. Skipping build (need ≥1.66, have ${INSTALLED})."
        warn "Set BOOST_FORCE=1 to rebuild Boost ${BOOST_VERSION}."
        [[ "${BOOST_FORCE:-0}" != "1" ]] && return 0
    fi

    BOOST_TAR="boost_${BOOST_VER_UNDERSCORE}.tar.gz"
    BOOST_URL="https://boostorg.jfrog.io/artifactory/main/release/${BOOST_VERSION}/source/${BOOST_TAR}"

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"

    if [[ ! -f "$BOOST_TAR" ]]; then
        info "Downloading Boost ${BOOST_VERSION}..."
        wget -q --show-progress "$BOOST_URL" -O "$BOOST_TAR"
    fi

    tar -xzf "$BOOST_TAR"
    cd "boost_${BOOST_VER_UNDERSCORE}"

    ./bootstrap.sh --prefix="$PREFIX" --with-libraries=system,thread,date_time,regex,filesystem
    ./b2 install --prefix="$PREFIX" -j"$JOBS" variant=release link=shared threading=multi

    info "Boost ${BOOST_VERSION} installed to ${PREFIX}."
}

# ── Step 4: Protobuf 3.21 ────────────────────────────────────
install_protobuf() {
    step 4 "Install Protobuf ${PROTOBUF_VERSION}"

    if command -v protoc &>/dev/null; then
        INSTALLED=$(protoc --version | awk '{print $2}')
        if [[ "$INSTALLED" == "$PROTOBUF_VERSION" ]]; then
            info "protoc $INSTALLED already installed. Skipping."
            return 0
        fi
    fi

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"
    PROTO_DIR="protobuf-${PROTOBUF_VERSION}"

    if [[ ! -d "$PROTO_DIR" ]]; then
        info "Downloading protobuf ${PROTOBUF_VERSION}..."
        wget -q --show-progress \
            "https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOBUF_VERSION}/protobuf-cpp-${PROTOBUF_VERSION}.tar.gz" \
            -O "protobuf-cpp-${PROTOBUF_VERSION}.tar.gz"
        tar -xzf "protobuf-cpp-${PROTOBUF_VERSION}.tar.gz"
    fi

    cd "$PROTO_DIR"
    mkdir -p build && cd build

    cmake .. \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" \
        -DCMAKE_BUILD_TYPE=Release \
        -Dprotobuf_BUILD_TESTS=OFF \
        -Dprotobuf_BUILD_SHARED_LIBS=ON

    make -j"$JOBS"
    make install

    # Add to ldconfig
    echo "$PREFIX/lib" > /etc/ld.so.conf.d/agario.conf
    ldconfig

    info "Protobuf ${PROTOBUF_VERSION} installed."
}

# ── Step 5: hiredis ──────────────────────────────────────────
install_hiredis() {
    step 5 "Install hiredis ${HIREDIS_VERSION}"

    if [[ -f "$PREFIX/lib/libhiredis.so" ]]; then
        info "hiredis already installed. Skipping."
        return 0
    fi

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"

    if [[ ! -d "hiredis" ]]; then
        git clone --depth=1 --branch "v${HIREDIS_VERSION}" \
            https://github.com/redis/hiredis.git hiredis
    fi

    cd hiredis
    mkdir -p build && cd build

    cmake .. \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" \
        -DCMAKE_BUILD_TYPE=Release \
        -DENABLE_SSL=ON

    make -j"$JOBS"
    make install
    ldconfig

    info "hiredis ${HIREDIS_VERSION} installed."
}

# ── Step 6: MongoDB C driver ─────────────────────────────────
install_mongoc() {
    step 6 "Install mongo-c-driver ${MONGOC_VERSION}"

    if [[ -f "$PREFIX/lib/libmongoc-1.0.so" ]]; then
        info "mongo-c-driver already installed. Skipping."
        return 0
    fi

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"

    MONGOC_TAR="mongo-c-driver-${MONGOC_VERSION}.tar.gz"
    if [[ ! -f "$MONGOC_TAR" ]]; then
        wget -q --show-progress \
            "https://github.com/mongodb/mongo-c-driver/releases/download/${MONGOC_VERSION}/${MONGOC_TAR}" \
            -O "$MONGOC_TAR"
        tar -xzf "$MONGOC_TAR"
    fi

    cd "mongo-c-driver-${MONGOC_VERSION}"
    mkdir -p build && cd build

    cmake .. \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" \
        -DCMAKE_BUILD_TYPE=Release \
        -DENABLE_AUTOMATIC_INIT_AND_CLEANUP=OFF \
        -DENABLE_TESTS=OFF \
        -DENABLE_EXAMPLES=OFF

    make -j"$JOBS"
    make install
    ldconfig

    info "mongo-c-driver ${MONGOC_VERSION} installed."
}

# ── Step 7: MongoDB C++ driver ───────────────────────────────
install_mongocxx() {
    step 7 "Install mongo-cxx-driver ${MONGOCXX_VERSION}"

    if [[ -f "$PREFIX/lib/libmongocxx.so" ]]; then
        info "mongo-cxx-driver already installed. Skipping."
        return 0
    fi

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"

    MONGOCXX_TAR="mongo-cxx-driver-r${MONGOCXX_VERSION}.tar.gz"
    if [[ ! -f "$MONGOCXX_TAR" ]]; then
        wget -q --show-progress \
            "https://github.com/mongodb/mongo-cxx-driver/releases/download/r${MONGOCXX_VERSION}/${MONGOCXX_TAR}" \
            -O "$MONGOCXX_TAR"
        tar -xzf "$MONGOCXX_TAR"
    fi

    cd "mongo-cxx-driver-r${MONGOCXX_VERSION}/build"

    cmake .. \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_PREFIX_PATH="$PREFIX" \
        -DBSONCXX_POLY_USE_STD=1 \
        -DMONGOCXX_OVERRIDE_DEFAULT_INSTALL_PREFIX=OFF

    make -j"$JOBS"
    make install
    ldconfig

    info "mongo-cxx-driver ${MONGOCXX_VERSION} installed."
}

# ── Step 8: libdatachannel ────────────────────────────────────
install_libdatachannel() {
    step 8 "Install libdatachannel ${LIBDATACHANNEL_VERSION} (WebRTC DataChannel)"

    if [[ -f "$PREFIX/lib/libdatachannel.so" ]]; then
        info "libdatachannel already installed. Skipping."
        return 0
    fi

    mkdir -p "$BUILD_DIR" && cd "$BUILD_DIR"

    if [[ ! -d "libdatachannel" ]]; then
        git clone --recursive --depth=1 \
            --branch "v${LIBDATACHANNEL_VERSION}" \
            https://github.com/paullouisageneau/libdatachannel.git
    fi

    cd libdatachannel
    # Update submodules if not already fetched
    git submodule update --init --recursive --depth=1

    mkdir -p build && cd build

    cmake .. \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" \
        -DCMAKE_BUILD_TYPE=Release \
        -DUSE_GNUTLS=OFF \
        -DUSE_MBEDTLS=OFF \
        -DOPENSSL_USE_STATIC_LIBS=FALSE \
        -DNO_EXAMPLES=ON \
        -DNO_TESTS=ON

    make -j"$JOBS"
    make install
    ldconfig

    info "libdatachannel ${LIBDATACHANNEL_VERSION} installed."
}

# ── Step 9: EnTT (header-only) ───────────────────────────────
install_entt() {
    step 9 "Install EnTT (header-only ECS)"

    mkdir -p "$VENDOR_DIR"

    if [[ -f "$VENDOR_DIR/entt/single_include/entt/entt.hpp" ]]; then
        info "EnTT already present at $VENDOR_DIR/entt. Skipping."
        return 0
    fi

    git clone --depth=1 https://github.com/skypjack/entt.git "$VENDOR_DIR/entt"
    info "EnTT cloned to $VENDOR_DIR/entt"
}

# ── Step 10: MongoDB Community Server ────────────────────────
check_mongodb() {
    step 10 "Check MongoDB server"

    if command -v mongod &>/dev/null; then
        info "mongod found: $(mongod --version | head -1)"
        return 0
    fi

    warn "MongoDB not found. Installing MongoDB 6.0 Community..."

    cat > /etc/yum.repos.d/mongodb-org-6.0.repo << 'EOF'
[mongodb-org-6.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/8/mongodb-org/6.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-6.0.asc
EOF

    dnf install -y mongodb-org || {
        warn "Could not install MongoDB from repo."
        warn "Install manually: https://www.mongodb.com/try/download/community"
        return 0
    }

    systemctl enable --now mongod
    info "MongoDB 6.0 installed and started."
    info "Data directory: /var/lib/mongo"
    info "Log file:       /var/log/mongodb/mongod.log"
}

# ── Step 11: Redis ────────────────────────────────────────────
check_redis() {
    step 11 "Check Redis server"

    if command -v redis-server &>/dev/null; then
        info "redis-server found: $(redis-server --version)"
        return 0
    fi

    warn "Redis not found. Installing via dnf/epel..."
    dnf install -y redis || {
        warn "Could not install Redis. Install manually: dnf install redis"
        return 0
    }

    systemctl enable --now redis
    info "Redis installed and started."
}

# ── Final: Write env file ─────────────────────────────────────
write_env_file() {
    cat > /etc/profile.d/agario_env.sh << EOF
# Agar.io server environment
export PKG_CONFIG_PATH="$PREFIX/lib/pkgconfig:\$PKG_CONFIG_PATH"
export CMAKE_PREFIX_PATH="$PREFIX:\$CMAKE_PREFIX_PATH"
export LD_LIBRARY_PATH="$PREFIX/lib:/opt/rh/devtoolset-9/root/usr/lib64:\$LD_LIBRARY_PATH"
export PATH="$PREFIX/bin:\$PATH"
EOF

    info "Environment written to /etc/profile.d/agario_env.sh"
    info "Source it with: source /etc/profile.d/agario_env.sh"
}

# ── Main ──────────────────────────────────────────────────────
main() {
    echo
    echo "═══════════════════════════════════════════════════════════"
    echo "  Agar.io Server — CentOS 8 Dependency Setup"
    echo "  Install prefix: $PREFIX"
    echo "  Parallel jobs:  $JOBS"
    echo "═══════════════════════════════════════════════════════════"
    echo

    require_root

    mkdir -p "$PREFIX" "$BUILD_DIR"

    fix_centos8_repos
    install_system_packages
    enable_devtoolset
    install_boost
    install_protobuf
    install_hiredis
    install_mongoc
    install_mongocxx
    install_libdatachannel
    install_entt
    check_mongodb
    check_redis
    write_env_file

    echo
    echo "═══════════════════════════════════════════════════════════"
    echo "  Setup complete!"
    echo
    echo "  Next steps:"
    echo "  1. Source the env:  source /etc/profile.d/agario_env.sh"
    echo "     (or log out and back in)"
    echo "  2. Build the server:"
    echo "     cd server/linux"
    echo "     mkdir build && cd build"
    echo "     cmake .. -DCMAKE_BUILD_TYPE=Release"
    echo "     make -j\$(nproc)"
    echo "  3. Start MongoDB:   systemctl start mongod"
    echo "  4. Start Redis:     systemctl start redis"
    echo "  5. Run:             ./agario_server"
    echo "═══════════════════════════════════════════════════════════"
}

main "$@"
