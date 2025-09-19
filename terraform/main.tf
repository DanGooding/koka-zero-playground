data "digitalocean_image" "nixos_vm_image" {
  name = var.nixos_vm_image_name
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
  size     = "s-2vcpu-2gb"
  ssh_keys = [for key in digitalocean_ssh_key.keys : key.id]
}

resource "digitalocean_project" "koka_zero_playground" {
  name        = "Koka Zero Playground"
  description = "Online environment for running Koka Zero code"
  purpose     = "Web Application"
  environment = "Production"
  resources = [digitalocean_droplet.main.urn]
}

resource "digitalocean_uptime_check" "koka_zero_playground" {
  name   = "koka-zero-playground uptime check"
  target = "https://koka-zero.danielgooding.uk"
  regions = ["eu_west"]
}

resource "digitalocean_uptime_alert" "up" {
  check_id   = digitalocean_uptime_check.koka_zero_playground.id
  name = "uptime alert"
  # alert once down for 10 min
  type       = "down"
  period     = "10m"
  comparison = "less_than"
  threshold  = 1
  notifications {
    email = [var.alert_email]
  }
}

resource "digitalocean_uptime_alert" "ssl_expiry" {
  check_id   = digitalocean_uptime_check.koka_zero_playground.id
  name = "ssl expiry alert"
  # alert once within 20d of expiry
  type       = "ssl_expiry"
  period     = "10m"
  comparison = "less_than"
  threshold  = 10
  notifications {
    email = [var.alert_email]
  }
}

