# chipsallinace-playground

## Introduction
This is a template repository for those who want to develop RTL based on rocket-chip and even chipyard, being able to edit all sources from chisel environments without publish them to local ivy.
You can add your own submodule in `build.sc`.  
For more information please visit [mill documentation](https://com-lihaoyi.github.io/mill/page/configuring-mill.html)
after adding your own code, you can add your library to playground dependency, and re-index Intellij to add your own library.

## IDE support
For mill use
```bash
mill mill.bsp.BSP/install
```
then open by your favorite IDE, which supports [BSP](https://build-server-protocol.github.io/) 

## Pending PRs
Philosophy of this repository is **fast break and fast fix**.
This repository always tracks remote developing branches, it may need some patches to work, `make patch` will append below in sequence:
<!-- BEGIN-PATCH -->
barstools https://github.com/ucb-bar/barstools/pull/101  
chisel3 https://github.com/chipsalliance/chisel3/pull/1903  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2810  
firesim https://github.com/firesim/firesim/pull/747  
firesim https://github.com/firesim/firesim/pull/749  
firesim https://github.com/firesim/firesim/pull/750  
firesim https://github.com/firesim/firesim/pull/761  
hwacha https://github.com/ucb-bar/hwacha/pull/30  
testchipip https://github.com/ucb-bar/testchipip/pull/126  
<!-- END-PATCH -->

## Why not Chipyard

1. Building Chisel and FIRRTL from sources, get rid of any version issue. You can view Chisel/FIRRTL source codes from IDEA.
1. No more make+sbt: Scala dependencies are managed by mill -> bsp -> IDEA, minimal IDEA indexing time.
1. flatten git submodule in dependency, get rid of submodule recursive update.

So generally, this repo is the fast and cleanest way to start your Chisel project codebase.

## Always keep update-to-date
You can use this template and start your own job by appending commits on it. GitHub Action will automatically bump all dependencies, you can merge or rebase `sequencer/master` to your branch.

## System Dependencies
Currently, only support **Arch Linux and Debian** sid, you can PR your own distributions, like Mac Homebrew, Fedora.  
**Notice Ubuntu and CentOS is unacceptable, since they have a stale package repository, not possible use official package manager to install these requirements, if you insist using them, please install requirements below by your self.**
* GNU Make
  - Arch Linux: make
  - Debian: make
* git
  - Arch Linux: git
  - Debian: git
* mill
  - Arch Linux: mill
## SanityTests
This package is the standalone tests to check is bumping correct or not, served as the unittest, this also can be a great example to illustrate usages.

**NOTICE: SanityTests also contains additional system dependencies:**
* python: execute `vlsi_mem_gen`
  - Arch Linux: python
  - Debian: python3
* clang: bootrom cross compiling and veriltor C++ -> binary compiling
  - Arch Linux: clang
  - Debian: clang
* llvm: gnu toolchain replacement 
  - Arch Linux: llvm
  - Debian: llvm
* lld: LLVM based linker
  - Arch Linux: lld
  - Debian: lld
* verilator -> Verilog -> C++ generation
  - Arch Linux: verilator
  - Debian: verilator
* cmake -> verilator emulator build system
  - Arch Linux: cmake
  - Debian: cmake
* ninja -> verilator emulator build system
  - Arch Linux: ninja
  - Debian: ninja-build
* dtc -> dependent by spike
  - Arch Linux: dtc
  - Debian: device-tree-compiler
