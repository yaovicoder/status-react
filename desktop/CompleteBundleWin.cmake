#set(PROJECT_SOURCE_DIR /home/pedro/src/github.com/status-im/status-react/desktop)

message("SOURCE_ROOT=/home/pedro/src/github.com/status-im/status-react/desktop")
message("CMAKE_CURRENT_BINARY_DIR=/home/pedro/src/github.com/status-im/status-react/desktop")
message(FATAL_ERROR "/home/pedro/.conan/data/qt5/5.11.2/status-im/experimental/package/85e0115c6e1f0b7dca6b515fb6bee3c0773cfd34/gcc_64/bin/windeployqt")
execute_process(COMMAND /home/pedro/.conan/data/qt5/5.11.2/status-im/experimental/package/85e0115c6e1f0b7dca6b515fb6bee3c0773cfd34/gcc_64/bin/windeployqt --verbose 9 --qmldir /home/pedro/src/github.com/status-im/status-react/desktop ${CMAKE_CURRENT_BINARY_DIR}/bin/Status.exe)

# on windows windeployqt doesnt install non qt libraries, let cmake do that
include(BundleUtilities)

foreach(d /home/pedro/.conan/data/qt5/5.11.2/status-im/experimental/package/85e0115c6e1f0b7dca6b515fb6bee3c0773cfd34/gcc_64)
  list(APPEND SEARCHDIRS ${d}/bin)
  list(APPEND SEARCHDIRS ${d}/lib)
endforeach()

fixup_bundle("${CMAKE_INSTALL_PREFIX}/Status.exe" "" "${SEARCHDIRS}") 
#install(PROGRAMS ${VCREDIST_DIR}/vc_redist_2015_x64.exe DESTINATION .)

# if()
#   file(GLOB_RECURSE EXES ${CMAKE_INSTALL_PREFIX}/*.exe ${CMAKE_INSTALL_PREFIX}/*.dll)
#   set(ENV{errorlevel} 1)
#   foreach(e ${EXES})
#     message("-- Signing: ${PROJECT_SOURCE_DIR}/scripts/WindowsSign.cmd ${e}")
#     execute_process(COMMAND ${PROJECT_SOURCE_DIR}/scripts/WindowsSign.cmd "${e}" RESULT_VARIABLE RES)
#   endforeach()
# endif()
