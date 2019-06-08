# Graphql Server Plugin

A graphql implementation for jenkins.

## WARNING: This is currently a work in progress

## TODO

[X] Remove hacked in security stuff entirely (mostly commented out atm)
[X] Abstract classes should be handled the same as interfaces
[ ] Create whoami function to output who you are
[X] Remove allFreestyleJobs, and just allow class to be passed into allJobs
[ ] Add "id" argument to allQueryTypes
[ ] Add "id" argument to list[] fields?
[ ] Add allBuildsForJobs?
[ ] Add allTestsForJobs?

## "Feature Complete" query

```
query {
  allJobs {
    name
    _class
    actions {
      _class
    }
    allBuilds {
      _class
      actions {
        _class
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
      description
      displayName
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


// FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
// environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);
