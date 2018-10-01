# Toolchain file for building for Windows from an Ubuntu Linux system.
#
# Typical usage:
#    *) install cross compiler: `sudo apt-get install mingw-w64 g++-mingw-w64`
#    *) cmake -DCMAKE_TOOLCHAIN_FILE=~/Toolchain-Ubuntu-mingw64.cmake ..

message(STATUS "Cross-compiling for Windows")

set(CMAKE_SYSTEM_NAME Windows)
set(TOOLCHAIN_PREFIX x86_64-w64-mingw32)

# cross compilers to use for C and C++
set(CMAKE_C_COMPILER "/home/pedro/.conan/data/statustoolchain-x86_64-w64-mingw32/1.23.0-1/status-im/experimental/package/6dd81ead6edc4ffe1e7b0f43c96eee6958954311/bin/${TOOLCHAIN_PREFIX}-gcc")
set(CMAKE_CXX_COMPILER "/home/pedro/.conan/data/statustoolchain-x86_64-w64-mingw32/1.23.0-1/status-im/experimental/package/6dd81ead6edc4ffe1e7b0f43c96eee6958954311/bin/${TOOLCHAIN_PREFIX}-g++")
set(CMAKE_RC_COMPILER "/home/pedro/.conan/data/statustoolchain-x86_64-w64-mingw32/1.23.0-1/status-im/experimental/package/6dd81ead6edc4ffe1e7b0f43c96eee6958954311/bin/${TOOLCHAIN_PREFIX}-windres")
include_directories("/home/pedro/.conan/data/statustoolchain-x86_64-w64-mingw32/1.23.0-1/status-im/experimental/package/6dd81ead6edc4ffe1e7b0f43c96eee6958954311/x86_64-w64-mingw32/sysroot/usr/x86_64-w64-mingw32/include")
#include_directories("/usr/lib/gcc/x86_64-w64-mingw32/7.3-win32/include/c++")
#include_directories("/usr/x86_64-w64-mingw32/include")

list(APPEND CMAKE_FIND_ROOT_PATH /home/pedro/.conan/data/statustoolchain-x86_64-w64-mingw32/1.23.0-1/status-im/experimental/package/6dd81ead6edc4ffe1e7b0f43c96eee6958954311/${TOOLCHAIN_PREFIX})
