# Changes in version 2.17.0 - 2021-06-14

 * Medium changes
   - Detect GeoIP files automatically by filename.

 * Minor changes
   - Update to Apache Commons Logging 1.1.3.


# Changes in version 2.16.0 - 2021-05-02

 * Medium changes
   - Parse new NAT-based Snowflake lines.
   - Added a new parser for the IP Fire GeoIP database files used in core Tor.
   - Tests now depend on objenesis 2.2 (previously 2.1).
 * Minor changes
   - A lib folder is now included in the source distribution and does not need
     to be created prior to using the ant resolve task.


# Changes in version 2.15.0 - 2020-12-11

 * Medium changes
   - Parse version 3 onion service statistics contained in extra-info
     descriptors.
   - Optimize parsing of large files containing many descriptors.

 * Minor changes
   - Provide microdescriptor SHA-256 digest in hexadecimal encoding.


# Changes in version 2.14.0 - 2020-08-07

 * Medium changes
   - Extend Torperf results to provide error codes.
   - Parse OnionPerf analysis results format version 3.0.
   - Parse new ipv6-{write,read}-history and ipv6-conn-bi-direct
     lines in extra-info descriptors.


# Changes in version 2.13.0 - 2020-05-16

 * Medium changes
   - Parse OnionPerf analysis results format version 2.0.
   - Extend Torperf results to provide partial download times for 10,
     20, 50, 100, 200, and 500 KiB as well as 1, 2, and 5 MiB.

 * Minor changes
   - Include previously unknown error codes in Torperf results
     converted from OnionPerf analysis files.


# Changes in version 2.12.2 - 2020-04-30

 * Minor changes
   - Change the order of detecting descriptor types by content and/
     file name.


# Changes in version 2.12.1 - 2020-04-30

 * Minor changes
   - Change back how we treat xz-compressed files by leaving
     decompression to descriptor implementations.


# Changes in version 2.12.0 - 2020-04-30

 * Medium changes
   - Add parsing support for OnionPerf analysis files by converting
     and returning contained transfers as Torperf results.


# Changes in version 2.11.0 - 2020-04-13

 * Medium changes
   - Compute bandwidth file digests.
   - Parse bandwidth file header and bandwidth file digest in votes.
   - Parse bridge distribution requests in bridge server descriptors.
   - Parse authority fingerprint in bridge network statuses.

 * Minor changes
   - Avoid invoking overridable methods from constructors.
   - Make all descriptor instances serializable.
   - Simplify logging configuration.


# Changes in version 2.10.0 - 2020-01-15

 * Medium changes
   - Parse three newly added lines in snowflake statistics files.

 * Minor changes
   - Fix a NullPointerException when parsing an invalid crypto block
     starting with "-----END " rather than "-----BEGIN ".
   - Properly parse an authority's hostname from the "dir-source" line
     in a v2 network status.


# Changes in version 2.9.1 - 2019-11-09

 * Minor changes
   - Do not fail when processing index.json files with unknown fields.


# Changes in version 2.9.0 - 2019-11-01

 * Medium changes
   - Make NetworkStatusEntryImpl#parseSLine thread-safe.


# Changes in version 2.8.0 - 2019-10-18

 * Medium changes
   - Extend DescriptorReader#readDescriptors to support .gz-compressed
     files.
   - Add new BridgedbMetrics descriptor type.


# Changes in version 2.7.0 - 2019-09-06

 * Medium changes
   - Use Ivy for resolving external dependencies rather than relying
     on files found in Debian stable packages. Requires installing Ivy
     (using `apt-get install ivy`, `brew install ivy`, or similar) and
     running `ant resolve` (or `ant -lib /usr/share/java resolve`).
     Retrieved files are then copied to the `lib/` directory, except
     for dependencies on other metrics libraries that still need to be
     copied to the `lib/` directory manually. Current dependency
     versions resolved by Ivy are the same as in Debian stretch with
     few exceptions.
   - Remove Cobertura from the build process.
   - Add new SnowflakeStats descriptor type.


