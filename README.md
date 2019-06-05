# Graphql Server Plugin

A graphql implementation for jenkins.

## WARNING: This is currently a work in progress

## TODO

[ ] Create whoami function to output who you are
[ ] Remove allFreestyleJobs, and just allow class to be passed into allJobs
[ ] Remove hacked in security stuff entirely (mostly commented out atm)
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
