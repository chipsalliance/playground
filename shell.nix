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

pkgs.callPackage (
  {
    mkShell,
    jdk,
    gnumake, git, mill, wget, parallel, dtc, protobuf, antlr4,
    llvmPackages, clang, lld, verilator, cmake, ninja
  }:

  mkShell {
    name = "sequencer-playground";
    depsBuildBuild = [
      jdk gnumake git mill wget parallel dtc protobuf antlr4
      verilator cmake ninja
      llvmPackages.llvm lld
      clang
      pkgsCross.riscv64.buildPackages.clang
      # why not crossSystem? it introduces cross stdenv!
    ];
  }
) {}