# Changes in version 2.6.2 - 2019-05-29

 * Medium changes
   - Recognize bandwidth files with @type annotation.


# Changes in version 2.6.1 - 2019-05-03

 * Medium changes
   - Fix a bug in recognizing descriptors as bandwidth files.


# Changes in version 2.6.0 - 2019-04-29

 * Medium changes
   - Stop signing jar files.
   - Add new BandwidthFile descriptor for parsed bandwidth files.


# Changes in version 2.5.0 - 2018-09-25

 * Medium changes
   - Go back to using Apache Commons Codec as base64 and hexadecimal
     codec rather than using JAXB which won't be available anymore
     after upgrading from Java 8 to 9. Applications must provide
     Apache Commons Codec 1.10 as dependency.

 * Minor changes
   - Make DescriptorCollector resume previously aborted downloads.


# Changes in version 2.4.0 - 2018-05-23

 * Medium changes
   - Replace Gson with Jackson. Applications must provide Jackson
     2.8.6 or compatible as dependency and do not need to provide Gson
     as dependency anymore.


# Changes in version 2.3.0 - 2018-04-18

 * Medium changes
   - Replace ServerDescriptor#getHiddenServiceDirVersions with
     ServerDescriptor#isHiddenServiceDir, because Tor has never
     supported versions in the hidden-service-dir descriptor line.
   - Add support for reading web server logs contained in tarballs by
     providing file names of both the tarball and of contained files
     to WebServerAccessLog.

 * Minor changes
   - Override logLines() method from LogDescriptor in
     WebServerAccessLog.
   - Use 1-minute connect and read timeouts for fetching CollecTor's
     index.json.


# Changes in version 2.2.0 - 2018-02-26

 * Major changes
   - Add new descriptor type WebServerAccessLog to parse web server
     access logs.

 * Minor changes
   - Add log message reporting progress reading tarballs.


# Changes in version 2.1.1 - 2017-10-09

 * Minor changes
   - Add new optional "build_revision" field to index.json with the
     Git revision supplied by the calling software.


# Changes in version 2.1.0 - 2017-09-15

 * Major changes
   - Update dependencies to versions available in Debian stretch:
     Apache Commons Compress 1.13, Apache Commons Lang 3.5, Google
     Gson 2.4, JUnit 4.12, Logback 1.1.9, SLF4J 1.7.22, and XZ 1.6.
   - Update to Java 8.

 * Minor changes
   - Retain trailing newline in Torperf results.


# Changes in version 2.0.0 - 2017-06-28

 * Major changes
   - Always use UTF-8 as charset rather than using the platform's
     default charset.
   - Remove all code that was deprecated in 1.x versions.
   - Remove previously non-deprecated but internally unused
     parseDescriptors(byte[], String)) method that returned a List and
     threw DescriptorParseException.

 * Minor changes
   - Replace custom ImplementationNotAccessibleException thrown by
     DescriptorSourceFactory with generic RuntimeException.
   - Rename jar files and release tarball to start with "metrics-lib".


# Changes in version 1.9.0 - 2017-06-21

 * Major changes
   - Simplify DescriptorReader by returning Descriptor instances
     rather than DescriptorFile instances containing Descriptors,
     deprecate DescriptorFile, and add a File reference to Descriptor.
   - Introduce a new UnparseableDescriptor to be returned by
     DescriptorParser and DescriptorReader if a descriptor cannot be
     parsed, as opposed to throwing a DescriptorParseException or
     skipping the entire descriptor file, respectively.

 * Medium changes
   - Let DescriptorParser return an Iterable instead of a List, which
     prepares parsing large descriptor files descriptor by descriptor.
   - Add new method to retrieve the raw descriptor length, rather than
     forcing applications to request (a copy of) raw descriptor bytes
     only to determine the raw descriptor length.

 * Minor changes
   - Fix a bug where NetworkStatusEntry's getMicrodescriptorDigests()
     and getMicrodescriptorDigestsSha256Base64() return hex strings
     rather than base64 strings.


