terraform {
  backend "s3" {
    bucket         = "terraform-bucket-voseghale"
    key            = "15003103/02ebe298-1037-4fbf-88dd-a3115930aeed/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "TerraformStateLocks"
  }
}