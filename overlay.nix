final: prev:
{
  mill = prev.mill.override { jre = final.openjdk19; };
  espresso = final.callPackage ./nix/espresso.nix { };
}
