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

  # Docker's overlay network pollutes the host routing table with routes from
  # the link-local range into the containers. These appear unused, and they
  # prevent do-agent from reaching the metadata service.
  # Fix this by adding a static route out of the correct interface.
  # Nix support for static routes is poor, so have to use a hacky startup script.
  systemd.services.do-metadata-static-route = {
    enable = true;
    description = "add a static route to the digitalocean metadata service";
    unitConfig = {
      after = [ "network-online.target" ];
      wants = [ "network-online.target" ];
    };
    serviceConfig = {
      Type = "oneshot";
      ExecStart =
        let metadataFixedIP = "169.254.169.254"; in
        "/run/current-system/sw/bin/ip route replace ${metadataFixedIP} dev ens3";
      RemainAfterExit = "yes";
    };

    wantedBy = [ "multi-user.target" ];
  };

  system.stateVersion = "25.05";
}
