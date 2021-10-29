with import <nixpkgs> {
  config = {
    packageOverrides = pkgs: {
      llvmPackages = pkgs.llvmPackages_13;
      clang = pkgs.clang_13;
      lld = pkgs.lld_13;
      jdk = pkgs.graalvm11-ce; # choose your preferred jdk
      protobuf = pkgs.protobuf3_15; # required by firrtl
    };
  };
};

let
  clang-multiple-target =
    pkgs.writeScriptBin "clang" ''
      #!${pkgs.bash}/bin/bash
      if [[ "$*" == *--target=riscv64* ]]; then
        # works partially, namely no ld
        ${pkgs.pkgsCross.riscv64.buildPackages.clang}/bin/riscv64-unknown-linux-gnu-clang $@
      else
        # works fully
        ${pkgs.clang}/bin/clang $@
      fi
    '';
  clangpp-multiple-target =
    pkgs.writeScriptBin "clang++" ''
      #!${pkgs.bash}/bin/bash
      if [[ "$*" == *--target=riscv64* ]]; then
        # works partially, namely no ld
        ${pkgs.pkgsCross.riscv64.buildPackages.clang}/bin/riscv64-unknown-linux-gnu-clang++ $@
      else
        # works fully
        ${pkgs.clang}/bin/clang++ $@
      fi
    '';
  cpp-multiple-target = pkgs.writeScriptBin "cpp" ''
    #!${pkgs.bash}/bin/bash
    ${pkgs.clang}/bin/cpp $@
  '';
in pkgs.callPackage (
  {
    mkShell,
    jdk,
    gnumake, git, mill, wget, parallel, dtc, protobuf, antlr4,
    llvmPackages, clang, lld, verilator, cmake, ninja, strace
  }:

  mkShell {
    name = "sequencer-playground";
    depsBuildBuild = [
      jdk gnumake git mill wget parallel dtc protobuf antlr4
      verilator cmake ninja
      llvmPackages.llvm lld

      clang-multiple-target
      clangpp-multiple-target
      cpp-multiple-target
    ];
    shellHook = ''
      unset CC
      unset CXX
      unset LD
    '';
  }
) {}