# Changes in version 1.8.2 - 2017-06-16

 * Medium changes
   - Fix a regression where any DescriptorParseException thrown while
     parsing a descriptor is encapsulated and rethrown as
     RuntimeException.


# Changes in version 1.8.1 - 2017-06-08

 * Medium changes
   - Fix a regression in parsing microdescriptors and version 1
     directories introduced in 1.8.0.


# Changes in version 1.8.0 - 2017-06-07

 * Medium changes
   - Store raw descriptor contents as offset and length into a
     referenced byte[], rather than copying contents into a separate
     byte[] per descriptor.

 * Minor changes
   - Turn keyword strings into enums and use the appropriate enum sets
     and maps to avoid repeating string literals and to use more speedy
     collection types.
   - Simplify and avoid repetition in parse helper methods.
   - Fix a bug where Microdescriptor's getDigestSha256Base64() returns
     a hex string rather than a base64 string.
   - Move descriptor digest computation to DescriptorImpl.
   - Fix a bug in digest computation by making sure that the
     descriptor string actually contains the end token.
   - Fix a bug where both RelayDirectoryImpl and all NetworkStatusImpl
     subclasses fail to get indexes right if parts of raw descriptor
     strings contain non-ASCII chars.  In practice, this only affects
     version 1 directories which were last archived in 2007.


# Changes in version 1.7.0 - 2017-05-17

 * Medium changes
   - Fix a bug where unrecognized lines in extra-info descriptors
     below crypto blocks were silently skipped.
   - Add support for six new key-value pairs added by OnionPerf.
   - Fix a bug where DescriptorIndexCollector would not delete
     extraneous local files if remote paths start with /.
   - Add previously missing method to obtain the digest of a vote.
   - Deprecate setFailUnrecognizedDescriptorLines() in
     DescriptorParser and DescriptorReader and refer to
     getUnrecognizedLines() in Descriptor if applications really need
     to fail descriptors containing unrecognized lines.
   - Parse "padding-counts" lines in extra-info descriptors.

 * Minor changes
   - Accept extra arguments in statistics-related extra-info
     descriptor lines, as permitted by dir-spec.txt.
   - Streamline digest method names.


# Changes in version 1.6.0 - 2017-02-17

 * Major changes
   - Deprecate DescriptorDownloader in favor of the much more widely
     used DescriptorCollector.

 * Medium changes
   - Add two methods for loading and saving a parse history file in
     the descriptor reader to avoid situations where applications fail
     after all descriptors are read but before they are all processed.
   - Unify the build process by adding git-submodule metrics-base in
     src/build and removing all centralized parts of the build
     process.
   - Avoid deleting extraneous local descriptor files when collecting
     descriptors from CollecTor.
   - Turn the descriptor reader thread into a daemon thread, so that
     the application can decide at any time to stop consuming
     descriptors without having to worry about the reader thread not
     being done.
   - Parse "proto" lines in server descriptors, "pr" lines in status
     entries, and "(recommended|required)-(client|relay)-protocols"
     lines in consensuses and votes.
   - Parse "shared-rand-.*" lines in consensuses and votes.
   - Deprecate DescriptorCollectorImpl now that
     DescriptorIndexCollector is the default.


# Changes in version 1.5.0 - 2016-10-19

 * Major changes
   - Make the DescriptorCollector implementation that uses CollecTor's
     index.json file to determine which descriptor files to fetch the
     new default.  Applications must provide gson-2.2.4.jar or higher
     as dependency.

 * Minor changes
   - Avoid running into an IOException and logging a warning for it.


