{ modulesPath, lib, pkgs, ... }:
{
  imports = lib.optional (builtins.pathExists ./do-userdata.nix) ./do-userdata.nix ++ [
    (modulesPath + "/virtualisation/digital-ocean-config.nix")
  ];

  environment.systemPackages = with pkgs; [
    docker
    fail2ban
  ];

  virtualisation.docker = {
    enable = true;

    # To give the containers the desired seccomp profile
    # we have to set it on the daemon, to be inherited.
    daemon.settings."seccomp-profile" = ../seccomp/bwrap-seccomp-profile.json;
  };

  services.fail2ban.enable = true;

  swapDevices = [
    {
      device = "/swapfile";
      size = 2048; # 2GB
    }
  ];

  system.stateVersion = "25.05";
}
