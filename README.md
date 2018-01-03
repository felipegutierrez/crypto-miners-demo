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
```
[{"id":"rack-3","produced":0.30000001192092896,"currentHour":1514947025626},{"id":"rack-4","produced":0.4000000059604645,"currentHour":1514947028738},{"id":"rack-1","produced":0.10000000149011612,"currentHour":1514947039259},{"id":"rack-2","produced":0.20000000298023224,"currentHour":1514947042838}]
```


### Listing specific rack
```
curl http://localhost:9000/api/racks?at=1514947028738
```
```
[{"id":"rack-4","produced":0.4000000059604645,"currentHour":1514947028738}]
```

### Adding Gpu to a rack
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1", "produced":0.1 }' http://localhost:9000/api/racks
```