# Changes in version 1.4.0 - 2016-08-31

 * Major changes
   - Add the Simple Logging Facade for Java (slf4j) for logging
     support rather than printing warnings to stderr.  Applications
     must provide slf4j-api-1.7.7.jar or higher as dependency and can
     optionally provide a compatible logging framework of their choice
     (java.util.logging, logback, log4j).

 * Medium changes
   - Add an alpha version of a DescriptorCollector implementation that
     is not enabled by default and that uses CollecTor's index.json
     file to determine which descriptor files to fetch.  Applications
     can enable this implementation by providing gson-2.2.4.jar or
     higher as dependency and setting property descriptor.collector to
     org.torproject.descriptor.index.DescriptorIndexCollector.

 * Minor changes
   - Include resource files in src/*/resources/ in the release
     tarball.
   - Move executable, source, and javadoc jar to generated/dist/.


# Changes in version 1.3.1 - 2016-08-01

 * Medium changes
   - Adapt to CollecTor's new date format to make DescriptorCollector
     work again.


# Changes in version 1.3.0 - 2016-07-06

 * Medium changes
   - Parse "package" lines in consensuses and votes.
   - Support more than one "directory-signature" line in a vote, which
     may become relevant when authorities start signing votes using
     more than one algorithm.
   - Provide directory signatures in consensuses and votes in a list
     rather than a map to support multiple signatures made using the
     same identity key digest but different algorithms.
   - Be more lenient about digest lengths in directory signatures
     which may be longer or shorter than 20 bytes.
   - Parse "tunnelled-dir-server" lines in server descriptors.

 * Minor changes
   - Stop reporting "-----END .*-----" lines in v2 network statuses as
     unrecognized.


# Changes in version 1.2.0 - 2016-05-31

 * Medium changes
   - Include the hostname in directory source entries of consensuses
     and votes.
   - Also accept \r\n as newline in Torperf results files.
   - Make unrecognized keys of Torperf results available together with
     the corresponding values, rather than just the whole line.
   - In Torperf results, recognize all percentiles of expected bytes
     read for 0 <= x <= 100 rather than just x = { 10, 20, ..., 90 }.
   - Rename properties for overriding default descriptor source
     implementation classes.
   - Actually return the signing key digest in network status votes.
   - Parse crypto parts in network status votes.
   - Document all public parts in org.torproject.descriptor and add
     an Ant target to generate Javadocs.

 * Minor changes
   - Include a Torperf results line with more than one unrecognized
     key only once in the unrecognized lines.
   - Make "consensus-methods" line optional in network statuses votes,
     which would mean that only method 1 is supported.
   - Stop reporting "-----END .*-----" lines in directory key
     certificates as unrecognized.
   - Add code used for benchmarking.


# Changes in version 1.1.0 - 2015-12-28

 * Medium changes
   - Parse flag thresholds in bridge network statuses, and parse the
     "ignoring-advertised-bws" flag threshold in relay network status
     votes.
   - Support parsing of .xz-compressed tarballs using Apache Commons
     Compress and XZ for Java.  Applications only need to add XZ for
     Java as dependency if they want to parse .xz-compressed tarballs.
   - Introduce a new ExitList.Entry type for exit list entries instead
     of the ExitListEntry type which is now deprecated.  The main
     difference between the two is that ExitList.Entry can hold more
     than one exit address and scan time which were previously parsed
     as multiple ExitListEntry instances.
   - Introduce four new types to distinguish between relay and bridge
     descriptors: RelayServerDescriptor, RelayExtraInfoDescriptor,
     BridgeServerDescriptor, and BridgeExtraInfoDescriptor.  The
     existing types, ServerDescriptor and ExtraInfoDescriptor, are
     still usable and will not be deprecated, because applications may
     not care whether a relay or a bridge published a descriptor.
   - Support Ed25519 certificates, Ed25519 master keys, SHA-256
     digests, and Ed25519 signatures thereof in server descriptors and
     extra-info descriptors, and support Ed25519 master keys in votes.
   - Include RSA-1024 signatures of SHA-1 digests of extra-info
     descriptors, which were parsed and discarded before.
   - Support hidden-service statistics in extra-info descriptors.
   - Support onion-key and ntor-onion-key cross certificates in server
     descriptors.

 * Minor changes
   - Start using Java 7 features like the diamond operator and switch
     on String, and use StringBuilder correctly in many places.


# Changes in version 1.0.0 - 2015-12-05

 * Major changes
   - This is the initial release after four years of development.
     Happy 4th birthday!

