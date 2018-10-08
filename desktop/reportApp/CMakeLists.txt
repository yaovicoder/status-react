# Copyright (c) 2017-present, Status Research and Development GmbH.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DUNICODE -std=c++11")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUNICODE -std=c11")

set(APP_NAME "reportApp")

set(MAIN_CPP_SOURCE reportpublisher.cpp
                    reportpublisher.cpp
                    main.cpp)

# Set some Win32 Specific Settings
IF(WIN32)
  SET(GUI_TYPE WIN32)
ENDIF(WIN32)
# Set some Apple MacOS Specific settings
IF (APPLE)
  SET(GUI_TYPE MACOSX_BUNDLE)
ENDIF (APPLE)

add_executable(
  ${APP_NAME}
  ${GUI_TYPE}
  ${MAIN_CPP_SOURCE}
  main.qrc
)

message("USED_QT_MODULES=${USED_QT_MODULES}")
#qt5_use_modules(${APP_NAME} ${USED_QT_MODULES})
# target_link_libraries(${APP_NAME}
#                       ${USED_QT_MODULES})
target_link_libraries(${APP_NAME}
                      PRIVATE ${QT5_LIBRARIES})

set(REACT_NATIVE_DESKTOP_EXTERNAL_PROJECT_DEPS ${REACT_NATIVE_DESKTOP_EXTERNAL_PROJECT_DEPS} reportApp PARENT_SCOPE)

#set(SOURCE_ROOT ${CMAKE_SOURCE_DIR})
include(CompleteBundle)
