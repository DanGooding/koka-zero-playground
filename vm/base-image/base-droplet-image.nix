# A nix function to generate a default DigitalOcean VM image.
# Bootstrap the droplet with this, then rebuild with our custom config.
#
# This will generate an image for whatever architecture it is built on.
# Passing { crossSystem } was too slow in practice, and was missing some deps.

{ pkgs ? import <nixpkgs> }:
let config = {
  imports = [ <nixpkgs/nixos/modules/virtualisation/digital-ocean-image.nix> ];
};
in
(pkgs.nixos config).digitalOceanImage
