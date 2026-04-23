cmake_minimum_required(VERSION 3.14)
project(AgarioServer CXX)

# ═══════════════════════════════════════════════════════════════
#  Agar.io C++ Game Server — CentOS 8 / Linux Build
#  Requires: devtoolset-9 (GCC 9+), CMake 3.14+
#  Run install_deps.sh first to set up all dependencies.
# ═══════════════════════════════════════════════════════════════

# ─── C++17 ───────────────────────────────────────────────────
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ─── Build type default ──────────────────────────────────────
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

message(STATUS "Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "Compiler:   ${CMAKE_CXX_COMPILER} ${CMAKE_CXX_COMPILER_VERSION}")

# ─── Compiler flags ──────────────────────────────────────────
add_compile_options(
    -Wall -Wextra -Wno-unused-parameter
    -pthread
)

add_compile_definitions(
    _GNU_SOURCE
    BOOST_ASIO_USE_TS_EXECUTOR_AS_DEFAULT   # Boost 1.74+ on CentOS 8
)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_compile_options(-O3 -march=native -flto)
    add_link_options(-flto)
endif()

# ─── Dependency install prefix ───────────────────────────────
# install_deps.sh puts everything under /opt/agario
set(AGARIO_PREFIX "/opt/agario" CACHE PATH "Prefix where install_deps.sh installed libs")

list(PREPEND CMAKE_PREFIX_PATH
    "${AGARIO_PREFIX}"
    "/usr/local"
)

# ─── Boost ───────────────────────────────────────────────────
# CentOS 8 ships Boost 1.66 via dnf; install_deps.sh can build 1.82
find_package(Boost 1.66 REQUIRED COMPONENTS system)
message(STATUS "Boost version:  ${Boost_VERSION}")
message(STATUS "Boost includes: ${Boost_INCLUDE_DIRS}")

# ─── Protobuf ────────────────────────────────────────────────
find_package(Protobuf REQUIRED)
message(STATUS "Protobuf version: ${Protobuf_VERSION}")

# ─── MongoDB C++ driver ──────────────────────────────────────
find_package(mongocxx  REQUIRED)
find_package(bsoncxx   REQUIRED)

# ─── hiredis ─────────────────────────────────────────────────
find_library(HIREDIS_LIB  hiredis  REQUIRED
    HINTS "${AGARIO_PREFIX}/lib" /usr/local/lib /usr/lib64)
find_path(HIREDIS_INCLUDE hiredis/hiredis.h REQUIRED
    HINTS "${AGARIO_PREFIX}/include" /usr/local/include /usr/include)
message(STATUS "hiredis: ${HIREDIS_LIB}")

# ─── libdatachannel (WebRTC DataChannel) ─────────────────────
find_package(LibDataChannel QUIET
    HINTS "${AGARIO_PREFIX}/lib/cmake" /usr/local/lib/cmake)

if(NOT LibDataChannel_FOUND)
    # Fall back to manual path when cmake config not present
    find_library(DATACHANNEL_LIB datachannel REQUIRED
        HINTS "${AGARIO_PREFIX}/lib" /usr/local/lib)
    find_path(DATACHANNEL_INCLUDE rtc/rtc.hpp REQUIRED
        HINTS "${AGARIO_PREFIX}/include" /usr/local/include)
    message(STATUS "libdatachannel (manual): ${DATACHANNEL_LIB}")
    add_library(LibDataChannel::LibDataChannel UNKNOWN IMPORTED)
    set_target_properties(LibDataChannel::LibDataChannel PROPERTIES
        IMPORTED_LOCATION             "${DATACHANNEL_LIB}"
        INTERFACE_INCLUDE_DIRECTORIES "${DATACHANNEL_INCLUDE}")
else()
    message(STATUS "libdatachannel (cmake): found")
endif()

# ─── OpenSSL ─────────────────────────────────────────────────
find_package(OpenSSL REQUIRED)
message(STATUS "OpenSSL version: ${OPENSSL_VERSION}")

# ─── EnTT (header-only) ──────────────────────────────────────
# install_deps.sh clones it to ${CMAKE_SOURCE_DIR}/../vendor/entt
set(ENTT_INCLUDE "${CMAKE_SOURCE_DIR}/../vendor/entt/single_include"
    CACHE PATH "Path to EnTT single_include directory")
if(NOT EXISTS "${ENTT_INCLUDE}/entt/entt.hpp")
    message(FATAL_ERROR
        "EnTT not found at ${ENTT_INCLUDE}/entt/entt.hpp\n"
        "Run: git clone --depth=1 https://github.com/skypjack/entt.git "
        "${CMAKE_SOURCE_DIR}/../vendor/entt")
endif()

# ─── Protobuf code generation ────────────────────────────────
set(PROTO_FILE "${CMAKE_SOURCE_DIR}/../../proto/agario.proto")
set(PROTO_OUT  "${CMAKE_BINARY_DIR}/proto_gen")
file(MAKE_DIRECTORY "${PROTO_OUT}")

# Use protobuf_generate_cpp with explicit output dir
get_filename_component(PROTO_NAME "${PROTO_FILE}" NAME_WE)
set(PROTO_SRC "${PROTO_OUT}/${PROTO_NAME}.pb.cc")
set(PROTO_HDR "${PROTO_OUT}/${PROTO_NAME}.pb.h")

add_custom_command(
    OUTPUT  "${PROTO_SRC}" "${PROTO_HDR}"
    COMMAND "${Protobuf_PROTOC_EXECUTABLE}"
            "--cpp_out=${PROTO_OUT}"
            "--proto_path=${CMAKE_SOURCE_DIR}/../../proto"
            "${PROTO_FILE}"
    DEPENDS "${PROTO_FILE}"
    COMMENT "Generating protobuf sources from agario.proto"
    VERBATIM
)

# ─── Source files ────────────────────────────────────────────
set(SOURCES
    ${CMAKE_SOURCE_DIR}/../src/main.cpp
    ${CMAKE_SOURCE_DIR}/../src/SpatialGrid.cpp
    ${CMAKE_SOURCE_DIR}/../src/Systems.cpp
    ${CMAKE_SOURCE_DIR}/../src/GameLoop.cpp
    ${CMAKE_SOURCE_DIR}/../src/Database.cpp
    ${CMAKE_SOURCE_DIR}/../src/RedisClient.cpp
    ${CMAKE_SOURCE_DIR}/../src/PacketSerializer.cpp
    ${CMAKE_SOURCE_DIR}/../src/TcpServer.cpp
    ${CMAKE_SOURCE_DIR}/../src/UdpServer.cpp
    ${CMAKE_SOURCE_DIR}/../src/Player.cpp
    "${PROTO_SRC}"
)

add_executable(agario_server ${SOURCES})

# Suppress warnings in generated protobuf code
set_source_files_properties("${PROTO_SRC}" PROPERTIES
    COMPILE_FLAGS "-w")

# ─── Include directories ─────────────────────────────────────
target_include_directories(agario_server PRIVATE
    ${CMAKE_SOURCE_DIR}/../src
    ${PROTO_OUT}                  # generated agario.pb.h
    ${Boost_INCLUDE_DIRS}
    ${Protobuf_INCLUDE_DIRS}
    ${HIREDIS_INCLUDE}
    ${ENTT_INCLUDE}
    ${AGARIO_PREFIX}/include      # mongo, libdatachannel, etc.
    /usr/local/include
)

# ─── Link libraries ──────────────────────────────────────────
target_link_libraries(agario_server PRIVATE
    Boost::system
    ${Protobuf_LIBRARIES}
    mongo::mongocxx_shared
    mongo::bsoncxx_shared
    ${HIREDIS_LIB}
    LibDataChannel::LibDataChannel
    OpenSSL::SSL
    OpenSSL::Crypto
    pthread
    dl
    rt
)

# ─── Install ─────────────────────────────────────────────────
include(GNUInstallDirs)
install(TARGETS agario_server
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})

# ─── Summary ─────────────────────────────────────────────────
message(STATUS "")
message(STATUS "════════════════════════════════════════")
message(STATUS " AgarioServer configuration summary")
message(STATUS "────────────────────────────────────────")
message(STATUS " Build type  : ${CMAKE_BUILD_TYPE}")
message(STATUS " Install to  : ${CMAKE_INSTALL_PREFIX}")
message(STATUS " Dep prefix  : ${AGARIO_PREFIX}")
message(STATUS " EnTT        : ${ENTT_INCLUDE}")
message(STATUS "════════════════════════════════════════")
message(STATUS "")
