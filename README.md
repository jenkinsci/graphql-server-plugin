# Graphql Server Plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?style=plastic&job=Plugins%2Fgraphql-server-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/graphql-server-plugin/job/master/)

A graphql implementation for jenkins.

## WARNING: This is currently a work in progress

## "Feature Complete" query

```
query allJobs {
  allAbstractItems {
    name
    id
    _class
    actions {
      _class
    }
    description
    displayName
    fullDisplayName
    url
    ... on hudson_model_Job {
      allBuilds {
        _class
        id
        actions {
          _class
          ... on hudson_tasks_junit_TestResultAction {
            _class
            failCount
            skipCount
            totalCount
            urlName
          }
          ... on hudson_model_CauseAction {
            causes {
              _class
              shortDescription
              ... on hudson_model_Cause_UserIdCause {
                userId
                userName
              }
            }
          }
        }
        artifacts {
          _class
          displayPath
          fileName
          relativePath
        }
        building
        duration
        estimatedDuration
        executor {
          _class
          currentExecutable {
            _class
          }
          idle
          likelyStuck
          number
          progress
        }
        fingerprint {
          _class
          fileName
          hash
          timestamp
        }
        keepLog
        number
        queueId
        result
        timestamp
      }
    }
  }
  allUsers {
    _class
    absoluteUrl
    description
    fullName
    id
    property {
      _class
    }
  }
  whoAmI {
    anonymous
    authenticated
    authorities
    details
    name
    toString
  }
}
```

// FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
// environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);
