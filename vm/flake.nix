# based on https://nixos-and-flakes.thiscute.world/nixos-with-flakes/nixos-with-flakes-enabled
{
  description = "NixOS Prod Machine Configuration";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05";
  };

  outputs = { self, nixpkgs, ... }@inputs: {
    # 'main' here is the hostname
    nixosConfigurations.main = nixpkgs.lib.nixosSystem {
      system = "x86_64-linux";
      modules = [
        ./configuration.nix
      ];
    };
  };
}