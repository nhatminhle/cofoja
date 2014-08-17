# Cofoja

Contracts for Java, or Cofoja for short, is a contract programming
framework and test tool for Java, which uses annotation processing and
bytecode instrumentation to provide run-time checking. (In particular,
this is not a static analysis tool.)

I originally wrote Cofoja when interning at Google in 2010, based on
prior work on Modern Jass. It was open-sourced in early 2011, and has
since been maintained by myself, with the help of a small community.


## Download

Pre-built JAR files are available on the GitHub release page:
https://github.com/nhatminhle/cofoja/releases

Each release comes in four flavours. `cofoja` is the basic version,
which contains every feature Cofoja has to offer. `+asm` builds are
bundled with a compatible version of the ASM library. `+contracts` are
debug builds that include self-check contracts on the annotation
processor and library themselves.

If you are confused about which version to choose, pick either the
plain `cofoja` JAR file if you already have the ASM library installed,
or plan to install it by other means, or the `cofoja+asm` JAR file if
you want a single JAR file that works out of the box.

The class files are compiled for Java 6. Cofoja itself depends on
features not available to older versions of Java.


## Build

### Dependencies

Building Cofoja requires:

* JDK 6 or higher, for annotation processing and bytecode
  instrumentation.
* ASM 5.x (or higher versions with ASM5-compatible API), for bytecode
  instrumentation. http://asm.ow2.org
* Ant 1.9.1 or higher for the build script.

In order to enable contract checking at run time, Cofoja binaries and
dependencies are needed. Normal execution (with contracts disabled)
does not require Cofoja or any of its dependencies.


### Configuration

The build script reads properties from the user-provided
`local.properties` file. See `default.properties` for a list of
configuration options to adjust to fit your build environment. Most
importantly, paths to the required JAR files must be provided.


### Ant targets

The default target, `dist`, builds all four release JAR files. To
include only those without self-contracts, execute the `nobootstrap`
target.

By default, the produced JAR files are placed in the `dist` folder, in
the current directory.

You should run the `test` target to check that your build of Cofoja
behaves (somewhat) as expected.


## Usage

Cofoja supports a contract model similar to that of Eiffel, with added
support for a few Java-specific things, such as exceptions.

Some general understanding of contract programming (also called design
by contract) is required to use Cofoja effectively. If you have no
idea what this is, Wikipedia may be a good place to start:
http://en.wikipedia.org/wiki/Design_by_Contract


### Annotations

In Cofoja, contracts are written as Java code within quoted strings,
embedded in annotations. E.g., `@Requires("x < 100")` states that `x`
must be less than 100. Any Java expression may be used, provided the
string is escaped properly.

An annotation binds a contract to a code element: either a method or
a type. Cofoja defines three main annotation types, which live in the
`com.google.java.contract` package:

* `@Requires` for method preconditions;
* `@Ensures` for method postconditions;
* `@Invariant` for class and interface invariants;

Contract annotations work on both classes and interfaces. For
convenience, arrays of quoted expressions are accepted, and behave as
if the components were separated by `&&`. E.g., `@Ensures({ "x > 0",
"x < 50" })` is equivalent to `@Ensures("x > 0 && x < 50")`.


### Method contracts

A method may have preconditions and postconditions attached to
it. Together, they specify the contract between caller and callee: if
the precondition is satisfied on entry of the method, then the caller
may assume the postcondition on exit. The precondition is what the
callee demands of the caller, and in return the caller expects the
postcondition to hold after the call.

As an example, consider the following specification of the square root
function, which states that for any non-negative double `x` given,
`sqrt` will return a non-negative result.

```java
@Requires("x >= 0")
@Ensures("result >= 0")
static double sqrt(double x);
```

As shown in this example, a precondition may access parameter values;
in fact, preconditions and postconditions are evaluated in the context
of the method they are bound to. More precisely, each annotation
behaves as if it were a method, with the same arguments and in the
same scope as the qualified method. In terms of scoping, the previous
code is equivalent to the following:

```java
static void sqrt_Requires(double x) {
  assert x >= 0;
}
static void sqrt_Ensures(double result) {
  assert result >= 0;
}
static double sqrt(double x);
```

In addition, postconditions may contain a few extensions:

* As we have seen, they may refer to the returned value, using the
  `result` keyword.
* Within a postcondition, `old` is a keyword which is followed by
  a single parenthesized expression, such that `old (x)` evaluates to
  the value of `x` on entry of the method invocation.
* An `old` expression is evaluated in the same context as
  preconditions and has access to the same things, including parameter
  values.

Given this, a more complete specification of `sqrt` might be:

```java
@Requires("x >= 0")
@Ensures({ "result >= 0", "Math.abs(x - result * result) < EPS" })
static double sqrt(double x);
```

At run time, when contracts are enabled, preconditions and
postconditions translate to checks on entry and exit, respectively, of
the method. A failure results in a `PreconditionError` or
`PostconditionError` being thrown, depending on the origin: failure to
meet a precondition means that the method was called incorrectly,
whereas an unsatisfied postcondition points to a bug in the
implementation of the method itself.


### Class and interface contracts

A class or interface may have associated invariants. Instead of
specifying a contract between a caller and a callee, those invariants
describe the state of a valid object of the qualified type. Calling
methods on an object may cause it to change; invariants guarantee that
after any such change, the object remains in a consistent state.

Of course, internal operations are allowed to muck around and
temporarily invalidate invariants to do their job, but they agree to
eventually put everything back into their proper places. Intuitively,
any operation made against `this` is considered internal and does not
need to obey the invariants. Only method invocations on other
variables do.

In Cofoja, when contracts are enabled, invariants are checked on entry
and exit of method calls on objects not already in the call stack
(including `this`). Failure results in an `InvariantError` exception
and indicates that the guilty method has left the object in an
inconsistent state.


### Inheritance

Contracts apply to all objects of the associated type, including any
instances of derived classes: all implementations of a contracted
interface must honor the interface contracts, all children of a class
must honor the contracts of their parent.

In addition, derived types may refine those contracts by adding their
own preconditions, postconditions and invariants to the mix. However,
they cannot replace the inherited contracts, only augment them
according to the following subtyping rules.

Preconditions may be weakened, i.e., methods may be overriden with
implementations that accept a wider range of inputs. Callers that
access the object through a superclass or interface need only
establish the parent contracts. In Cofoja, a method checks that either
its inherited *or* its own preconditions are satisfied.

Postconditions may be strengthened, i.e., methods may be overriden
with implementations that produce a smaller range of outputs. Callers
that access the object through a superclass or interface expect at
least the parent contracts. In Cofoja, a method checks both its
inherited *and* its own postconditions.

Invariants may be strengthened, i.e., classes and interfaces may be
derived to restrict the set of valid states. An object must qualify as
a consistent value of any of its superclasses or interfaces. In
Cofoja, a type asserts both its inherited *and* its own invariants.


### Other features

#### Import annotations

Previously, we have used `Math.abs` in our examples, which resides in
`java.lang`, and is thus available to any Java code. For other
symbols, we may need to import a class or package. Imports for
contracts are kept separately from those of the main file. The
`@ContractImport` annotation specifies names to be imported for use in
contract code within the associated type. It must be put on the
enclosing type and accepts an array of strings, each one containing an
import pattern, compatible with the `import` Java statement.

#### Constructors

With respect to Cofoja, constructors behave slightly differently from
other methods. They assert invariants, but only on exit, and may be
attached to preconditions and postconditions. However, a very
important distinction is that constructor preconditions are checked
*after* any call to a parent constructor. This is due to `super` calls
being necessarily the first instruction of any constructor call,
making it impossible to insert precondition checks before them. (This
is considered a known bug.)

#### Exception handling

When an exception is thrown from within a contracted method, normal
postconditions are not checked by Cofoja, as they may refer to
a result that does not exist.

Instead, the `@ThrowEnsures` annotation is provided. It functions
similarly to `@Ensures` but accepts an alternating list of exception
type names to catch and matching postcondition expressions, as quoted
strings. Within those exceptional postconditions, the keyword `signal`
refers to the exception object that has been thrown.

A possible use of `@ThrowEnsures` is to guarantee that exceptional
method exits do not inadvertently alter the state of the object.

```java
class RestrictedInteger {
  int x;

  @Ensures("x == y")
  @ThrowEnsures({ "IllegalArgumentException", "x == old (x)" })
  void setX(int y) throws IllegalArgumentException {
    ...
  }
}
```


### Invocation

Contracts for Java consists of an annotation processor, an
instrumentation agent, as well as an offline bytecode rewriter. The
annotation processor compiles annotations into separate contract class
files. The instrumentation agent weaves these contract files with the
real classes before they are loaded into the JVM. Alternatively, the
offline bytecode rewriter can be used to produce pre-weaved class
files that can be directly deployed without requiring the Java agent
or ASM library at run time.

The pre-built JAR files contain both the Java agent and annotation
processor. The latter is named `AnnotationProcessor` (in the
`com.google.java.contract.core.apt` package); the agent can be loaded
directly by specifying the JAR file (e.g., as an argument to the
`-javaagent` argument).

