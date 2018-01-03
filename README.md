## crypto-miners-demo

### Dependencies
 - Just to print pretty the json output: `yum install jq`
 - Open any browser and execute `http://localhost:9000/` to create the in-memory database.


### Adding racks
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1", "produced":0.1 }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2", "produced":0.2 }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-3", "produced":0.3 }' http://localhost:9000/api/setup
```

### Adding Gpu to a rack
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1", "produced":0.1 }' http://localhost:9000/api/racks
```

### Listing all racks: `curl http://localhost:9000/api/all | jq .`
```
[
  {
    "id": "rack-1",
    "produced": 0.10000000149011612,
    "currentHour": 1514991180180,
    "gpuList": [
      {
        "id": "rack-1-gpu-0",
        "rackId": "rack-1",
        "produced": 0,
        "installedAt": 1514991248162
      },
      {
        "id": "rack-1-gpu-1",
        "rackId": "rack-1",
        "produced": 0,
        "installedAt": 1514991251342
      }
    ]
  },
  {
    "id": "rack-2",
    "produced": 0.20000000298023224,
    "currentHour": 1514991208519,
    "gpuList": []
  }
]
```


### Listing specific rack: `curl http://localhost:9000/api/racks?at=1514991887142 | jq .`
```
[
  {
    "id": "rack-1",
    "produced": 0.10000000149011612,
    "currentHour": 1514991887142,
    "gpuList": [
      {
        "id": "rack-1-gpu-0",
        "rackId": "rack-1",
        "produced": 0,
        "installedAt": 1514991907235
      },
      {
        "id": "rack-1-gpu-1",
        "rackId": "rack-1",
        "produced": 0,
        "installedAt": 1514991908062
      },
      {
        "id": "rack-1-gpu-2",
        "rackId": "rack-1",
        "produced": 0,
        "installedAt": 1514991908660
      }
    ]
  }
]
```




