data "digitalocean_image" "nixos_vm_image" {
  name = var.nixos_vm_image_name
}

data "digitalocean_ssh_key" "from_nixos_build_machine" {
  name = "NixOS Build Machine"
}

resource "digitalocean_droplet" "main" {
  image    = data.digitalocean_image.nixos_vm_image.id
  name     = "main"
  region   = var.region
  size     = "s-1vcpu-1gb"
  ssh_keys = [data.digitalocean_ssh_key.from_nixos_build_machine.id]
}

resource "digitalocean_project" "koka_zero_playground" {
  name        = "Koka Zero Playground"
  description = "Online environment for running Koka Zero code"
  purpose     = "Web Application"
  environment = "Production"
  resources   = [digitalocean_droplet.main.urn]
}
