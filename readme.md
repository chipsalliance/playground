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
chisel3 https://github.com/chipsalliance/chisel3/pull/1854  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2809  
dsptools https://github.com/ucb-bar/dsptools/pull/222  
dsptools https://github.com/ucb-bar/dsptools/pull/226  
firesim https://github.com/firesim/firesim/pull/747  
firesim https://github.com/firesim/firesim/pull/749  
firesim https://github.com/firesim/firesim/pull/750  
hwacha https://github.com/ucb-bar/hwacha/pull/30  
testchipip https://github.com/ucb-bar/testchipip/pull/126  
<!-- END-PATCH -->
## Why not Chipyard

1. Building Chisel and FIRRTL from sources, get rid of any version issue. You can view Chisel/FIRRTL source codes from IDEA.
1. No more make+sbt: Scala dependencies are managed by mill -> bsp -> IDEA, minimal IDEA indexing time.
1. flatten git submodule in dependency, get rid of submodule recursive update.

So generally, this repo is the fast and cleanest way to start your Chisel project codebase.

## Always keep update-to-date
You can use this template and start your own job by appending commits on it. GitHub Action will automaticlly bumping all dependencies, you can merge or rebase `sequencer/master` to your branch.
