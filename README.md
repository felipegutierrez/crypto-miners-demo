## crypto-miners-demo

### Dependencies
 - Just to print pretty the json output: `yum install jq`
 - Scala version 2.12.3
 - Sbt version 1.0

### Executing
 - Clone the project in your local machine.
 - Use `sbt` to execute the project.
 - Open any browser and execute `http://localhost:9000/` to create the in-memory database.
 - Execute `curl` commands in you terminal.

### Adding racks
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1" }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2" }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-3" }' http://localhost:9000/api/setup
```

### Adding Gpu to a rack
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2", "produced": 0.3 }' http://localhost:9000/api/racks
```

### Listing all racks:
`curl http://localhost:9000/api/all | jq .`
```
[
  {
    "id": "rack-3",
    "produced": 0.30000001192092896,
    "currentHour": "2018-01-03T16:09:17.656Z",
    "gpuList": []
  },
  {
    "id": "rack-2",
    "produced": 0.20000000298023224,
    "currentHour": "2018-01-03T16:09:24.83Z",
    "gpuList": [
      {
        "id": "rack-2-gpu-0",
        "rackId": "rack-2",
        "produced": 0,
        "installedAt": "2018-01-03T23:07:45.779Z"
      },
      {
        "id": "rack-2-gpu-1",
        "rackId": "rack-2",
        "produced": 0,
        "installedAt": "2018-01-03T23:07:45.779Z"
      }
    ]
  }
]
```

### Listing specific rack:
`curl http://localhost:9000/api/racks?at="2018-01-03T16:09:24.83Z" | jq .`
```
[
  {
    "id": "rack-2",
    "produced": 0.20000000298023224,
    "currentHour": "2018-01-03T16:09:24.83Z",
    "gpuList": [
      {
        "id": "rack-2-gpu-0",
        "rackId": "rack-2",
        "produced": 0,
        "installedAt": "2018-01-03T23:07:45.779Z"
      },
      {
        "id": "rack-2-gpu-1",
        "rackId": "rack-2",
        "produced": 0,
        "installedAt": "2018-01-03T23:07:45.779Z"
      }
    ]
  }
]
```

### Listing all gpu's:
`curl http://localhost:9000/api/allGpu | jq .`
```
[
  {
    "id": "rack-1-gpu-0",
    "rackId": "rack-1",
    "produced": 0,
    "installedAt": "2018-01-03T22:36:39.582Z"
  },
  {
    "id": "rack-1-gpu-1",
    "rackId": "rack-1",
    "produced": 0,
    "installedAt": "2018-01-03T22:36:44.876Z"
  },
  {
    "id": "rack-3-gpu-0",
    "rackId": "rack-3",
    "produced": 0,
    "installedAt": "2018-01-03T23:00:58.87Z"
  },
  {
    "id": "rack-3-gpu-1",
    "rackId": "rack-3",
    "produced": 0,
    "installedAt": "2018-01-03T23:01:40.330Z"
  }
]
```


