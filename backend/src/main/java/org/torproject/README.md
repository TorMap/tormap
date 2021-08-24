DescripTor -- A Tor Descriptor API for Java
===========================================

DescripTor is a Java API that fetches Tor descriptors from a variety of
sources like cached descriptors and directory authorities/mirrors.
The DescripTor API is useful to support statistical analysis of the Tor
network data and for building services and applications.

The descriptor types supported by DescripTor include relay and bridge
descriptors which are part of Tor's directory protocol as well as Torperf
data files and TorDNSEL's exit lists.  Access to these descriptors is
unified to facilitate access to publicly available data about the Tor
network.

This API is designed for Java programs that process Tor descriptors in
batches.  A Java program using this API first sets up a descriptor source
by defining where to find descriptors and which descriptors it considers
relevant.  The descriptor source then makes the descriptors available in a
descriptor store.  The program can then query the descriptor store for the
contained descriptors.  Changes to the descriptor sources after
descriptors are made available in the descriptor store will not be
noticed.  This simple programming model was designed for periodically
running, batch-processing applications and not for continuously running
applications that rely on learning about changes to an underlying
descriptor source.

The executable jar, source jar, and javadoc jar can be found in

```
generated/dist/
```

Before using them please verify the release (see below for instructions).


Verifying releases
------------------

Releases can be cryptographically verified to get some more confidence that
they were put together by a Tor developer.  The following steps explain the
verification process by example.

Download the release tarball and the separate signature file:

```
wget https://dist.torproject.org/metrics-lib/2.0.0/metrics-lib-2.0.0.tar.gz
wget https://dist.torproject.org/metrics-lib/2.0.0/metrics-lib-2.0.0.tar.gz.asc
```

(Note that earlier tarballs were named descriptor-VERSION.tar.gz and could
be found in https://dist.torproject.org/descriptor/.)

Attempt to verify the signature on the tarball:

```
gpg --verify metrics-lib-2.0.0.tar.gz.asc
```

If the signature cannot be verified due to the public key of the signer
not being locally available, download that public key from one of the key
servers and retry:

```
gpg --keyserver pgp.mit.edu --recv-key 0x4EFD4FDC3F46D41E
gpg --verify metrics-lib-2.0.0.tar.gz.asc
```

If the signature still cannot be verified, something is wrong!

But note that even if it can be verified, you now only know that the
signature was made by the person claiming to own this key, which could be
anyone.  You'll need a trust path to the owner of this key in order to
trust this signature, but that's clearly out of scope here.  In short,
your best chance is to meet a Tor developer in real life and enter the web
of trust.

If you want to go one step further in the verification game, you can
verify the signature on the .jar files.

Print and then import the provided X.509 certificate:

```
keytool -printcert -file CERT
keytool -importcert -alias karsten -file CERT
```

Verify the signatures on the contained .jar files using Java's jarsigner
tool:

```
jarsigner -verify metrics-lib-2.0.0.jar
jarsigner -verify metrics-lib-2.0.0-sources.jar
```


Tutorial
--------

The Metrics website has a tutorial for getting started with metrics-lib:

https://metrics.torproject.org/metrics-lib.html

The examples explained in the tutorials are available as source code in

```
src/main/resources/examples/
```


Development
-----------

To contribute to metrics-lib, check out the Java survival guide first:
https://gitlab.torproject.org/tpo/network-health/team/-/wikis/metrics/Java

Start by checking out the project from Git.
The source code from gitlab.torproject.org and can be cloned via HTTPS or SSH
depending on the access you have. If you are doing this manually, ensure you
perform a recursive clone as metrics-base is included in the repository as a
submodule.

The tests, builds and other common development tasks are primarily
performed through calling Ant targets. Ant is also used to fetch dependencies.

Run:
```
$ ant resolve
```

To fetch needed dependencies. Make sure you have [ivy](https://ant.apache.org/ivy/)
configured and installed under:
```
-/usr/share/ant/lib
-/home/hiro/.ant/lib
```
