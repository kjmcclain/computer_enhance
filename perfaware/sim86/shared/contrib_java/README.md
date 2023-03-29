# Java Wrapper for 8086 Simulator

## About

This wrapper uses JNI to call into the 8086 simulator.  When the JVM
loads a dynamic library, it binds native methods to properly named
symbols in the library.  For instance, the native method `mnemonic` in
class `com.computerenhance.sim86.Decoder` is bound to the symbol
`Java_com_computerenhance_sim86_Decoder_mnemonic` if found in the
dynamic library.  Moreover, this symbol should be a function of a very
specific signature that involves a number of JNI objects that allow
the C/C++ code to interface with the Java objects.  All this means JNI
can't interface with arbitrary dynamic libraries, but instead requires
glue code written in C/C++ to serve as binding between native methods
and the libraries themselves.  This glue code is in
`c/sim86_lib_java.c`.

## Directory Layout

- `lib` A directory of pre-built dynamic libraries.

- `sim86.jar` The jar file that loads the glue dynamic library, and
  provides an interface to it.

- `c` The C-code for the glue dynamic library
- `src` The source code for `sim86.jar`
- `test` The test code for testing `sim86.jar`
- `out/production/contrib_java` Compiler output for `src`
- `out/test/contrib_java` Compiler output for `test`

## Running

If you wish to try using the pre-built dynamic libraries, you should
be able to run your code using just them and `sim86.jar`.  All you
need to do is tell the java process what directory to look at to find
the dynamic libraries, and include sim86.jar in your dependencies.
The dynamic library search path is specified with
`-Djava.library.path`.

For instance, to run the test code from this directory, run:

```
java -cp out/test/contrib_java:sim86.jar \
     -Djava.library.path=lib \
     com.computerenhance.sim86.DecoderTest
```

Alternately, you can just tell the `Decoder` class exactly which glue
dynamic library file to load with `-Dsim86java=path/to/libsim86java.so`

```
java -cp out/test/contrib_java:sim86.jar \
     -Dsim86java=lib/libsim86java.so \
     com.computerenhance.sim86.DecoderTest
```

## Building

If the pre-built dynamic libraries don't work for you, or you're on
windows, you'll need to build them yourselves.

Building is only support on mac and linux, but if you look at
`Build.java`, you should be able to fill in the steps for windows.
Java 17 is assumed.

To build, run:

```
java Build.java
```

First, this will compile the java code from `src` into
`out/production/contrib_java`, and then package it as a jar file
`sim86.jar`.  It'll also compile the test code as well.  (This should
work on all platforms.)

Second, it will attempt to build both the shared library `libsim86.so`
from `../../sim86_lib.cpp` and glue shared library `libsim86java.so` from
`c/sim86_lib_java.c`.  On Mac, this uses `clang` and on linux, it uses
both `gcc` and `clang` since `../sim86_lib.h` uses c23 features that
gcc doesn't implement, but clang seems to be okay with.

