# JLoop - A Java Looper

A simple Java Looper implementation / example. Only depends on [SLF4J](https://www.slf4j.org) for a 
logging facade.

# Usage

## Build from source

`#> mvn verify`

## Include in build

Quickest way is by using [JitPack](https://jitpack.io/) (Also see for instructions on gradle usage)

Add JitPack repository

```$xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add dependency

```$xml
<dependency>
    <groupId>com.github.ivovandongen</groupId>
    <artifactId>jloop</artifactId>
    <version>master</version>
</dependency>
```

## Main Looper

A main looper can be created once and can be accessed statically from any other thread for usage after.
For a more complete reference see the [Unit Test](src/test/java/nl/ivovandongen/jloop/LooperTest.java)

On the main thread:
```$Java

Looper looper = Looper.prepareMainLooper();
looper.run() // Will block indefinitely

```

On any other thread:
```Java
Looper.prepareMainLooper().post(() -> {
    // Do what must be done on the main thread 
});

```

Other methods available are `Looper#postDelayed` and `Looper#ask()`

## "Regular" Looper

For other use cases, a regular looper can be prepared on a Thread. Reference to this looper must be handed
to other threads manually:

```$Java

Looper looper = Looper.prepare();
looper.run() // Will block indefinitely

```

## LooperThread

A `LooperThread` is a convenient way to create a `Looper` on a new, separate `Thread`. See the 
[Unit Tests](src/test/java/nl/ivovandongen/jloop/LooperThreadTest.java) for more details.

```$Java
LooperThread lp = new LooperThread();
lp.start(); // Will block until backing Looper is ready

```