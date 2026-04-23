# Agar.io Server — Linux (CentOS 8)

C++ game server built with CMake, targeting CentOS 8.x / CentOS Stream 8.

## Prerequisites

| Tool | Minimum | Notes |
|------|---------|-------|
| OS | CentOS 8.5 | Stream 8 also works; see EOL note below |
| GCC | 9.x | Via `devtoolset-9` (SCL) |
| CMake | 3.14+ | `cmake3` from EPEL |
| Git | any | For cloning deps |

> **CentOS 8 EOL**: CentOS Linux 8 reached end-of-life on 2021-12-31.
> `install_deps.sh` automatically rewrites repo URLs to `vault.centos.org`
> so `dnf` can still resolve packages.

## Quick Start

```bash
# 1. Install all dependencies (run as root, takes ~20-40 min)
sudo bash install_deps.sh

# 2. Activate devtoolset-9 in your current shell
source /opt/rh/devtoolset-9/enable
source /etc/profile.d/agario_env.sh

# 3. Configure and build
cd server/linux
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)

# 4. Run
./agario_server
```

## What install_deps.sh Does

| Step | Action |
|------|--------|
| 0 | Fixes CentOS 8 EOL repo URLs → `vault.centos.org` |
| 1 | Installs system packages via `dnf` (cmake3, openssl-devel, boost-devel…) |
| 2 | Enables `devtoolset-9` (GCC 9) for C++17 support |
| 3 | Builds **Boost 1.82** from source (optional, system Boost 1.66 also works) |
| 4 | Builds **Protobuf 3.21.12** from source |
| 5 | Builds **hiredis 1.2.0** from source (with SSL) |
| 6 | Builds **mongo-c-driver 1.24.4** from source |
| 7 | Builds **mongo-cxx-driver 3.8.1** from source |
| 8 | Builds **libdatachannel 0.19.5** from source (WebRTC DataChannel) |
| 9 | Clones **EnTT** (header-only ECS) to `vendor/entt` |
| 10 | Installs/checks **MongoDB 6.0** Community |
| 11 | Installs/checks **Redis** |

All libraries are installed to `/opt/agario` to avoid polluting system paths.

## CMake Options

| Option | Default | Description |
|--------|---------|-------------|
| `CMAKE_BUILD_TYPE` | `Release` | `Debug` or `Release` |
| `AGARIO_PREFIX` | `/opt/agario` | Where `install_deps.sh` installed libraries |
| `ENTT_INCLUDE` | `../vendor/entt/single_include` | Path to EnTT headers |

Example with custom prefix:
```bash
cmake .. \
    -DCMAKE_BUILD_TYPE=Debug \
    -DAGARIO_PREFIX=/usr/local \
    -DCMAKE_INSTALL_PREFIX=/opt/agario_server
```

## Directory Layout

```
server/
├── linux/
│   ├── CMakeLists.txt      ← this project
│   ├── install_deps.sh     ← dependency installer
│   └── README.md
├── windows/
│   ├── AgarServer.sln      ← VS 2017 solution
│   └── ...
├── src/                    ← shared C++ source (both platforms)
│   ├── main.cpp
│   ├── GameLoop.cpp/.h
│   ├── Systems.cpp/.h
│   ├── SpatialGrid.cpp/.h
│   ├── TcpServer.cpp/.h    ← Boost.Beast WebSocket
│   ├── UdpServer.cpp/.h    ← libdatachannel WebRTC
│   ├── Database.cpp/.h     ← MongoDB async writer
│   ├── RedisClient.cpp/.h
│   ├── PacketSerializer.cpp/.h
│   ├── Player.cpp/.h
│   └── Config.h
└── vendor/
    └── entt/               ← cloned by install_deps.sh
```

## Runtime

Start dependencies before running the server:

```bash
# MongoDB
sudo systemctl start mongod
# verify
mongo --eval "db.runCommand({ connectionStatus: 1 })"

# Redis
sudo systemctl start redis
# verify
redis-cli ping   # should reply: PONG

# Run server (default: TCP :9000, WebRTC signaling on same port)
./build/agario_server
```

## Troubleshooting

### `devtoolset-9: command not found`
```bash
sudo dnf install centos-release-scl
sudo dnf install devtoolset-9-gcc devtoolset-9-gcc-c++
source /opt/rh/devtoolset-9/enable
```

### `cmake: command not found`
```bash
sudo dnf install cmake3
sudo ln -sf /usr/bin/cmake3 /usr/local/bin/cmake
```

### `libmongocxx.so: cannot open shared object file`
```bash
echo "/opt/agario/lib" | sudo tee /etc/ld.so.conf.d/agario.conf
sudo ldconfig
```

### MongoDB connection refused
```bash
sudo systemctl status mongod
# If not started:
sudo mkdir -p /var/lib/mongo /var/log/mongodb
sudo chown mongod:mongod /var/lib/mongo /var/log/mongodb
sudo systemctl start mongod
```

### `protoc: command not found`
```bash
export PATH="/opt/agario/bin:$PATH"
# or re-source:
source /etc/profile.d/agario_env.sh
```

## Architecture Notes

- **Thread model**: main thread (TCP), game thread (ECS loop at 20 Hz), DB worker thread (MongoDB), Redis sub thread
- **Transport**: TCP WebSocket (Boost.Beast) for reliable events; WebRTC DataChannel (libdatachannel) for battle packets
- **Serialization**: Protobuf — see `../../proto/agario.proto`
- **ECS**: EnTT — contiguous component arrays, no virtual dispatch in hot path
- **Spatial index**: Static grid (CELL_SIZE=1000, 9-cell AoI query)
