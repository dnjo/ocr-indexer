#! /bin/bash
set -euf -o pipefail

mvn clean package

sam package \
    --output-template-file packaged.yaml \
    --s3-bucket sam2.dnjo.net

sam deploy \
    --template-file packaged.yaml \
    --stack-name ocr-indexer \
    --capabilities CAPABILITY_IAM \
    --region eu-west-1
