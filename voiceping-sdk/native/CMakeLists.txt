cmake_minimum_required(VERSION 3.22)
project(myopus C)

# Match the shipped libmyopus.so: fixed-point Opus, statically linked into a
# single self-contained JNI shared library.
set(OPUS_FIXED_POINT ON CACHE BOOL "" FORCE)
set(OPUS_BUILD_SHARED_LIBRARY OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory(${OPUS_DIR} opus_build)

add_library(myopus SHARED opus_jni.c)
target_include_directories(myopus PRIVATE ${OPUS_DIR}/include)
target_link_libraries(myopus PRIVATE opus)

# 16 KB page-size support: force ELF PT_LOAD segments to 16 KB alignment so the
# library loads on devices with 16 KB memory pages (Android 15+ / Google Play).
target_link_options(myopus PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384")
