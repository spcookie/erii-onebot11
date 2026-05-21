module github.com/erii/onebot-app

go 1.22

require github.com/erii/onebot-sdk v0.0.0

replace github.com/erii/onebot-sdk => ../onebot-sdk

require gopkg.in/yaml.v3 v3.0.1

require github.com/gorilla/websocket v1.5.3 // indirect
