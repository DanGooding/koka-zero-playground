variable "digitalocean_personal_access_token" {
  type      = string
  sensitive = true
}

variable "region" {
  type    = string
  default = "lon1"
}
