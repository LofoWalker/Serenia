mkdir keys
openssl genrsa -out keys/rsaPrivateKey.pem 2048
openssl rsa -pubout -in keys/rsaPrivateKey.pem -out keys/publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in keys/rsaPrivateKey.pem -outform pem -out keys/privateKey.pem
chmod 600 keys/rsaPrivateKey.pem
chmod 600 keys/privateKey.pem