## crypto-miners-demo

### Dependencies
 - Just to print pretty the json output: `yum install jq`
 - Scala version 2.12.3
 - Sbt version 1.0

### Executing
 - Clone the project in your local machine.
 - Use `sbt` to execute the project.
 - Open any browser and execute `http://localhost:9000/` to create the in-memory database: `Apply this script now!`.
 - Execute `curl` commands in you terminal: `curl http://localhost:9000/api/all | jq .`.
```
{
  "profitPerGpu": 0.1235567033290863,
  "rackList": []
}
```

### Adding racks
To add a Rack you only need to set its `id`. The other properties will be generated automatically with default values. If the operation is successful you are going to see the output `HTTP/1.1 200 OK` on your console.

You cannot insert duplicate Rack `id`. If you try to create a Rack with the same `id` the system is going to update the properties `prpduced` and `currentTime`. You are going to see the output `HTTP/1.1 200 OK` on your console.
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-1" }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2" }' http://localhost:9000/api/setup
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-3" }' http://localhost:9000/api/setup
curl http://localhost:9000/api/all | jq .
```
```
{
  "profitPerGpu": 0.1235567033290863,
  "rackList": [
    {
      "id": "rack-1",
      "produced": 0,
      "currentHour": "2018-01-04T10:10:05.598Z",
      "gpuList": []
    },
    {
      "id": "rack-2",
      "produced": 0,
      "currentHour": "2018-01-04T10:13:38.522Z",
      "gpuList": []
    }
  ]
}
```

### Adding Gpu to a rack
You can add a Gpu with `produced` property or not. The default value for `produced` is `0`. This value will increase on the `produced` property of the Rack.
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2", "produced": 0.3 }' http://localhost:9000/api/racks
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-2" }'http://localhost:9000/api/racks
curl http://localhost:9000/api/all | jq .
```
```
{
  "profitPerGpu": 0.1235567033290863,
  "rackList": [
    {
      "id": "rack-1",
      "produced": 0,
      "currentHour": "2018-01-04T10:10:05.598Z",
      "gpuList": []
    },
    {
      "id": "rack-2",
      "produced": 0.30000001192092896,
      "currentHour": "2018-01-04T10:13:38.522Z",
      "gpuList": [
        {
          "id": "rack-2-gpu-0",
          "rackId": "rack-2",
          "produced": 0.30000001192092896,
          "installedAt": "2018-01-04T10:14:45.494Z"
        },
        {
          "id": "rack-2-gpu-1",
          "rackId": "rack-2",
          "produced": 0,
          "installedAt": "2018-01-04T10:14:52.985Z"
        }
      ]
    }
  ]
}
```

### Listing all racks:
`curl http://localhost:9000/api/all | jq .`
```
{
  "profitPerGpu": 0.1235567033290863,
  "rackList": [
    {
      "id": "rack-1",
      "produced": 1.2999999523162842,
      "currentHour": "2018-01-04T10:10:05.598Z",
      "gpuList": [
        {
          "id": "rack-1-gpu-0",
          "rackId": "rack-1",
          "produced": 1.2999999523162842,
          "installedAt": "2018-01-04T10:17:37.859Z"
        }
      ]
    },
    {
      "id": "rack-2",
      "produced": 0.30000001192092896,
      "currentHour": "2018-01-04T10:13:38.522Z",
      "gpuList": [
        {
          "id": "rack-2-gpu-0",
          "rackId": "rack-2",
          "produced": 0.30000001192092896,
          "installedAt": "2018-01-04T10:14:45.494Z"
        },
        {
          "id": "rack-2-gpu-1",
          "rackId": "rack-2",
          "produced": 0,
          "installedAt": "2018-01-04T10:14:52.985Z"
        }
      ]
    },
    {
      "id": "rack-3",
      "produced": 0,
      "currentHour": "2018-01-04T10:17:46.566Z",
      "gpuList": []
    }
  ]
}
```

### Listing specific rack:
`curl http://localhost:9000/api/racks?at="2018-01-04T10:13:38.522Z" | jq .`
```
[
  {
    "id": "rack-2",
    "produced": 0.30000001192092896,
    "currentHour": "2018-01-04T10:13:38.522Z",
    "gpuList": [
      {
        "id": "rack-2-gpu-0",
        "rackId": "rack-2",
        "produced": 0.30000001192092896,
        "installedAt": "2018-01-04T10:14:45.494Z"
      },
      {
        "id": "rack-2-gpu-1",
        "rackId": "rack-2",
        "produced": 0,
        "installedAt": "2018-01-04T10:14:52.985Z"
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
    "id": "rack-2-gpu-0",
    "rackId": "rack-2",
    "produced": 0.30000001192092896,
    "installedAt": "2018-01-04T10:14:45.494Z"
  },
  {
    "id": "rack-2-gpu-1",
    "rackId": "rack-2",
    "produced": 0,
    "installedAt": "2018-01-04T10:14:52.985Z"
  },
  {
    "id": "rack-1-gpu-0",
    "rackId": "rack-1",
    "produced": 1.2999999523162842,
    "installedAt": "2018-01-04T10:17:37.859Z"
  }
]
```