To execute code compiled with contract checking enabled, make sure the
generated files (additional `.class` and `.contracts` files) are in
your class path and enable the Cofoja JAR file as a Java agent.

Or use the offline instrumenter, which lives in the `PreMain` class,
under the `com.google.java.contract.core.agent` package, and takes
paths to class files as command-line arguments.


### Run-time contract configuration

#### Selective contracts

When using the Java agent, contract evaluation can be enabled
selectively, similar to how assertions can be toggled on and off for
specific types. Whether contracts are checked on methods of a given
type is determined at load time and may not be changed afterwards.

These settings are controlled through a user-defined configurator
object. As part of its early start-up procedure, the Java agent
attempts to create an instance of the configurator class, whose name
is provided through the `com.google.java.contract.configurator` JVM
property. It then calls the `configure` method on the newly created
object, passing it an argument of type `ContractEnvironment` (from
package `com.google.java.contract`).

The `ContractEnvironment` object provides methods such as
`enablePreconditions` and `disablePreconditions`, to enable or disable
contracts selectively. Each method accepts an import-like string
pattern. In case of pattern overlap, the behavior specified by the
last matching call is retained.

Disabling contracts for a specific type does not prevent its contracts
from being inherited and checked correctly for the derived types.

#### Blacklist

The blacklist is controlled through the `ContractEnvironment` methods
`ignore` and `unignore`, which also take patterns, similarly to the
contract selection methods.

Ignoring a pattern is different from disabling contracts for that
pattern. A blacklisted type is not be examined at all by the Java
agent; in particular, it is not searched for contracts and its
bytecode is not modified in any way. It is assumed to contain no
contracts; thus, derived types inherit nothing from it.

#### Debug tracing

For debug purposes, Cofoja may be instructed to print a trace to
stderr of contract that are evaluated. Compilation support for tracing
is provided by the `com.google.java.contract.debug` annotation
processor flag. The actual trace is obtained by running the contracted
program with the `com.google.java.contract.log.contract` JVM property
set to `true`.


### Quick reference

#### Annotations

All annotations reside in the `com.google.java.contract` package.

Annotation      | Checked on            | Inheritance
--------------- | --------------------- | -----------
`@Invariant`    | Object entry and exit | And
`@Requires`     | Entry                 | Or
`@Ensures`      | Normal exit           | And
`@ThrowEnsures` | Abnormal exit         | And

#### Keywords

Keyword  | May appear in               | Description
-------- | --------------------------- | ---------------------
`old`    | `@Ensures`, `@ThrowEnsures` | Value on method entry
`result` | `@Ensures`                  | Value to be returned
`signal` | `@ThrowEnsures`             | Exception thrown

#### Annotation processor options

All option names reside in the `com.google.java.contract` name
space. Options that have a Javac counterpart are passed down to the
underlying Java compiler used to compile contract code. In most cases,
they should match those used to compile the main project.

Option        | Type      | Javac option  | Description
------------- | --------- | ------------- | ------------------------------------
`classpath`   | path      | `-classpath`  | Class path for contract code
`sourcepath`  | path      | `-sourcepath` | Where to find source files
`classoutput` | directory | `-d`          | Where to put compiled contract files
`debug`       | flag      |               | Enable run-time logging support
`dump`        | directory |               | Where to put generated source files

Additionally, you may want to pass `-proc:only` (or equivalent) to the
Java compiler, so it only runs the annotation processor, which will
generate compiled contract files without producing the normal class
files. This is recommended for medium-to-large projects.

#### Java agent properties

All properties reside in the `com.google.java.contract` name space.

Property       | Type    | Description
-------------- | ------- | ----------------------------------------------
`configurator` | String  | Configurator class name
`dump`         | String  | Where to dump instrumented class files
`log.contract` | Boolean | Print a trace of evaluated contracts to stderr

`log.contract` requires contracts compiled with the `debug` annotation
processor option.

#### Run-time contract configuration methods

All methods live in `com.google.java.contract.ContractEnvironment`.

Method                  | Description
----------------------- | ---------------------------------------------------
`enablePreconditions`   | Check preconditions for all methods of class
`disablePreconditions`  | Do not check preconditions for any method of class
`enablePostconditions`  | Check postconditions for all methods of class
`disablePostconditions` | Do not check postconditions for any method of class
`enableInvariants`      | Check invariants for class
`disableInvariants`     | Do not check invariants for class
`ignore`                | Do not search class for contracts
`unignore`              | Search class for contracts


## Bugs

Contracts for Java is a not-so-young project. Please help make it
better by reporting bugs at:
https://github.com/nhatminhle/cofoja/issues
