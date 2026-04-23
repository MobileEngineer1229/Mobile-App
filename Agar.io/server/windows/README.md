# Agar.io Server — Windows 10 (Visual Studio 2017)

C++ game server built with MSBuild/VS 2017, targeting Windows 10 x64.

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Visual Studio | 2017 (15.x) | [visualstudio.microsoft.com](https://visualstudio.microsoft.com/vs/older-downloads/) |
| VS Workload | Desktop development with C++ | (select during VS install) |
| Git | any | [git-scm.com](https://git-scm.com) |
| CMake | 3.14+ | [cmake.org](https://cmake.org/download/) |

> Visual Studio 2022 or 2019 also works — change `PlatformToolset` from
> `v141` to `v142` / `v143` in `AgarServer.vcxproj`.

## Quick Start

```bat
REM 1. Install all C++ dependencies (~20-30 min)
cd server\windows
install_deps.bat

REM 2. Set VCPKG_ROOT if not already set (installer prints the path)
setx VCPKG_ROOT C:\vcpkg

REM 3. Open the solution
start AgarServer.sln

REM 4. In Visual Studio: Build → Build Solution  (Release | x64)

REM 5. Run
bin\Release\AgarServer.exe
```

## What install_deps.bat Does

| Step | Action |
|------|--------|
| 1 | Clones **vcpkg** to `C:\vcpkg` (or `%VCPKG_ROOT%`) and bootstraps it |
| 2 | Runs `vcpkg integrate install` (auto-links all installed packages to VS) |
| 3 | Installs packages for `x64-windows` triplet: `boost-asio`, `boost-beast`, `boost-system`, `protobuf`, `mongo-cxx-driver`, `hiredis`, `libdatachannel`, `openssl` |
| 4 | Clones **EnTT** (header-only) to `server\vendor\entt` |
| 5 | Checks for MongoDB and Redis; prints download links if not found |

## Project Files

| File | Purpose |
|------|---------|
| `AgarServer.sln` | VS 2017 solution (open this) |
| `AgarServer.vcxproj` | MSBuild project — sources, includes, link libs, protoc pre-build event |
| `AgarServer.vcxproj.filters` | Solution Explorer grouping (Network / Game / Data / Proto) |
| `vcpkg.json` | vcpkg manifest (dependency list with version constraints) |
| `install_deps.bat` | One-click dependency installer |

## Build Configurations

| Config | Description |
|--------|-------------|
| `Debug | x64` | `/Od`, debug CRT (`/MDd`), full debug info |
| `Release | x64` | `/O2`, release CRT (`/MD`), link-time optimization (LTCG) |

## Runtime Services

Start these before running the server:

```bat
REM MongoDB — open a new terminal
mongod --dbpath C:\data\db

REM Redis / Memurai — open a new terminal
redis-server
REM or: start Memurai from Start Menu
```

## Directory Layout

```
server\
├── windows\
│   ├── AgarServer.sln          ← open in VS 2017
│   ├── AgarServer.vcxproj
│   ├── AgarServer.vcxproj.filters
│   ├── vcpkg.json
│   ├── install_deps.bat
│   └── README.md
├── linux\
│   ├── CMakeLists.txt
│   ├── install_deps.sh
│   └── README.md
├── src\                        ← shared C++ source (both platforms)
│   ├── main.cpp
│   ├── GameLoop.cpp/.h
│   ├── Systems.cpp/.h
│   ├── SpatialGrid.cpp/.h
│   ├── TcpServer.cpp/.h        ← Boost.Beast WebSocket
│   ├── UdpServer.cpp/.h        ← libdatachannel WebRTC
│   ├── Database.cpp/.h         ← MongoDB async writer
│   ├── RedisClient.cpp/.h
│   ├── PacketSerializer.cpp/.h
│   ├── Player.cpp/.h
│   └── Config.h
└── vendor\
    └── entt\                   ← cloned by install_deps.bat
```

## Troubleshooting

### `vcpkg integrate install` requires admin
Run `install_deps.bat` from an **Administrator** command prompt.

### `protoc.exe not found` in pre-build event
Ensure `%VCPKG_ROOT%` is set as a system environment variable:
```bat
setx VCPKG_ROOT C:\vcpkg /M
```
Then restart Visual Studio.

### Linker error: `unresolved external symbol mongocxx`
Make sure vcpkg integration is active. In VS: `Tools → Command Line → Developer Command Prompt`:
```bat
C:\vcpkg\vcpkg.exe integrate install
```

### MongoDB not connecting
```bat
REM Create data directory and start mongod
mkdir C:\data\db
mongod --dbpath C:\data\db --port 27017
```

### `hiredis.lib: cannot open file`
Build the debug configuration first — it links `hiredis.lib` (release).  
Or swap to release: `Configuration Manager → Release | x64`.

## Architecture Notes

- **Thread model**: main thread (TCP), game thread (ECS loop at 20 Hz), DB worker thread (MongoDB), Redis sub thread
- **Transport**: TCP WebSocket (Boost.Beast) for reliable events; WebRTC DataChannel (libdatachannel) for battle packets
- **Serialization**: Protobuf — see `..\..\proto\agario.proto`
- **ECS**: EnTT — contiguous component arrays, no virtual dispatch in hot path
- **Spatial index**: Static grid (CELL_SIZE=1000, 9-cell AoI query)
- **Pre-build event**: runs `protoc.exe` to regenerate `agario.pb.h` / `agario.pb.cc` on every build
