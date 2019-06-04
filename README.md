Graphql Server Plugin
=====================

A graphql implementation for jenkins.

## WARNING: This is currently a work in progress

## TODO

[] Create whoami function to output who you are

## "Feature Complete" query


```
query {
  allJobs {
    name
    _class
    allBuilds {
      _class
      actions {
        _class
        ... on CauseAction {
          _class
          causes {
            _class
            shortDescription
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
      description
      displayName
      duration
      estimatedDuration
      executor {
        _class
        currentExecutable
        idle
        likelyStuck
        number
        progress
      }
      fingerprint {
        _class
        fileName
        hash
        # original: BuildPtr
        timestamp
        # usage: [RangeItem]
      }
      fullDisplayName
      keepLog
      number
      queueId
      result
      timestamp
      url
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
}
```
