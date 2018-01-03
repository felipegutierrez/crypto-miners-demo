## crypto-miners-demo



### Adding racks
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1", "produced":0.1 }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2", "produced":0.2 }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-3", "produced":0.3 }' http://localhost:9000/api/setup
```

### Listing all racks:
```
curl http://localhost:9000/api/all
```


### Listing specific rack
```
curl http://localhost:9000/api/racks?at=1514946232851
```


