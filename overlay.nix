final: prev:
{
  mill = prev.mill.overrideAttrs (oldAttrs: rec {
    version = "0.10.11";
    src = prev.fetchurl {
      url = "https://github.com/com-lihaoyi/mill/releases/download/${version}/${version}-assembly";
      hash = "sha256-B47C7sqOqiHa/2kC5lk/J1pXK61l1M5umVKaCfVO7cc=";
    };
  });
  espresso = final.callPackage ./nix/espresso.nix { };
}
