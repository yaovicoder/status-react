# Toolchain file for building for Windows from an Ubuntu Linux system.
#
# Typical usage:
#    *) install cross compiler: `sudo apt-get install mingw-w64 g++-mingw-w64`
#    *) cmake -DCMAKE_TOOLCHAIN_FILE=~/Toolchain-Ubuntu-mingw64.cmake ..

message(STATUS "Cross-compiling for Windows")

set(CMAKE_SYSTEM_NAME Windows)
set(TOOLCHAIN_PREFIX x86_64-w64-mingw32)
SET(CMAKE_FIND_ROOT_PATH /usr/${TOOLCHAIN_PREFIX})

# cross compilers to use for C and C++
set(CMAKE_C_COMPILER "/usr/bin/${TOOLCHAIN_PREFIX}-gcc")
set(CMAKE_CXX_COMPILER "/usr/bin/${TOOLCHAIN_PREFIX}-g++")
set(CMAKE_RC_COMPILER "/usr/bin/${TOOLCHAIN_PREFIX}-windres")
