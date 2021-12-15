# playground

## Introduction
This is a template repository for those who want to develop RTL based on rocket-chip and even chipyard, being able to edit all sources from chisel environments without publish them to local ivy.
You can add your own submodule in `build.sc`.  
For more information please visit [mill documentation](https://com-lihaoyi.github.io/mill/page/configuring-mill.html)
after adding your own code, you can add your library to playground dependency, and re-index Intellij to add your own library.

## Quick Start

To use this repo as your Chisel development environment, simply follow the steps.

1. Clone this repo;

```bash
git clone git@github.com:sequencer/playground.git
```


2. [Optional] Remove unused dependences to accelerate bsp compile in `build.sc` `playground.moduleDeps`;

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


3. Init and update dependences;

```bash
cd playground # entry your project directory
make init     # init the submodules
make patch    # using the correct patches for some repos
```


4. Generate IDE bsp;

```bash
make bsp
```


5. Open your IDE and wait bsp compile;

```bash
idea . # open IDEA at current directory
```


6. Enjory your development with playground :)

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
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2810  
hwacha https://github.com/ucb-bar/hwacha/pull/30  
fpga-shells https://github.com/sifive/fpga-shells/pull/161  
fpga-shells https://github.com/sifive/fpga-shells/pull/162  
testchipip https://github.com/ucb-bar/testchipip/pull/137  
rocket-dsp-utils https://github.com/ucb-bar/rocket-dsp-utils/pull/4  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2889  
riscv-boom https://github.com/riscv-boom/riscv-boom/pull/565  
dsptools https://github.com/ucb-bar/dsptools/pull/237  
berkeley-hardfloat https://github.com/ucb-bar/berkeley-hardfloat/pull/60  
icenet https://github.com/firesim/icenet/pull/32  
firesim https://github.com/firesim/firesim/pull/843  
hwacha https://github.com/ucb-bar/hwacha/pull/33  
chipyard https://github.com/ucb-bar/chipyard/pull/1001  
riscv-sodor https://github.com/ucb-bar/riscv-sodor/pull/67  
gemmini https://github.com/ucb-bar/gemmini/pull/150  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2890  
dsptools https://github.com/ucb-bar/dsptools/pull/240  
rocket-dsp-utils https://github.com/ucb-bar/rocket-dsp-utils/pull/6  
sha3 https://github.com/ucb-bar/sha3/pull/33  
<!-- END-PATCH -->

## Why not Chipyard

1. Building Chisel and FIRRTL from sources, get rid of any version issue. You can view Chisel/FIRRTL source codes from IDEA.
1. No more make+sbt: Scala dependencies are managed by mill -> bsp -> IDEA, minimal IDEA indexing time.
1. flatten git submodule in dependency, get rid of submodule recursive update.

So generally, this repo is the fast and cleanest way to start your Chisel project codebase.

## Always keep update-to-date
You can use this template and start your own job by appending commits on it. GitHub Action will automatically bump all dependencies, you can merge or rebase `sequencer/master` to your branch.

```bash
cd playground # entry your project directory
git rebase origin/master
```

## System Dependencies
Currently, only support **Arch Linux, macOS, Debian sid and Nix**, you can PR your own distributions, like Fedora.

Nix users may use `shell.nix` as provided.

**Notice Ubuntu and CentOS is unacceptable, since they have a stale package repository, not possible use official package manager to install these requirements, if you insist using them, please install requirements below by your self.**
* GNU Make
  - Arch Linux: make
  - Debian: make
  - Homebrew: make
* git
  - Arch Linux: git
  - Debian: git
  - Homebrew: git
* mill
  - Arch Linux: mill
  - Homebrew: mill
* wget
  - Arch Linux: wget
  - Debian: wget
  - Homebrew: wget
* GNU Parallel
  - Arch Linux: parallel
  - Debian: parallel
  - Homebrew: parallel
* Device Tree Compiler
  - Arch Linux: dtc
  - Debian: device-tree-compiler
  - Homebrew: dtc
* protobuf
  - Arch Linux: protobuf
  - Debian: protobuf-compiler
  - Homebrew: protobuf
* antlr4
  - Arch Linux: antlr4
  - Debian: antlr4
  - Homebrew: antlr

## SanityTests
This package is the standalone tests to check is bumping correct or not, served as the unittest, this also can be a great example to illustrate usages.

Nix users may use `shell.nix` as provided.

**NOTICE: SanityTests also contains additional system dependencies:**
**SanityTests do not support Mac, since LLVM package doesn't contain lld. **
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
