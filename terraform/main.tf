data "digitalocean_image" "nixos_vm_image" {
  name = var.nixos_vm_image_name
}

data "digitalocean_ssh_keys" "keys" {
  filter {
    key = "name"
    values = [
      "NixOS Build Machine",
      "Ubuntu Inspiron"
    ]
  }
}

resource "digitalocean_ssh_key" "keys" {
  for_each = {
    for key in var.ssh_keys :
    key.name => key
  }

  name       = each.value.name
  public_key = each.value.public_key
}

resource "digitalocean_droplet" "main" {
  image    = data.digitalocean_image.nixos_vm_image.id
  name     = "main"
  region   = var.region
  size     = "s-1vcpu-1gb"
  ssh_keys = [for key in digitalocean_ssh_key.keys : key.id]
}

resource "digitalocean_project" "koka_zero_playground" {
  name        = "Koka Zero Playground"
  description = "Online environment for running Koka Zero code"
  purpose     = "Web Application"
  environment = "Production"
  resources   = [digitalocean_droplet.main.urn]
}
