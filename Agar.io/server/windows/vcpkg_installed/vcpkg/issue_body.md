Package: zlib:x64-windows@1.3.1

**Host Environment**

- Host: x64-windows
- Compiler: MSVC 19.50.35728.0
- CMake Version: 4.2.3
-    vcpkg-tool version: 2026-04-08-e0612b42ce44e55a0e630f2ee9d3c533a63d8bc1
    vcpkg-scripts version: d13fa75214 2026-04-14 (4 hours ago)

**To Reproduce**

`vcpkg install `

**Failure logs**

```
Downloading https://github.com/madler/zlib/archive/v1.3.1.tar.gz -> madler-zlib-v1.3.1.tar.gz
error: curl operation failed with error code 35 (SSL connect error).
error: Not a transient network error, won't retry download from https://github.com/madler/zlib/archive/v1.3.1.tar.gz
note: If you are using a proxy, please ensure your proxy settings are correct.
Possible causes are:
1. You are actually using an HTTP proxy, but setting HTTPS_PROXY variable to `https://address:port`.
This is not correct, because `https://` prefix claims the proxy is an HTTPS proxy, while your proxy (v2ray, shadowsocksr, etc...) is an HTTP proxy.
Try setting `http://address:port` to both HTTP_PROXY and HTTPS_PROXY instead.
2. If you are using Windows, vcpkg will automatically use your Windows IE Proxy Settings set by your proxy software. See: https://github.com/microsoft/vcpkg-tool/pull/77
The value set by your proxy might be wrong, or have same `https://` prefix issue.
3. Your proxy's remote server is out of service.
If you believe this is not a temporary download server failure and vcpkg needs to be changed to download this file from a different location, please submit an issue to https://github.com/Microsoft/vcpkg/issues
CMake Error at scripts/cmake/vcpkg_download_distfile.cmake:136 (message):
  Download failed, halting portfile.
Call Stack (most recent call first):
  scripts/cmake/vcpkg_from_github.cmake:120 (vcpkg_download_distfile)
  buildtrees/versioning_/versions/zlib/3f05e04b9aededb96786a911a16193cdb711f0c9/portfile.cmake:2 (vcpkg_from_github)
  scripts/ports.cmake:206 (include)



```

**Additional context**

<details><summary>vcpkg.json</summary>

```
{
  "$schema": "https://raw.githubusercontent.com/microsoft/vcpkg-tool/main/docs/vcpkg.schema.json",
  "name": "agario-server",
  "version": "1.0.0",
  "description": "Agar.io C++ Game Server",
  "dependencies": [
    {
      "name": "boost-asio",
      "version>=": "1.82.0"
    },
    {
      "name": "boost-beast",
      "version>=": "1.82.0"
    },
    {
      "name": "boost-system",
      "version>=": "1.82.0"
    },
    {
      "name": "protobuf",
      "version>=": "3.21.12"
    },
    {
      "name": "mongo-cxx-driver",
      "version>=": "3.8.0"
    },
    "hiredis",
    "libdatachannel",
    "openssl"
  ],
  "overrides": [],
  "builtin-baseline": "d13fa75214c258099923cf25a5e6311e58c07f3b"
}

```
</details>
