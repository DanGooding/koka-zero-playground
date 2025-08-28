#!/usr/bin/env bash

# Has to be run on machine with target architecture
nix build --file ./base-droplet-image.nix

# Result has to be uploaded to DigitalOcean manually via the browser console.
# Since doctl doesn't allow uploads except from urls.
