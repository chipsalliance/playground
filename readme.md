# rocket-playground

## Introduction
This is a template repository for those who want to develope RTL based on rocket-chip, and be able to edit all sources from chisel environments without publish them to local ivy.
You can add your own submodule in `build.sc`, for more infomration please visit [mill documentation](https://www.lihaoyi.com/mill/page/configuring-mill.html)
after adding your own code, you can add your library to playground dependency, and re-index Intellij to add your own library.

## IDE support
use
```bash
mill -i mill.contrib.BSP/install
```
then open by your favorite IDE.
