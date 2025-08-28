resource "digitalocean_project" "koka_zero_playground" {
  name        = "Koka Zero Playground"
  description = "Online environment for running Koka Zero code"
  purpose     = "Web Application"
  environment = "Production"
  resources   = []
}
