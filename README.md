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
You can add a Gpu to a Rack by identifying the RackID.
 - The properties `produced` and `installedAt` are optional.
 - The default value for `installedAt` is the current day/hour.
 - The default value for `produced` is calculated by how many hours the `installedAt` of the Gpu is after the `currentHour` of the Rack.
 - The value of `installedAt` must be after the `currentHour` of the Rack.
 - The value of `produced` property is going to increase on the Rack `produced` property.
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "rackId": "rack-1", "produced":0.2, "installedAt": "2018-01-06T15:10:48.515Z" }' http://localhost:9000/api/racks
curl -v --request POST --header "Content-Type: application/json" --data '{ "rackId": "rack-1", "installedAt": "2018-01-06T15:10:48.515Z" }' http://localhost:9000/api/racks
curl -v --request POST --header "Content-Type: application/json" --data '{ "rackId": "rack-1" }' http://localhost:9000/api/racks
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
If you try to add a Gpu to a non existing Rack you are going to receive a `HTTP/1.1 400 Bad Request` status.
```
curl -v --request POST --header "Content-Type: application/json" --data '{ "id": "rack-10000000-no-exist" }' http://localhost:9000/api/racks
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
If you try to pass a wrong String representation of time you are going to receive `Rack not found` or `Error on parse String to time.`.
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

### Executing some processing with Spark

#### Using Spark SQL

 - Listing all movies from a file: `curl http://localhost:9000/api/listAll`
 - Counting how many movies there is listed in the file: `curl http://localhost:9000/api/count`
 - Rating all the movies from two files: movies.csv and ratings.csv: `http://localhost:9000/api/rate`
 - Listing all movies by genre: `http://localhost:9000/api/listByGenre?genres=Comedy,Romance`

#### Using Spark context

 - Listing popular movies (count, movieId): `http://localhost:9000/api/popularMovies`



### Testing Controllers:
To test the controllers execute `sbt test`.



