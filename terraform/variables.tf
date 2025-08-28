variable "digitalocean_personal_access_token" {
  type      = string
  sensitive = true
}

variable "region" {
  type    = string
  default = "lon1"
}

variable "nixos_vm_image_name" {
  type    = string
  default = "nixos-image-digital-ocean-25.11pre852965.c3e5d9f86b3f-x86_64-linux"
}
