# playground

## Introduction
This is a template repository for those who want to develop RTL based on rocket-chip and even chipyard, being able to edit all sources from chisel environments without publish them to local ivy.
You can add your own submodule in `build.sc`.  
For more information please visit [Mill documentation](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html)
after adding your own code, you can add your library to playground dependency, and re-index Intellij to add your own library.

## Quick Start

To use this repo as your Chisel development environment, simply follow the steps.

0. Clone this repo;

```bash
git clone git@github.com:chipsalliance/playground.git
```

0. Install dependencies and setup environments:
- Arch Linux `pacman -Syu --noconfirm make parallel wget cmake ninja mill dtc verilator git llvm clang lld protobuf antlr4 numactl`
- Nix `nix-shell`

0. [Optional] Remove unused dependences to accelerate bsp compile in `build.sc` `playground.moduleDeps`;

```bash
cd playground # entry your project directory
vim build.sc
```

```scala
// build.sc

// Original
object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(myrocketchip, inclusivecache, blocks, rocketdsputils, shells, firesim, boom, chipyard, chipyard.fpga, chipyard.utilities, mychiseltest)
  ...
}

// Remove unused dependences, e.g.,
object playground extends CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)
  ...
}
```


0. Init and update dependences;

```bash
cd playground # entry your project directory
make init     # init the submodules
make patch    # using the correct patches for some repos
```


0. Generate IDE bsp;

```bash
make bsp
```


0. Open your IDE and wait bsp compile;

```bash
idea . # open IDEA at current directory
```
06. Enjory your development with playground :)

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
rocket-chip-inclusive-cache https://github.com/chipsalliance/rocket-chip-inclusive-cache/pull/22.diff
<!-- END-PATCH -->

## Why not Chipyard

1. Building Chisel and FIRRTL from sources, get rid of any version issue. You can view Chisel/FIRRTL source codes from IDEA.
1. No more make+sbt: Scala dependencies are managed by mill -> bsp -> IDEA, minimal IDEA indexing time.
1. flatten git submodule in dependency, get rid of submodule recursive update.

So generally, this repo is the fast and cleanest way to start your Chisel project codebase.

## Always keep update-to-date
You can use this template and start your own job by appending commits on it. GitHub Action will automatically bump all dependencies, you can merge or rebase `chipsalliance/master` to your branch.

```bash
cd playground # entry your project directory
git rebase origin/master
```

## System Dependencies
Currently, only support **Arch Linux**, if you are using other distros please install nix.

* GNU Make
  - Arch Linux: make
* git
  - Arch Linux: git
* mill
  - Arch Linux: mill
* wget
  - Arch Linux: wget
* GNU Parallel
  - Arch Linux: parallel
* Device Tree Compiler
  - Arch Linux: dtc
* protobuf
  - Arch Linux: protobuf
* antlr4
  - Arch Linux: antlr4

## SanityTests
This package is the standalone tests to check is bumping correct or not, served as the unittest, this also can be a great example to illustrate usages.

**NOTICE: SanityTests also contains additional system dependencies:**
* clang: bootrom cross compiling and veriltor C++ -> binary compiling
  - Arch Linux: clang
* llvm: gnu toolchain replacement 
  - Arch Linux: llvm
* lld: LLVM based linker
  - Arch Linux: lld
* verilator -> Verilog -> C++ generation
  - Arch Linux: verilator numactl
* cmake -> verilator emulator build system
  - Arch Linux: cmake
* ninja -> verilator emulator build system
  - Arch Linux: ninja

### rocketchip
This package is a replacement to RocketChip Makefile based generator, it directly generate a simple RocketChip emulator with verilator and linked to spike. 
```
mill sanitytests.rocketchip
```

### vcu118
This package uses rocketchip and fpga-shells to elaborate FPGA bitstream generator and debug script with board [VCU118](https://www.xilinx.com/products/boards-and-kits/vcu118.html)
```
mill sanitytests.vcu118
```
If you wanna alter this to your own board, you can choose implmenting your own Shell to replace `VCU118Shell` in this test.
