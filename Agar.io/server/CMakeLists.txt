cmake_minimum_required(VERSION 3.14)
project(AgarioServer CXX)

# ─── C++ Standard ────────────────────────────────────────────
# VS 2017 supports C++17 with /std:c++17 flag
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ─── VS 2017 specific ────────────────────────────────────────
if(MSVC)
    add_compile_options(/W3 /MP /bigobj)
    add_compile_definitions(_WIN32_WINNT=0x0601)  # Windows 7+
endif()

# ─── Find packages ────────────────────────────────────────────
find_package(Boost 1.70 REQUIRED COMPONENTS system)
find_package(Protobuf REQUIRED)
find_package(mongocxx REQUIRED)
find_package(bsoncxx  REQUIRED)

# EnTT is header-only (include manually or via vcpkg)
# libdatachannel for WebRTC DataChannel
find_package(LibDataChannel REQUIRED)

# hiredis for Redis
find_library(HIREDIS_LIB hiredis REQUIRED)
find_path(HIREDIS_INCLUDE hiredis/hiredis.h REQUIRED)

# OpenSSL (required by Boost.Beast for WSS + libdatachannel for DTLS)
find_package(OpenSSL REQUIRED)

# ─── Protobuf code generation ────────────────────────────────
set(PROTO_FILE ${CMAKE_SOURCE_DIR}/../proto/agario.proto)
set(PROTO_OUT  ${CMAKE_BINARY_DIR}/proto_gen)
file(MAKE_DIRECTORY ${PROTO_OUT})

protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS
    ${PROTO_FILE}
)

# ─── Source files ────────────────────────────────────────────
set(SOURCES
    src/main.cpp
    src/SpatialGrid.cpp
    src/Systems.cpp
    src/GameLoop.cpp
    src/Database.cpp
    src/RedisClient.cpp
    src/PacketSerializer.cpp
    src/TcpServer.cpp
    src/UdpServer.cpp
    src/Player.cpp
    ${PROTO_SRCS}
)

add_executable(agario_server ${SOURCES})

target_include_directories(agario_server PRIVATE
    ${CMAKE_SOURCE_DIR}/src
    ${CMAKE_BINARY_DIR}           # for generated agario.pb.h
    ${Boost_INCLUDE_DIRS}
    ${Protobuf_INCLUDE_DIRS}
    ${MONGO_CXX_DRIVER_INCLUDE_DIR}
    ${HIREDIS_INCLUDE}
    # EnTT — download from github.com/skypjack/entt and put in vendor/
    ${CMAKE_SOURCE_DIR}/vendor/entt/single_include
    # libdatachannel headers
)

target_link_libraries(agario_server PRIVATE
    Boost::system
    ${Protobuf_LIBRARIES}
    mongo::mongocxx_shared
    mongo::bsoncxx_shared
    ${HIREDIS_LIB}
    LibDataChannel::LibDataChannel
    OpenSSL::SSL
    OpenSSL::Crypto
)

if(WIN32)
    target_link_libraries(agario_server PRIVATE ws2_32 wsock32)
endif()

# ─── Install ─────────────────────────────────────────────────
install(TARGETS agario_server DESTINATION bin)
