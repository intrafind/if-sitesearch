#!/usr/bin/env sh

#docker run -w /app -v $(pwd):/app --entrypoint /usr/bin/env -it hashicorp/terraform:light sh
#rm -rf ./terraform
#terraform init

echo "Terraform workspace: `terraform output`"
password=`terraform output password`
terraform destroy -auto-approve
terraform apply -auto-approve \
    -var password=$password \
    -var nodeCount=1 \
    -var masterCount=1 \
    $1

`terraform output k8s_ssh`
